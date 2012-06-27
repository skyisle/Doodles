
package com.alanjeon.doodles.util;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.concurrent.Executor;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.ImageView;

public abstract class ImageWorker {
    private static final String TAG = LogUtils.makeLogTag(ImageWorker.class);
    private final Hashtable<Integer, Bitmap> loadingBitmaps = new Hashtable<Integer, Bitmap>(2);
    protected Context mContext;
    private boolean mExitTasksEarly = false;
    protected boolean mFadeInBitmap = true;
    protected ImageCache mImageCache;
    protected Bitmap mLoadingBitmap;

    protected ImageWorker(Context context) {
        mContext = context;
    }

    public static boolean cancelPotentialWork(Object object, ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            Object bitmapData = bitmapWorkerTask.data;
            LogUtils.LOGD(TAG, "currentObject = " + bitmapData + " object = " + object);

            if (bitmapData != null && !bitmapData.equals(object)) {
                LogUtils.LOGV(TAG, "cancelPotentialWork - cancelled work for " + object);
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }

        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }

        return null;
    }

    private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            Drawable[] drawables = new Drawable[2];
            drawables[0] = new ColorDrawable(0x106000d);
            drawables[1] = new BitmapDrawable(mContext.getResources(), bitmap);
            TransitionDrawable fadeInTransition = new TransitionDrawable(drawables);
            imageView.setBackgroundDrawable(imageView.getDrawable());
            imageView.setImageDrawable(fadeInTransition);
            fadeInTransition.startTransition(200);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    protected void loadImage(Object object, ImageView imageView, int resId) {
        if (!loadingBitmaps.containsKey(resId)) {
            loadingBitmaps.put(resId,
                    BitmapFactory.decodeResource(mContext.getResources(), resId));
        }

        loadImage(object, imageView, (Bitmap) loadingBitmaps.get(resId));
    }

    protected void loadImage(Object object, ImageView imageView, Bitmap loadingBitmap) {
        Bitmap bitmap = null;
        if (mImageCache != null) {
            bitmap = mImageCache.getBitmapFromMemCache(String.valueOf(object));
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);

            if (!cancelPotentialWork(object, imageView))
                return;
        } else {
            BitmapWorkerTask workerTask = new BitmapWorkerTask(imageView);
            imageView.setImageDrawable(new AsyncDrawable(mContext.getResources(),
                    loadingBitmap, workerTask));

            executeWorker(workerTask, object);
        }
    }

    @TargetApi(11)
    private void executeWorker(BitmapWorkerTask workerTask, Object object) {
        if (UIUtils.isHoneycomb()) {
            Executor localExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
            Object[] params = new Object[1];
            params[0] = object;
            workerTask.executeOnExecutor(localExecutor, params);
        } else {
            Object[] arrayOfObject1 = new Object[1];
            arrayOfObject1[0] = object;
            workerTask.execute(arrayOfObject1);
        }
    }

    protected abstract Bitmap processBitmap(Object object);

    public void setImageCache(ImageCache imageCache) {
        mImageCache = imageCache;
    }

    public void setImageFadeIn(boolean fadein) {
        mFadeInBitmap = fadein;
    }

    public void setPauseDiskCache(boolean pause) {
        if (mImageCache == null) {
            return;
        }
        mImageCache.setPauseDiskCache(pause);
    }

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap loadingBitmap,
                BitmapWorkerTask workerTask) {
            super(res, loadingBitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(workerTask);
        }

        public ImageWorker.BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
        private Object data;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        private ImageView getAttachedImageView() {
            ImageView imageView = (ImageView) imageViewReference.get();
            BitmapWorkerTask bitmapWorkerTask = ImageWorker.getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }

        protected Bitmap doInBackground(Object... params) {
            this.data = params[0];
            String url = String.valueOf(this.data);

            Bitmap bitmap = null;
            if (ImageWorker.this.mImageCache != null
                    && !isCancelled()
                    && getAttachedImageView() != null
                    && !ImageWorker.this.mExitTasksEarly) {
                bitmap = ImageWorker.this.mImageCache.getBitmapFromDiskCache(url);
            }

            if (bitmap == null
                    && !isCancelled()
                    && getAttachedImageView() != null
                    && !ImageWorker.this.mExitTasksEarly) {
                bitmap = ImageWorker.this.processBitmap(params[0]);
            }

            if (bitmap != null
                    && ImageWorker.this.mImageCache != null) {
                ImageWorker.this.mImageCache.addBitmapToCache(url, bitmap);
            }

            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled() || ImageWorker.this.mExitTasksEarly) {
                bitmap = null;
            }

            ImageView imageView = getAttachedImageView();
            if (bitmap == null || imageView == null) {
                return;
            }

            ImageWorker.this.setImageBitmap(imageView, bitmap);
        }
    }

    public static boolean isHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
}


package com.alanjeon.doodles.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.ImageView;

public class ImageFetcher extends ImageWorker {
    private static final String TAG = LogUtils.makeLogTag(ImageFetcher.class);
    private ImageFetcherParams mFetcherParams;

    public ImageFetcher(Context context) {
        super(context.getApplicationContext());
        setParams(new ImageFetcherParams());
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;

        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    /** @deprecated */
    public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth,
            int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }

    public static void disableConnectionReuseIfNecessary() {
        if (hasHttpConnectionBug()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    public static File downloadBitmapToFile(Context context, String key,
            String cacheDir) {
        File cache = ImageCache.getDiskCacheDir(context, cacheDir);
        if (!cache.exists()) {
            cache.mkdir();
        }

        ImageFetcher.disableConnectionReuseIfNecessary();
        File imageFile = null;
        HttpURLConnection conn = null;

        try {
            imageFile = File.createTempFile(null, null, null);

            URL url = new URL(key);
            conn = (HttpURLConnection) url.openConnection();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(imageFile));

            byte buffer[] = new byte[1024];
            int bytes = 0;
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }

            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            LogUtils.LOGE(TAG, "downloadBitmapToFile error ", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return imageFile;
    }

    public static byte[] downloadBitmapToMemory(Context context, String key,
            int maxBytes)
    {
        ImageFetcher.disableConnectionReuseIfNecessary();

        HttpURLConnection conn = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            URL url = new URL(key);
            conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());

            byte buffer[] = new byte[1024];
            int bytes = 0;
            while ((bytes = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytes);
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
            LogUtils.LOGE(TAG, "downloadBitmapToMemory error ", e);
        } catch (IOException e) {
            LogUtils.LOGE(TAG, "downloadBitmapToMemory error ", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return baos.toByteArray();
    }

    public static boolean hasHttpConnectionBug() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    }

    private Bitmap processBitmap(String key, int type) {
        LogUtils.LOGD(TAG, "processBitmap - " + key);
        Bitmap bitmap = null;
        if (type == 1) {
            File file = downloadBitmapToFile(mContext, key,
                    mFetcherParams.mHttpCacheDir);
            if (file == null)
                return null;

            bitmap = decodeSampledBitmapFromFile(file.toString(),
                    mFetcherParams.mImageWidth, mFetcherParams.mImageHeight);
            file.delete();
        } else if (type == 0) {
            byte[] bytes = downloadBitmapToMemory(mContext, key,
                    mFetcherParams.mMaxThumbnailBytes);
            if (bytes != null) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }

        return bitmap;

    }

    public void loadThumbnailImage(String url, ImageView imageView) {
        loadImage(new ImageData(url, 0), imageView, mLoadingBitmap);
    }

    public void loadThumbnailImage(String url, ImageView imageView, int resId) {
        loadImage(new ImageData(url, 0), imageView, resId);
    }

    protected Bitmap processBitmap(Object object) {
        ImageData data = (ImageData) object;
        return processBitmap(data.mKey, data.mType);
    }

    public void setParams(ImageFetcherParams params) {
        mFetcherParams = params;
    }

    private static class ImageData {
        public String mKey;
        public int mType;

        public ImageData(String key, int type) {
            mKey = key;
            mType = type;
        }

        public String toString() {
            return mKey;
        }
    }

    public static class ImageFetcherParams {
        public String mHttpCacheDir = "http";
        public int mHttpCacheSize = 5 * 1024 * 1024; // 5MB
        public int mImageHeight = 1024;
        public int mImageWidth = 1024;
        public int mMaxThumbnailBytes = 50 * 1024; // 50KB
    }
}

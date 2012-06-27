
package com.alanjeon.doodles.util;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageCache {
    private static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT;
    private static final String TAG = LogUtils.makeLogTag(ImageCache.class);
    private ImageCacheParams mCacheParams;
    private DiskLruCache mDiskCache;
    private LruCache<String, Bitmap> mMemoryCache;
    private boolean mPauseDiskAccess = false;

    static {
        DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    }

    public ImageCache(Context context, ImageCacheParams params) {
        init(context, params);
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            String str = Integer.toHexString(0xFF & bytes[i]);
            if (str.length() == 1) {
                sb.append('0');
            }
            sb.append(str);
        }
        return sb.toString();
    }

    public static ImageCache findOrCreateCache(FragmentActivity activity,
            ImageCacheParams params)
    {
        RetainFragment fragment = findOrCreateRetainFragment(activity
                .getSupportFragmentManager());
        ImageCache cache = (ImageCache) fragment.getObject();
        if (cache == null) {
            cache = new ImageCache(activity, params);
            fragment.setObject(cache);
        }
        return cache;
    }

    public static ImageCache findOrCreateCache(FragmentActivity activity,
            String uniqueName) {
        return findOrCreateCache(activity, new ImageCacheParams(activity,
                uniqueName));
    }

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment, TAG).commit();
        }
        return fragment;
    }

    @TargetApi(12)
    public static int getBitmapSize(Bitmap bitmap)
    {
        int size;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            size = bitmap.getByteCount();
        } else {
            size = bitmap.getRowBytes() * bitmap.getHeight();
        }

        return size;
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                        || !isExternalStorageRemovable()
                        ? getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    @TargetApi(8)
    public static File getExternalCacheDir(Context context)
    {
        File cacheDir = null;
        if (hasExternalCacheDir()) {
            cacheDir = context.getExternalCacheDir();
            if (cacheDir == null) {
                String str = "/Android/data/" + context.getPackageName() + "/cache/";
                cacheDir = new File(Environment.getExternalStorageDirectory().getPath() + str);
            }
        }

        return cacheDir;
    }

    @TargetApi(9)
    public static long getUsableSpace(File file)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return file.getUsableSpace();
        } else {
            StatFs stat = new StatFs(file.getPath());
            return stat.getBlockSize() * stat.getAvailableBlocks();
        }
    }

    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static String hashKeyForDisk(String name) {
        String key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(name.getBytes());
            key = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException localNoSuchAlgorithmException) {
            key = String.valueOf(name.hashCode());
        }

        return key;
    }

    private void init(Context context, ImageCacheParams params) {
        mCacheParams = params;
        File cacheDir = getDiskCacheDir(context, params.uniqueName);
        try
        {
            if (params.diskCacheEnabled) {
                if (!cacheDir.exists()) {
                    cacheDir.mkdir();
                }
                if (getUsableSpace(cacheDir) > params.diskCacheSize) {
                    mDiskCache = DiskLruCache.open(cacheDir, 1, 1,
                            params.diskCacheSize);
                }
            }

            if (params.memoryCacheEnabled) {
                mMemoryCache = new LruCache<String, Bitmap>(params.memCacheSize) {
                    protected int sizeOf(String paramString, Bitmap paramBitmap) {
                        return ImageCache.getBitmapSize(paramBitmap);
                    }
                };
            }
            return;
        } catch (IOException e) {
            LogUtils.LOGE(TAG, "init - " + e);
        }
    }

    @TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    public void addBitmapToCache(String url, Bitmap bitmap) {
        if (url != null && bitmap != null) {
            if (mMemoryCache != null && mMemoryCache.get(url) == null) {
                mMemoryCache.put(url, bitmap);
            }

            if (mDiskCache == null) {
                return;
            }

            String str = hashKeyForDisk(url);
            try {
                if (mDiskCache.get(str) != null)
                    return;

                DiskLruCache.Editor editor = mDiskCache.edit(str);
                if (editor == null)
                    return;

                OutputStream output = editor.newOutputStream(0);
                bitmap.compress(mCacheParams.compressFormat,
                        mCacheParams.compressQuality, output);
                editor.commit();
            } catch (IOException e) {
                LogUtils.LOGE(TAG, "addBitmapToCache - " + e);
            }
        }
    }

    public Bitmap getBitmapFromDiskCache(String path)
    {
        if (mDiskCache == null)
            return null;

        String hashKey = hashKeyForDisk(path);

        Bitmap bitmap;
        try {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(hashKey);
            if (snapshot == null) {
                return null;
            }

            LogUtils.LOGV(TAG, "ICS disk cache hit");
            if (mPauseDiskAccess) {
                return null;
            }

            bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0));
        } catch (IOException e) {
            LogUtils.LOGE(TAG, "getBitmapFromDiskCache - " + e);
            bitmap = null;
        }

        return bitmap;
    }

    public Bitmap getBitmapFromMemCache(String key) {
        Bitmap bitmap = null;
        if (mMemoryCache != null) {
            bitmap = mMemoryCache.get(key);
        }

        return bitmap;

    }

    public void setPauseDiskCache(boolean pause) {
        mPauseDiskAccess = pause;
    }

    public static class RetainFragment extends Fragment {
        private Object mObject;

        public Object getObject() {
            return mObject;
        }

        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            setRetainInstance(true);
        }

        public void setObject(Object object) {
            mObject = object;
        }
    }

    public static class ImageCacheParams {
        public String cacheFilenamePrefix = "cache_";
        public boolean clearDiskCacheOnStart = false;
        public Bitmap.CompressFormat compressFormat = ImageCache.DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = 75;
        public boolean diskCacheEnabled = true;
        public long diskCacheSize = 10 * 1024 * 1024;
        public int memCacheSize = 2 * 1024 * 1024;
        public boolean memoryCacheEnabled = true;
        public int memoryClass = 0;
        public String uniqueName;

        public ImageCacheParams(Context context, String uniquename) {
            uniqueName = uniquename;
            memoryClass = ((ActivityManager) context.getSystemService("activity"))
                    .getMemoryClass();
            memCacheSize = (1024 * 1024 * memoryClass / 8);
        }
    }
}

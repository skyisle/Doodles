
package com.alanjeon.doodles.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.FragmentActivity;

public class UIUtils {
    public static boolean isHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK)
        >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return isHoneycomb() && isTablet(context);
    }

    public static ImageFetcher getImageFetcher(FragmentActivity fragmentActivity) {
        ImageFetcher imageFetcher = new ImageFetcher(fragmentActivity);
        imageFetcher.setImageCache(ImageCache.findOrCreateCache(fragmentActivity,
                "imageFetcher"));
        return imageFetcher;
    }
}


package com.alanjeon.doodles.util;

import android.util.Log;

public class LogUtils {
    private static String LOG_PREFIX = "iosched_";
    private static int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static int MAX_LOG_TAG_LENGTH = 23;

    public static void LOGD(String tag, String msg) {
        // if (!Log.isLoggable(tag, Log.DEBUG))
        // return;
        Log.d(tag, msg);
    }

    public static void LOGE(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void LOGE(String tag, String msg, Throwable paramThrowable) {
        Log.e(tag, msg, paramThrowable);
    }

    public static void LOGI(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void LOGI(String tag, String msg, Throwable paramThrowable) {
        Log.i(tag, msg, paramThrowable);
    }

    public static void LOGV(String tag, String msg) {
        Log.v(tag, msg);
    }

    public static void LOGW(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static String makeLogTag(Class<?> paramClass) {
        return makeLogTag(paramClass.getSimpleName());
    }

    public static String makeLogTag(String name) {
        String tag = null;
        if (name.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            tag = LOG_PREFIX
                    + name.substring(0, -1 + MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH);
        } else {
            tag = LOG_PREFIX + name;
        }
        return tag;
    }
}

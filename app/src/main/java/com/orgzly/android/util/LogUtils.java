package com.orgzly.android.util;

import android.text.TextUtils;
import android.util.Log;

/**
 * Methods for logging debugging information.
 *
 * They are slow and should *never* be called by production code.
 * Call them under "if (BuildConfig.LOG_DEBUG)" so they are removed by ProGuard.
 */
public class LogUtils {
    private static final int LOGCAT_BUFFER_SIZE = 1024;
    
    /**
     * Logs caller's method name followed by specified parameters.
     */
    public static void d(String tag, Object... args) {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];

            if (arg instanceof String[]) {
                s.append(TextUtils.join("|", (String[]) arg));
            } else {
                s.append(arg);
            }

            if (i < args.length - 1) {
                s.append(" ");
            }
        }

        /* Prefix with caller's method name. */
        if (s.length() > 0) {
            s.insert(0, ": ");
        }
        s.insert(0, getCallerMethodName());

        /* Prefix with thread. */
        Thread curr = Thread.currentThread();
        s.insert(0, "T#" + curr.getId() + "#" + curr.getName() + ": ");

        doLog(tag, s.toString());
    }

    /**
     * Logs in chunks, due to logcat limit.
     */
    private static void doLog(String tag, String s) {
        int length = s.length();

        for (int i = 0; i < length; i += LOGCAT_BUFFER_SIZE) {
            if (i + LOGCAT_BUFFER_SIZE < length) {
                Log.d(tag, s.substring(i, i + LOGCAT_BUFFER_SIZE));
            } else {
                Log.d(tag, s.substring(i, length));
            }
        }
    }

    /**
     * Returns the last method found in stack trace (before this class).
     */
    private static String getCallerMethodName() {
        String lastMethod = "UNKNOWN-METHOD";

        StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        for (int i = ste.length-1; i >= 0; i--) {
            if (ste[i].toString().contains(LogUtils.class.getName())) {
                return lastMethod;
            }

            lastMethod = ste[i].getMethodName();
        }

        return lastMethod;
    }
}

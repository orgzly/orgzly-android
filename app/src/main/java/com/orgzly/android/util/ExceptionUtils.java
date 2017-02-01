package com.orgzly.android.util;

import java.io.IOException;

public class ExceptionUtils {
    // TODO: Use more where getCause is used
    public static IOException IOException(Exception e, String msg) {
        if (e.getCause() != null) {
            return new IOException(msg + ": " + e.getCause());
        } else {
            return new IOException(msg + ": " + e.toString());
        }
    }
}

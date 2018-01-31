package com.orgzly.android.util;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.orgzly.android.BookName;

import java.util.List;


public class UriUtils {
    /**
     * Everything except the last path segment.
     */
    public static Uri dirUri(Uri uri) {
        List<String> path = null;

        if (uri.getPathSegments().size() > 0) {
            path = uri.getPathSegments().subList(0, uri.getPathSegments().size() - 1);
        }

        Uri.Builder builder = new Uri.Builder()
                .scheme(uri.getScheme())
                .authority(uri.getAuthority());

        if (path != null) {
            for (String p : path) {
                builder.appendPath(p);
            }
        }

        return builder.build();
    }

    /**
     * Create URI from directory.
     */
    public static Uri uriFromPath(String schema, String directory) {
        Uri.Builder builder = new Uri.Builder().scheme(schema);
        for (String s : directory.split("/+", -1)) {
            builder.appendPath(s);
        }
        return builder.build();
    }

    /**
     * Replaces the name part of the uri, leaving everything (including the extension) the same.
     */
    public static Uri getUriForNewName(Uri uri, String name) {
        BookName bookName = BookName.fromFileName(uri.getLastPathSegment());
        BookName.Format format = bookName.getFormat();

        String newFilename = BookName.fileName(name, format);

        return UriUtils.dirUri(uri) // Old Uri without file name
                .buildUpon()
                .appendPath(newFilename) // New file name
                .build();
    }
}

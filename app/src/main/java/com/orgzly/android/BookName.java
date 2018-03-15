package com.orgzly.android;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.orgzly.BuildConfig;
import com.orgzly.android.repos.Rook;
import com.orgzly.android.util.LogUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Given a filename determines book's format based on extension.
 * Given a book name and a format - constructs a filename.
 */
public class BookName {
    private static final String TAG = BookName.class.getName();

    private static final Pattern PATTERN = Pattern.compile("(.*)\\.(org)(\\.txt)?$");
    private static final Pattern SKIP_PATTERN = Pattern.compile("^\\.#.*");

    private final String mFileName;
    private final String mName;
    private final Format mFormat;

    private BookName(String fileName, String name, Format format) {
        mFileName = fileName;
        mName = name;
        mFormat = format;
    }

    public static String getFileName(Context context, Book book) {
//        if (book.hasRook()) {
//            return getFileName(context, book.getRook().getUri());
//
//        } else
        if (book.getLastSyncedToRook() != null) {
            return getFileName(context, book.getLastSyncedToRook().getUri());

        } else {
            return fileName(book.getName(), Format.ORG);
        }
    }

    public static String getFileName(Context context, Uri uri) {
        String fileName;

        DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);

        if ("content".equals(uri.getScheme()) && documentFile != null) {
            // Try using DocumentFile first (KitKat and above)
            fileName = documentFile.getName();

        } else { // Just get the last path segment
            fileName = uri.getLastPathSegment();
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(
                TAG,
                uri,
                documentFile,
                documentFile != null ? documentFile.getName() : "no-doc-no-name",
                fileName);

        return fileName;
    }

    public static BookName getInstance(Context context, Rook rook) {
        return fromFileName(getFileName(context, rook.getUri()));
    }

    public static boolean isSupportedFormatFileName(String fileName) {
        return PATTERN.matcher(fileName).matches() && !SKIP_PATTERN.matcher(fileName).matches();
    }

    public static String fileName(String name, Format format) {
        if (format == Format.ORG) {
            return name + ".org";

        } else {
            throw new IllegalArgumentException("Unsupported format " + format);
        }
    }

    public static BookName fromFileName(String fileName) {
        if (fileName != null) {
            Matcher m = PATTERN.matcher(fileName);

            if (m.find()) {
                String name = m.group(1);
                String extension = m.group(2);

                if (extension.equals("org")) {
                    return new BookName(fileName, name, Format.ORG);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported book file name " + fileName);
    }

    public String getName() {
        return mName;
    }

    public Format getFormat() {
        return mFormat;
    }

    public String getFileName() {
        return mFileName;
    }

    public enum Format {
        ORG
    }
}

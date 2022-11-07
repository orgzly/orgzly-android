package com.orgzly.android;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

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
    private final BookFormat mFormat;

    private BookName(String fileName, String name, BookFormat format) {
        mFileName = fileName;
        mName = name;
        mFormat = format;
    }

    public static String getFileName(Context context, com.orgzly.android.db.entity.BookView bookView) {
        if (bookView.getSyncedTo() != null) {
            return getFileName(context, bookView.getSyncedTo().getUri());

        } else {
            return fileName(bookView.getBook().getName(), BookFormat.ORG);
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
                fileName);

        return fileName;
    }

    public static BookName getInstance(Context context, Rook rook) {
        return fromFileName(getFileName(context, rook.getUri()));
    }

    public static boolean isSupportedFormatFileName(String fileName) {
        return PATTERN.matcher(fileName).matches() && !SKIP_PATTERN.matcher(fileName).matches();
    }

    public static String fileName(String name, BookFormat format) {
        if (format == BookFormat.ORG) {
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
                    return new BookName(fileName, name, BookFormat.ORG);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported book file name " + fileName);
    }

    public String getName() {
        return mName;
    }

    public BookFormat getFormat() {
        return mFormat;
    }

    public String getFileName() {
        return mFileName;
    }

}

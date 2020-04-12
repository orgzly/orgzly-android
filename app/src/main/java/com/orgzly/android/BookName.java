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

    private static final Pattern PATTERN = Pattern.compile("(.*)\\.(org)(\\.txt)?(\\.gpg)?$"); // todo (.gpg)
    private static final Pattern SKIP_PATTERN = Pattern.compile("^\\.#.*");
    // todo private static final Pattern GPG_PATTERN = Pattern.compile("(.*)(\\.gpg)$");
    // todo ?what happens with .org.txt.gpg files

    private final String mFileName;
    private final String mName;
    private final BookFormat mFormat;
    private final boolean mEncrypted;

    private BookName(String fileName, String name, BookFormat format, boolean encrypted) {
        mFileName = fileName;
        mName = name;
        mFormat = format;
        mEncrypted = encrypted;
    }

    public static String getFileName(Context context, com.orgzly.android.db.entity.BookView bookView) {

//        if (bookView.getSyncedTo() != null) { // todo and if the encryption state of this file fits. or make sure that
//            // todo after encryption toggle syncedTo should be deleted
//            return getFileName(context, bookView.getSyncedTo().getUri());
//
//        }
//        else
            {
                return fileName(bookView.getBook().getName(), BookFormat.ORG, bookView.hasEncryption());
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

    public static boolean getEncrypted(Context context, Uri uri) {
        String fileName = getFileName(context, uri);
        return ((fileName.length() > 4)
                && fileName.substring(fileName.length() - 4).equals(".gpg"));
        // todo replace by matcher
    }

    public static BookName getInstance(Context context, Rook rook) {
        return fromFileName(getFileName(context, rook.getUri()));
    }

    public static boolean isSupportedFormatFileName(String fileName) {
        return PATTERN.matcher(fileName).matches() && !SKIP_PATTERN.matcher(fileName).matches();
    }

    // todo $!or introduce filename without .gpg ending to which the caller can add their .gpg
    public static String fileName(String name, BookFormat format, boolean encrypted) {
        String fullName = name;
        if (format == BookFormat.ORG) {
            fullName += ".org";
        } else {
            throw new IllegalArgumentException("Unsupported format " + format);
        }

        if (encrypted) {
            fullName += ".gpg";
        }

        return fullName;
    }

    public static BookName fromFileName(String fileName) {
        if (fileName != null) {
            Matcher m = PATTERN.matcher(fileName);

            if (m.find()) {
                String name = m.group(1);
                String extension = m.group(2);
                String lastExtension = m.group(m.groupCount()); // todo ?needed

                if (extension.equals("org")) {
                    boolean encrypted = (lastExtension != null && lastExtension.equals(".gpg"));

                    return new BookName(fileName, name, BookFormat.ORG, encrypted);
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

    public boolean getEncrypted() {
        return mEncrypted;
    }
}

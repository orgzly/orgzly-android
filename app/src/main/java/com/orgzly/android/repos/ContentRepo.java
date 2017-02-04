package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.BookName;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Using DocumentFile, for devices running Lollipop or later.
 */
public class ContentRepo implements Repo {
    private static final String TAG = ContentRepo.class.getName();

    public static final String SCHEME = "content";

    private final Context context;
    private final Uri repoUri;

    private final DocumentFile repoDocumentFile;

    public ContentRepo(Context context, Uri uri) throws IOException {
        this.context = context;
        this.repoUri = uri;

        this.repoDocumentFile = DocumentFile.fromTreeUri(context, uri);
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return repoUri;
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        List<VersionedRook> result = new ArrayList<>();

        DocumentFile[] files = repoDocumentFile.listFiles();

        if (files != null) {
            // Can't compare TreeDocumentFile
            // Arrays.sort(files);

            for (int i = 0; i < files.length; i++) {
                DocumentFile file = files[i];

                if (BookName.isSupportedFormatFileName(file.getName())) {

                    if (BuildConfig.LOG_DEBUG) {
                        LogUtils.d(TAG,
                                "file.getName()", file.getName(),
                                "getUri()", getUri(),
                                "repoDocumentFile.getUri()", repoDocumentFile.getUri(),
                                "file", file,
                                "file.getUri()", file.getUri(),
                                "file.getParentFile()", file.getParentFile().getUri());
                    }

                    result.add(new VersionedRook(
                            getUri(),
                            file.getUri(),
                            String.valueOf(file.lastModified()),
                            file.lastModified()
                    ));
                }
            }

        } else {
            Log.e(TAG, "Listing files in " + getUri() + " returned null.");
        }

        return result;
    }

    @Override
    public VersionedRook retrieveBook(Rook rook, File destinationFile) throws IOException {
        DocumentFile sourceFile = DocumentFile.fromSingleUri(context, rook.getUri());

        /* "Download" the file. */
        InputStream is = context.getContentResolver().openInputStream(rook.getUri());
        try {
            MiscUtils.writeStreamToFile(is, destinationFile);
        } finally {
            is.close();
        }

        String rev = String.valueOf(sourceFile.lastModified());
        long mtime = sourceFile.lastModified();

        return new VersionedRook(rook, rev, mtime);
    }

    /**
     *
     * @param path Full path where to store the file, including the file name
     */
    @Override
    public VersionedRook storeBook(File file, String path) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file + " does not exist");
        }

        // Delete existing file
        DocumentFile existingFile = repoDocumentFile.findFile(path);
        if (existingFile != null) {
            existingFile.delete();
        }

        // Create new file
        DocumentFile destinationFile = repoDocumentFile.createFile("text/*", path);

        Uri uri = destinationFile.getUri();

        /* Write file content to uri. */
        OutputStream out = context.getContentResolver().openOutputStream(uri);
        try {
            MiscUtils.writeFileToStream(file, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        String rev = String.valueOf(destinationFile.lastModified());
        long mtime = destinationFile.lastModified();

        return new VersionedRook(getUri(), uri, rev, mtime);
    }

    @Override
    public VersionedRook moveBook(Uri from, Uri to) throws IOException {
        File fromFile = new File(from.getPath());
        File toFile = new File(to.getPath());

        if (toFile.exists()) {
            throw new IOException("File " + toFile + " already exists");
        }

        if (! fromFile.renameTo(toFile)) {
            throw new IOException("Failed renaming " + fromFile + " to " + toFile);
        }

        String rev = String.valueOf(toFile.lastModified());
        long mtime = toFile.lastModified();

        return new VersionedRook(getUri(), to, rev, mtime);
    }

    @Override
    public void delete(String path) throws IOException {
        DocumentFile file = repoDocumentFile.findFile(path);

        if (file.exists()) {
            if (! file.delete()) {
                throw new IOException("Failed deleting file " + path);
            }
        }
    }

    @Override
    public String toString() {
        return getUri().toString();
    }
}

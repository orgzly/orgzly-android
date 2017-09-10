package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.orgzly.android.App;
import com.orgzly.BuildConfig;
import com.orgzly.android.BookName;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.HashUtils;
import com.orgzly.android.provider.clients.CurrentRooksClient;

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

                    VersionedRook lastRook = CurrentRooksClient.get(App.getAppContext() , getUri().toString());
                    String rev = getRevision(file);
                    long mtime;
                    if (lastRook == null || !rev.equals(lastRook.getRevision())) {
                        mtime = System.currentTimeMillis();
                    } else {
                        mtime = lastRook.getMtime();
                    }
                    result.add(new VersionedRook(
                            getUri(),
                            file.getUri(),
                            rev,
                            mtime
                    ));
                }
            }

        } else {
            Log.e(TAG, "Listing files in " + getUri() + " returned null.");
        }

        return result;
    }

    @Override
    public VersionedRook retrieveBook(Uri uri, File destinationFile) throws IOException {
        DocumentFile sourceFile = DocumentFile.fromSingleUri(context, uri);

        /* "Download" the file. */
        InputStream is = context.getContentResolver().openInputStream(uri);
        try {
            MiscUtils.writeStreamToFile(is, destinationFile);
        } finally {
            is.close();
        }

        String rev = getRevision(sourceFile);

        VersionedRook lastRook = CurrentRooksClient.get(App.getAppContext() , uri.toString());
        long mtime;
        if (lastRook == null || !rev.equals(lastRook.getRevision())) {
            mtime = System.currentTimeMillis();
        } else {
            mtime = lastRook.getMtime();
        }
        return new VersionedRook(repoUri, uri, rev, mtime);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file + " does not exist");
        }

        /* Delete existing file. */
        DocumentFile existingFile = repoDocumentFile.findFile(fileName);
        if (existingFile != null) {
            existingFile.delete();
        }

        /* Create new file. */
        DocumentFile destinationFile = repoDocumentFile.createFile("text/*", fileName);

        if (destinationFile == null) {
            throw new IOException("Failed creating " + fileName + " in " + repoUri);
        }

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

        long mtime = System.currentTimeMillis(); // destinationFile.lastModified();
        String rev = getRevision(destinationFile);

        return new VersionedRook(getUri(), uri, rev, mtime);
    }

    @Override
    public VersionedRook renameBook(Uri from, String name) throws IOException {
        DocumentFile fromDocFile = DocumentFile.fromSingleUri(context, from);
        BookName bookName = BookName.fromFileName(fromDocFile.getName());
        String newFileName = BookName.fileName(name, bookName.getFormat());

        /* Check if document already exists. */
        DocumentFile existingFile = repoDocumentFile.findFile(newFileName);
        if (existingFile != null) {
            throw new IOException("File at " + existingFile.getUri() + " already exists");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri newUri = DocumentsContract.renameDocument(context.getContentResolver(), from, newFileName);

            long mtime = System.currentTimeMillis(); // destinationFile.lastModified();
            String rev = getRevision(fromDocFile);

            return new VersionedRook(getUri(), newUri, rev, mtime);

        } else {
            /*
             * This should never happen, unless the user downgraded
             * and uses the same repo uri.
             */
            throw new IOException("Renaming notebooks is not supported on your device " +
                                  "(requires at least Lollipop)");
        }
    }

    @Override
    public void delete(Uri uri) throws IOException {
        DocumentFile docFile = DocumentFile.fromSingleUri(context, uri);

        if (docFile != null && docFile.exists()) {
            if (! docFile.delete()) {
                throw new IOException("Failed deleting document " + uri);
            }
        }
    }

    @Override
    public String toString() {
        return getUri().toString();
    }

    private String getRevision(DocumentFile arg) throws IOException {
        InputStream istream = context.getContentResolver().openInputStream(arg.getUri());
        return HashUtils.MD5.checksum(istream);
    }
}

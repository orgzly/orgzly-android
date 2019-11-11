package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.BookName;
import com.orgzly.android.db.entity.Repo;
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
public class ContentRepo implements SyncRepo {
    private static final String TAG = ContentRepo.class.getName();

    public static final String SCHEME = "content";

    private final long repoId;
    private final Uri repoUri;

    private final Context context;

    private final DocumentFile repoDocumentFile;

    public ContentRepo(RepoWithProps repoWithProps, Context context) {
        Repo repo = repoWithProps.getRepo();

        this.repoId = repo.getId();
        this.repoUri = Uri.parse(repo.getUrl());

        this.context = context;

        this.repoDocumentFile = DocumentFile.fromTreeUri(context, repoUri);
    }

    @Override
    public boolean isConnectionRequired() {
        return false;
    }

    @Override
    public boolean isAutoSyncSupported() {
        return true;
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

            for (DocumentFile file : files) {
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
                            repoId,
                            RepoType.DOCUMENT,
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
    public VersionedRook retrieveBook(String fileName, File destinationFile) throws IOException {
        DocumentFile sourceFile = repoDocumentFile.findFile(fileName);
        if (sourceFile == null) {
            throw new FileNotFoundException("Book " + fileName + " not found in " + repoUri);
        } else {
            if (BuildConfig.LOG_DEBUG) {
                LogUtils.d(TAG, "Found DocumentFile for " + fileName + ": " + sourceFile.getUri());
            }
        }

        /* "Download" the file. */
        try (InputStream is = context.getContentResolver().openInputStream(sourceFile.getUri())) {
            MiscUtils.writeStreamToFile(is, destinationFile);
        }

        String rev = String.valueOf(sourceFile.lastModified());
        long mtime = sourceFile.lastModified();

        return new VersionedRook(repoId, RepoType.DOCUMENT, repoUri, sourceFile.getUri(), rev, mtime);
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

        String rev = String.valueOf(destinationFile.lastModified());
        long mtime = System.currentTimeMillis();

        return new VersionedRook(repoId, RepoType.DOCUMENT, getUri(), uri, rev, mtime);
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

            long mtime = fromDocFile.lastModified();
            String rev = String.valueOf(mtime);

            return new VersionedRook(repoId, RepoType.DOCUMENT, getUri(), newUri, rev, mtime);

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
}

package com.orgzly.android;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;

import java.io.File;
import java.io.IOException;


/**
 * Provides directories for various different types of files that needs to be stored locally.
 * Prefers cache directories for temporary files, external storage for large files etc.
 */
public class LocalStorage {
    private static final String TAG = LocalStorage.class.getName();

    private Context mContext;

    public LocalStorage(Context context) {
        mContext = context;
    }

    /**
     * Get file to which {@link Book} will be exported.
     * @param format book's format
     * @throws IOException if external directory is not available
     */
    public File getExportFile(Book book, BookName.Format format) throws IOException {
        return new File(downloadsDirectory(), BookName.fileName(book.getName(), format));
    }

    /**
     * Get temporary {@code File} for storing {@link Book}'s content.
     * @throws IOException
     */
    public File getTempBookFile() throws IOException {
        File baseDir;
        if (isExternalStorageWritable()) {
            baseDir = externalCacheDir("tmp");
        } else {
            baseDir = internalCacheDir("tmp");
        }

        try {
            return File.createTempFile("notebook.", ".tmp", baseDir);
        } catch (IOException e) {
            throw new IOException("Failed creating temporary file in " + baseDir + ": " + e.getMessage());
        }
    }

    public File getLocalRepoDirectory(String dir) throws IOException {
        File baseDir;
        if (isExternalStorageWritable()) {
            baseDir = externalCacheDir(dir);
        } else {
            baseDir = internalCacheDir(dir);
        }

        return baseDir;
    }

    /**
     * Export directory.
     */
    public File downloadsDirectory() throws IOException {
        if (!isExternalStorageWritable()) {
            throw new IOException(mContext.getString(R.string.primary_storage_not_available));
        }

        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_NAME);

        if (! file.isDirectory()) {
            if (! file.mkdirs()) {
                throw new IOException("Failed creating directory " + file);
            }
        }

        return file;
    }

    private File externalCacheDir(String dir) throws IOException {
        File file = new File(mContext.getExternalCacheDir(), dir);

        if (! file.isDirectory()) {
            if (! file.mkdirs()) {
                throw new IOException("Failed creating directory " + file);
            }
        }

        return file;
    }

    private File internalCacheDir(String dir) throws IOException {
        File file = new File(mContext.getCacheDir(), dir);

        if (! file.isDirectory()) {
            if (! file.mkdirs()) {
                throw new IOException("Failed creating directory " + file);
            }
        }

        return file;
    }

    /* Checks if external storage is available for read and write */
    // TODO: What is there is no external storage?
    // Must use internal but remember that by saving to
    // preferences or something when initializing for the first time?
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Removes all temporary and cache files.
     */
    public void cleanup() {
        deleteRecursive(mContext.getCacheDir());

        if (isExternalStorageWritable()) {
            deleteRecursive(mContext.getExternalCacheDir());
        }
    }

    public static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String aChildren : children) {
                deleteRecursive(new File(file, aChildren));
            }
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Deleting " + file + " ...");

        if (! file.delete()) {
            Log.e(TAG, "Failed deleting " + file);
        }
    }
}

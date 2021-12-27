package com.orgzly.android;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
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
     * Get file to which book with specified name will be exported to.
     * @throws IOException if external directory is not available
     */
    public File getExportFile(String name, BookFormat format) throws IOException {
        return new File(downloadsDirectory(), BookName.fileName(name, format));
    }

    /**
     * Get temporary {@code File} for storing book's content.
     */
    public File getTempBookFile() throws IOException {
        File dir = getCacheDirectory("notebooks");

        try {
            return File.createTempFile("notebook.", ".tmp", dir);
        } catch (IOException e) {
            throw new IOException("Failed creating temporary file in " + dir + ": " + e.getMessage());
        }
    }

    public File getCacheDirectory(String child) throws IOException {
        File dir = internalCacheDir(child);

        if (dir == null) {
            throw new IOException("Failed to get cache directory " + child);
        }

        return dir;
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

    /**
     * File in Download/ directory.
     */
    public File downloadsDirectory(String fileName) throws IOException {
        return new File(downloadsDirectory(), fileName);
    }

    private File externalCacheDir(String child) {
        if (!isExternalStorageWritable()) {
            return null;
        }

        File baseDir = mContext.getExternalCacheDir();

        if (baseDir == null) {
            return null;
        }

        if (child != null) {
            File dir = new File(baseDir, child);

            if (!dir.isDirectory()) {
                if (!dir.mkdirs()) {
                    return null;
                }
            }

            return dir;

        } else {
            return baseDir;
        }
    }

    private File internalCacheDir(String dir) {
        File file = new File(mContext.getCacheDir(), dir);

        if (! file.isDirectory()) {
            if (! file.mkdirs()) {
                return null;
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

        File dir = externalCacheDir(null);
        if (dir != null) {
            deleteRecursive(dir);
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

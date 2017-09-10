package com.orgzly.android.repos;

import android.net.Uri;
import android.util.Log;

import com.orgzly.android.App;
import com.orgzly.android.BookName;
import com.orgzly.android.LocalStorage;
import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;
import com.orgzly.android.util.HashUtils;
import com.orgzly.android.provider.clients.CurrentRooksClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryRepo implements Repo {
    private static final String TAG = DirectoryRepo.class.getName();

    public static final String SCHEME = "file";

    private File mDirectory;

    private final Uri repoUri;

    /**
     *
     * @param url repo url, in the format (file:/a/b/c)
     * @param wipe should files be deleted first from directory
     */
    public DirectoryRepo(String url, boolean wipe) throws IOException {
        repoUri = Uri.parse(url);

        mDirectory = new File(repoUri.getPath());

        /* Delete entire contents of directory. */
        if (wipe) {
            LocalStorage.deleteRecursive(mDirectory);
        }

        createDir(mDirectory);
    }

    private void createDir(File dir) throws IOException {
        if (! dir.isDirectory()) {
            if (! dir.mkdirs()) {
                throw new IOException("Failed creating directory " + dir);
            }
        }
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

        File[] files = mDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return BookName.isSupportedFormatFileName(filename);
            }
        });

        if (files != null) {
            Arrays.sort(files);

            for (int i = 0; i < files.length; i++) {
                Uri uri = repoUri.buildUpon().appendPath(files[i].getName()).build();

                VersionedRook lastRook = CurrentRooksClient.get(App.getAppContext() , uri.toString());
                String rev = getRevision(files[i]);
                long mtime;
                if (lastRook == null || !rev.equals(lastRook.getRevision())) {
                    mtime = System.currentTimeMillis();
                } else {
                    mtime = lastRook.getMtime();
                }
                result.add(new VersionedRook(
                        repoUri,
                        uri,
                        rev,
                        mtime
                ));
            }

        } else {
            Log.e(TAG, "Listing files in " + mDirectory + " returned null. No storage permission?");
        }

        return result;
    }

    @Override
    public VersionedRook retrieveBook(Uri uri, File destinationFile) throws IOException {
        File sourceFile = new File(uri.getPath());

        /* "Download" the file. */
        MiscUtils.copyFile(sourceFile, destinationFile);

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

        File destinationFile = new File(mDirectory, fileName);

        /* Create necessary directories. */
        createDir(destinationFile.getParentFile());

        /* "Upload" the file. */
        MiscUtils.copyFile(file, destinationFile);

        long mtime = System.currentTimeMillis();
        String rev = getRevision(destinationFile);

        Uri uri = repoUri.buildUpon().appendPath(fileName).build();

        return new VersionedRook(repoUri, uri, rev, mtime);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        File fromFile = new File(fromUri.getPath());
        Uri newUri = UriUtils.getUriForNewName(fromUri, name);
        File toFile = new File(newUri.getPath());

        if (toFile.exists()) {
            throw new IOException("File " + toFile + " already exists");
        }

        if (! fromFile.renameTo(toFile)) {
            throw new IOException("Failed renaming " + fromFile + " to " + toFile);
        }
        long mtime = System.currentTimeMillis();
        String rev = getRevision(toFile);

        return new VersionedRook(repoUri, newUri, rev, mtime);
    }

    @Override
    public void delete(Uri uri) throws IOException {
        File file = new File(uri.getPath());

        if (file.exists()) {
            if (! file.delete()) {
                throw new IOException("Failed deleting file " + uri.getPath());
            }
        }
    }

    public File getDirectory() {
        return mDirectory;
    }

    @Override
    public String toString() {
        return repoUri.toString();
    }

    private String getRevision(File arg) throws IOException {
        if (BuildConfig.LOG_DEBUG) LogUtils.d("revision: ", HashUtils.MD5.checksum(arg));
        return HashUtils.MD5.checksum(arg);
    }
}

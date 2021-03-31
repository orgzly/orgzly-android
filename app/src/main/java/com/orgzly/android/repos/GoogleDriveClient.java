package com.orgzly.android.repos;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.orgzly.android.BookName;

import java.util.Collections;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.util.Log;

public class GoogleDriveClient {
    private static final String TAG = GoogleDriveClient.class.getName();

    private static final long UPLOAD_FILE_SIZE_LIMIT = 150; // MB

    // TODO: Throw GoogleDriveNotLinked etc. instead and let the client get message from resources
    private static final String NOT_LINKED = "Not linked to Google Drive";
    private static final String LARGE_FILE = "File larger then " + UPLOAD_FILE_SIZE_LIMIT + " MB";

    /* The empty string ("") represents the root folder in Google Drive API v2. */
    private static final String ROOT_PATH = "";

    private final Context mContext;
    private final long repoId;
    private Drive mDriveService;

    public GoogleDriveClient(Context context, long id) {
        mContext = context;

        repoId = id;
    }

    public void setService(Drive driveService) {
        mDriveService = driveService;
    }

    public boolean isLinked() {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        return GoogleSignIn.getLastSignedInAccount(mContext) != null;
    }

    private void linkedOrThrow() throws IOException {
        if (! isLinked()) {
            throw new IOException(NOT_LINKED);
        }
    }

    public List<VersionedRook> getBooks(Uri repoUri) throws IOException {
        return null;
        // linkedOrThrow();
        //
        // List<VersionedRook> list = new ArrayList<>();
        //
        // String path = repoUri.getPath();
        //
        // /* Fix root path. */
        // if (path == null || path.equals("/")) {
        //     path = ROOT_PATH;
        // }
        //
        // /* Strip trailing slashes. */
        // path = path.replaceAll("/+$", "");
        //
        // try {
        //     // TODO: Alter to fit for google
        //     if (ROOT_PATH.equals(path) || dbxClient.files().getMetadata(path) instanceof FolderMetadata) {
        //         /* Get folder content. */
        //         ListFolderResult result = dbxClient.files().listFolder(path);
        //         while (true) {
        //             for (Metadata metadata : result.getEntries()) {
        //                 if (metadata instanceof FileMetadata) {
        //                     FileMetadata file = (FileMetadata) metadata;
        //
        //                     if (BookName.isSupportedFormatFileName(file.getName())) {
        //                         Uri uri = repoUri.buildUpon().appendPath(file.getName()).build();
        //                         VersionedRook book = new VersionedRook(
        //                                 repoId,
        //                                 RepoType.DROPBOX,
        //                                 repoUri,
        //                                 uri,
        //                                 file.getRev(),
        //                                 file.getServerModified().getTime());
        //
        //                         list.add(book);
        //                     }
        //                 }
        //             }
        //
        //             if (!result.getHasMore()) {
        //                 break;
        //             }
        //
        //             result = dbxClient.files().listFolderContinue(result.getCursor());
        //         }
        //
        //     } else {
        //         throw new IOException("Not a directory: " + repoUri);
        //     }
        //
        // } catch (DbxException e) {
        //     e.printStackTrace();
        //
        //     /* If we get NOT_FOUND from Dropbox, just return the empty list. */
        //     if (e instanceof GetMetadataErrorException) {
        //         if (((GetMetadataErrorException) e).errorValue.getPathValue() == LookupError.NOT_FOUND) {
        //             return list;
        //         }
        //     }
        //
        //     throw new IOException("Failed getting the list of files in " + repoUri +
        //                           " listing " + path + ": " +
        //                           (e.getMessage() != null ? e.getMessage() : e.toString()));
        // }
        //
        // return list;
    }

    /**
     * Download file from Dropbox and store it to a local file.
     */
    public VersionedRook download(Uri repoUri, String fileName, File localFile) throws IOException {
        return null;
        // linkedOrThrow();
        //
        // Uri uri = repoUri.buildUpon().appendPath(fileName).build();
        //
        // OutputStream out = new BufferedOutputStream(new FileOutputStream(localFile));
        //
        // try {
        //     // TODO: Alter to fit for google
        //     Metadata pathMetadata = dbxClient.files().getMetadata(uri.getPath());
        //
        //     if (pathMetadata instanceof FileMetadata) {
        //         FileMetadata metadata = (FileMetadata) pathMetadata;
        //
        //         String rev = metadata.getRev();
        //         long mtime = metadata.getServerModified().getTime();
        //
        //         dbxClient.files().download(metadata.getPathLower(), rev).download(out);
        //
        //         return new VersionedRook(repoId, RepoType.DROPBOX, repoUri, uri, rev, mtime);
        //
        //     } else {
        //         throw new IOException("Failed downloading Dropbox file " + uri + ": Not a file");
        //     }
        //
        // } catch (DbxException e) {
        //     if (e.getMessage() != null) {
        //         throw new IOException("Failed downloading Dropbox file " + uri + ": " + e.getMessage());
        //     } else {
        //         throw new IOException("Failed downloading Dropbox file " + uri + ": " + e.toString());
        //     }
        // } finally {
        //     out.close();
        // }
    }


    /** Upload file to Dropbox. */
    public VersionedRook upload(File file, Uri repoUri, String fileName) throws IOException {
        return null;
        // linkedOrThrow();
        //
        // Uri bookUri = repoUri.buildUpon().appendPath(fileName).build();
        //
        // if (file.length() > UPLOAD_FILE_SIZE_LIMIT * 1024 * 1024) {
        //     throw new IOException(LARGE_FILE);
        // }
        //
        // FileMetadata metadata;
        // InputStream in = new FileInputStream(file);
        //
        // try {
        //     // TODO: Alter to fit for google
        //     metadata = dbxClient.files()
        //             .uploadBuilder(bookUri.getPath())
        //             .withMode(WriteMode.OVERWRITE)
        //             .uploadAndFinish(in);
        //
        // } catch (DbxException e) {
        //     if (e.getMessage() != null) {
        //         throw new IOException("Failed overwriting " + bookUri.getPath() + " on Dropbox: " + e.getMessage());
        //     } else {
        //         throw new IOException("Failed overwriting " + bookUri.getPath() + " on Dropbox: " + e.toString());
        //     }
        // }
        //
        // String rev = metadata.getRev();
        // long mtime = metadata.getServerModified().getTime();
        //
        // return new VersionedRook(repoId, RepoType.DROPBOX, repoUri, bookUri, rev, mtime);
    }

    public void delete(String path) throws IOException {
        // linkedOrThrow();
        //
        // try {
        //     // TODO: Alter to fit for google
        //     if (dbxClient.files().getMetadata(path) instanceof FileMetadata) {
        //         dbxClient.files().deleteV2(path);
        //     } else {
        //         throw new IOException("Not a file: " + path);
        //     }
        //
        // } catch (DbxException e) {
        //     e.printStackTrace();
        //
        //     if (e.getMessage() != null) {
        //         throw new IOException("Failed deleting " + path + " on Dropbox: " + e.getMessage());
        //     } else {
        //         throw new IOException("Failed deleting " + path + " on Dropbox: " + e.toString());
        //     }
        // }
    }

    public VersionedRook move(Uri repoUri, Uri from, Uri to) throws IOException {
        return null;
        // linkedOrThrow();
        //
        // try {
        //     // TODO: Alter to fit for google
        //     RelocationResult relocationRes = dbxClient.files().moveV2(from.getPath(), to.getPath());
        //     Metadata metadata = relocationRes.getMetadata();
        //
        //     if (! (metadata instanceof FileMetadata)) {
        //         throw new IOException("Relocated object not a file?");
        //     }
        //
        //     FileMetadata fileMetadata = (FileMetadata) metadata;
        //
        //     String rev = fileMetadata.getRev();
        //     long mtime = fileMetadata.getServerModified().getTime();
        //
        //     return new VersionedRook(repoId, RepoType.DROPBOX, repoUri, to, rev, mtime);
        //
        // } catch (Exception e) {
        //     e.printStackTrace();
        //
        //     if (e.getMessage() != null) { // TODO: Move this throwing to utils
        //         throw new IOException("Failed moving " + from + " to " + to + ": " + e.getMessage(), e);
        //     } else {
        //         throw new IOException("Failed moving " + from + " to " + to + ": " + e.toString(), e);
        //     }
        // }
    }
}

package com.orgzly.android.repos;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.orgzly.android.BookName;

import java.io.BufferedOutputStream;
// import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

public class GoogleDriveClient {
    private static final String TAG = GoogleDriveClient.class.getName();

    private static final long UPLOAD_FILE_SIZE_LIMIT = 150; // MB

    // TODO: Throw GoogleDriveNotLinked etc. instead and let the client get message from resources
    private static final String NOT_LINKED = "Not linked to Google Drive";
    private static final String LARGE_FILE = "File larger then " + UPLOAD_FILE_SIZE_LIMIT + " MB";

    /* Using the empty string ("") to represent the root folder. */
    private static final String ROOT_PATH = "";

    private final Context mContext;
    private final long repoId;

    private GoogleSignInAccount mGoogleAccount;
    private Drive mDriveService;

    // Make static? Or maybe need to serialize or manage token.
    // SharedPreferences or SQLite
    // https://stackoverflow.com/questions/19274063/object-becomes-null
    // Thought that Google sign-in would handle it, but it's not working or not building.

    private Map<String, String> pathIds;
    {
        pathIds = new HashMap<>();
        pathIds.put("My Drive", "root");
        pathIds.put("", "root");
    }

    public GoogleDriveClient(Context context, long id) {
        mContext = context;

        repoId = id;
    }

    public boolean isLinked() {
        return setService();
    }

    private void linkedOrThrow() throws IOException {
        if (! isLinked()) {
            throw new IOException(NOT_LINKED);
        }
    }

    public boolean setService() {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        if (mDriveService == null) {
            mGoogleAccount = GoogleSignIn.getLastSignedInAccount(mContext);
            if (mGoogleAccount != null) {
                // Use the authenticated account to sign in to the Drive service.
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(mContext, Collections.singleton(DriveScopes.DRIVE));
                credential.setSelectedAccount(mGoogleAccount.getAccount());
                mDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                                              new GsonFactory(),
                                              credential)
                    .setApplicationName("Orgzly")
                    .build();
            }
        }
        return mDriveService != null;
    }

    private String findId(String path) throws IOException {
        if (pathIds.containsKey(path)) {
            return pathIds.get(path);
        }

        String[] parts = path.split("/");
        String[] ids = new String[parts.length+1];

        ids[0] = "root";

        for (int i=0; i < parts.length; ++i) {
            FileList result = mDriveService.files().list()
                .setQ(String.format("name = '%s' and '%s' in parents", parts[i], ids[i]))
                .setSpaces("drive")
                .setFields("files(id, name, mimeType)")
                .execute();
            List files = result.getFiles();
            if (!files.isEmpty()) {
                File file = (File) files.get(0);
                ids[i+1] = file.getId();
            }
        }

        for (int i = 0; i < ids.length; ++i) {
            if (ids[i] == null) {
                break;
            }
            pathIds.put(path, ids[i]);
        }

        return ids[ids.length-1]; // Returns null if no file is found
    }

    public List<VersionedRook> getBooks(Uri repoUri) throws IOException {
        linkedOrThrow();

        List<VersionedRook> list = new ArrayList<>();

        String path = repoUri.getPath();

        /* Fix root path. */
        if (path == null || path.equals("/")) {
            path = ROOT_PATH;
        }

        /* Strip trailing slashes. */
        path = path.replaceAll("/+$", "");

        try {

            String folderId = findId(path);


            if (folderId != null) {

                File folder = mDriveService.files().get(folderId)
                    .setFields("id, mimeType")
                    .execute();

                if (folder.getMimeType() == "application/vnd.google-apps.folder") {

                    String pageToken = null;
                    do {
                        FileList result = mDriveService.files().list()
                            .setQ(String.format("mimeType != 'application/vnd.google-apps.folder' " +
                                                "and '%s' in parents and trashed = false", folderId))
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name, mimeType)")
                            .setPageToken(pageToken)
                            .execute();
                        for (File file : result.getFiles()) {
                            if(BookName.isSupportedFormatFileName(file.getName())) {
                                Uri uri = repoUri.buildUpon().appendPath(file.getName()).build();
                                VersionedRook book = new VersionedRook(
                                                                       repoId,
                                                                       RepoType.GOOGLE_DRIVE,
                                                                       repoUri,
                                                                       uri,
                                                                       Long.toString(file.getVersion()),
                                                                       file.getModifiedTime().getValue());

                                list.add(book);
                            }
                        }
                        pageToken = result.getNextPageToken();
                    } while (pageToken != null);

                } else {
                    throw new IOException("Not a directory: " + repoUri);
                }
            } else {
                throw new IOException("Not a directory: " + repoUri);
            }

        } catch (Exception e) {
            e.printStackTrace();

            throw new IOException("Failed getting the list of files in " + repoUri +
                                  " listing " + path + ": " +
                                  (e.getMessage() != null ? e.getMessage() : e.toString()));
        }

        return list;
    }

    /**
     * Download file from Google Drive and store it to a local file.
     */
    public VersionedRook download(Uri repoUri, String fileName, java.io.File localFile) throws IOException {
        linkedOrThrow();

        Uri uri = repoUri.buildUpon().appendPath(fileName).build();

        OutputStream out = new BufferedOutputStream(new FileOutputStream(localFile));

        try {

            String fileId = findId(uri.getPath());

            if (fileId != null) {
                File file = mDriveService.files().get(fileId)
                    .setFields("id, mimeType, version, modifiedDate")
                    .execute();

                if (file.getMimeType() != "application/vnd.google-apps.folder") {

                    String rev = Long.toString(file.getVersion());
                    long mtime = file.getModifiedTime().getValue();

                    mDriveService.files().get(fileId).executeMediaAndDownloadTo(out);

                    return new VersionedRook(repoId, RepoType.GOOGLE_DRIVE, repoUri, uri, rev, mtime);

                } else {
                    throw new IOException("Failed downloading Google Drive file " + uri + ": Not a file");
                }
            } else {
                throw new IOException("Failed downloading Google Drive file " + uri + ": File not found");
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                throw new IOException("Failed downloading Google Drive file " + uri + ": " + e.getMessage());
            } else {
                throw new IOException("Failed downloading Google Drive file " + uri + ": " + e.toString());
            }
        } finally {
            out.close();
        }
    }


    /** Upload file to Google Drive. */
    public VersionedRook upload(java.io.File file, Uri repoUri, String fileName) throws IOException {
        linkedOrThrow();

        Uri bookUri = repoUri.buildUpon().appendPath(fileName).build();

        if (file.length() > UPLOAD_FILE_SIZE_LIMIT * 1024 * 1024) {
            throw new IOException(LARGE_FILE);
        }

        // FileMetadata metadata;
        // InputStream in = new FileInputStream(file);

        File fileMetadata = new File();
        String filePath = bookUri.getPath();

        try {
            fileMetadata.setName(fileName);
            fileMetadata.setTrashed(false);
            FileContent mediaContent = new FileContent("text/plain", file);

            String fileId = findId(filePath);

            if (fileId == null) {
                filePath = "/" + filePath; // Avoids errors when file is in root folder
                String folderPath = filePath.substring(0, filePath.lastIndexOf('/'));
                String folderId = findId(folderPath);

                fileMetadata.setParents(Collections.singletonList(folderId));
                fileMetadata = mDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
                fileId = fileMetadata.getId();

                pathIds.put(filePath, fileId);
            } else {
                fileMetadata = mDriveService.files().update(fileId, fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            }

        } catch (Exception e) {
            if (e.getMessage() != null) {
                throw new IOException("Failed overwriting " + filePath + " on Google Drive: " + e.getMessage());
            } else {
                throw new IOException("Failed overwriting " + filePath + " on Google Drive: " + e.toString());
            }
        }

        String rev = Long.toString(fileMetadata.getVersion());
        long mtime = fileMetadata.getModifiedTime().getValue();

        return new VersionedRook(repoId, RepoType.GOOGLE_DRIVE, repoUri, bookUri, rev, mtime);
    }

    public void delete(String path) throws IOException {
        linkedOrThrow();

        try {
            String fileId = findId(path);

            if (fileId != null) {
                File file = mDriveService.files().get(fileId).setFields("id, mimeType").execute();
                if (file.getMimeType() != "application/vnd.google-apps.folder") {
                    File fileMetadata = new File();
                    fileMetadata.setTrashed(true);
                    mDriveService.files().update(fileId, fileMetadata).execute();
                } else {
                    throw new IOException("Not a file: " + path);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (e.getMessage() != null) {
                throw new IOException("Failed deleting " + path + " on Google Drive: " + e.getMessage());
            } else {
                throw new IOException("Failed deleting " + path + " on Google Drive: " + e.toString());
            }
        }
    }

    public VersionedRook move(Uri repoUri, Uri from, Uri to) throws IOException {
        linkedOrThrow();

        try {
            String fileId = findId(from.getPath());

            File fileMetadata = new File();
            fileMetadata.setName(to.getPath());

            if (fileId != null) {
                fileMetadata = mDriveService.files().update(fileId, fileMetadata)
                    .setFields("id, mimeType, version, modifiedDate")
                    .execute();

                if (fileMetadata.getMimeType() == "application/vnd.google-apps.folder") {
                    throw new IOException("Relocated object not a file?");
                }

            }

            String rev = Long.toString(fileMetadata.getVersion());
            long mtime = fileMetadata.getModifiedTime().getValue();

            return new VersionedRook(repoId, RepoType.DROPBOX, repoUri, to, rev, mtime);

        } catch (Exception e) {
            e.printStackTrace();

            if (e.getMessage() != null) { // TODO: Move this throwing to utils
                throw new IOException("Failed moving " + from + " to " + to + ": " + e.getMessage(), e);
            } else {
                throw new IOException("Failed moving " + from + " to " + to + ": " + e.toString(), e);
            }
        }
    }
}

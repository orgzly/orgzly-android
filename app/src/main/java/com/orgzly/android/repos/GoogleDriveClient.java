package com.orgzly.android.repos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.orgzly.android.BookName;

import java.io.BufferedOutputStream;
// import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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

    /**
     * Note: technically, this is the Google Drive service, but we name it "client" for consistency
     * with the Dropbox code.
     */
    private Drive googleDriveClient;

    private boolean tryLinking = false;

    // Used to launch a new activity
    private static final int REQUEST_CODE_SIGN_IN = 1;

    // Holds the authentication (Google Sign In) client
    private GoogleSignInClient authClient;

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

    /**
     * Requires passing in Activity because it is used to create the authClient.
     */
    public GoogleDriveClient(Context context, long id) {
        mContext = context;

        repoId = id;

        createClient();
    }

    private void createClient() {
        // If the user is already signed in, the GoogleSignInAccount will be non-null.
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);

        if (account != null) {
            googleDriveClient = getNewGoogleDriveClient(account);
        }
    }

    private GoogleSignInClient createAuthClient(Activity activity) {
        var signInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        return GoogleSignIn.getClient(activity, signInOptions);
    }

    public boolean isLinked() {
        return googleDriveClient != null;
    }

    private void linkedOrThrow() throws IOException {
        if (! isLinked()) {
            throw new IOException(NOT_LINKED);
        }
    }

    /**
     * Requires activity in order to create authClient if needed.
     * Return type differs from Dropbox because authClient offers async.
     */
    public Task<Void> unlink(Activity activity) {
        if (authClient == null) {
            authClient = createAuthClient(activity);
        }
        googleDriveClient = null;
        final Task<Void> task = authClient.revokeAccess();
        authClient = null;
        return task;
    }

    /**
     * Unlike Dropbox, returns an Intent.
     */
    public Intent beginAuthentication(Activity activity) {
        tryLinking = true;
        var signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        authClient = GoogleSignIn.getClient(activity, signInOptions);
        return authClient.getSignInIntent();
    }

    /**
     * The logic is slightly different from Dropbox. Rather than FinishAuthentication being called
     * on activity resume, it is called when the sign-in activity returns,
     * when we actually have an account.
     */
    public boolean finishAuthentication(GoogleSignInAccount account) {
        if (googleDriveClient == null && tryLinking) {
            googleDriveClient = getNewGoogleDriveClient(account);
            return true;
        }

        return false;
    }

    public Drive getNewGoogleDriveClient(GoogleSignInAccount account) {
        assert account != null;
        GoogleAccountCredential credential = GoogleAccountCredential
                .usingOAuth2(mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Orgzly")
                .build();
    }

    // no need for setToken, saveToken, getToken, deleteToken

    /**
     * This is unique to Google Drive.
     * @param path A path in x/y/z form
     * @return A Google Drive file ID. (Note that in Google Drive, a folder is a type of file.)
     * @throws IOException On error
     */
    private String findId(String path) throws IOException {
        if (pathIds.containsKey(path)) {
            return pathIds.get(path);
        }

        String[] parts = path.split("/");
        String[] ids = new String[parts.length+1];

        ids[0] = "root";

        for (int i=0; i < parts.length; ++i) {
            FileList result = googleDriveClient.files().list()
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

                File folder = googleDriveClient.files().get(folderId)
                    .setFields("id, mimeType")
                    .execute();

                if (folder.getMimeType() == "application/vnd.google-apps.folder") {

                    String pageToken = null;
                    do {
                        FileList result = googleDriveClient.files().list()
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
                File file = googleDriveClient.files().get(fileId)
                    .setFields("id, mimeType, version, modifiedDate")
                    .execute();

                if (file.getMimeType() != "application/vnd.google-apps.folder") {

                    String rev = Long.toString(file.getVersion());
                    long mtime = file.getModifiedTime().getValue();

                    googleDriveClient.files().get(fileId).executeMediaAndDownloadTo(out);

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
                fileMetadata = googleDriveClient.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
                fileId = fileMetadata.getId();

                pathIds.put(filePath, fileId);
            } else {
                fileMetadata = googleDriveClient.files().update(fileId, fileMetadata, mediaContent)
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
                File file = googleDriveClient.files().get(fileId).setFields("id, mimeType").execute();
                if (file.getMimeType() != "application/vnd.google-apps.folder") {
                    File fileMetadata = new File();
                    fileMetadata.setTrashed(true);
                    googleDriveClient.files().update(fileId, fileMetadata).execute();
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
                fileMetadata = googleDriveClient.files().update(fileId, fileMetadata)
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

    /**
     * This is unique to Google Drive. A special utility to create an empty directory.
     * @return The ID of the directory. TODO: maybe I will use this in future code
     * @throws IOException On error
     */
    public String createDirectory() throws IOException {
        // https://github.com/googleworkspace/android-samples/blob/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample/driveapimigration/DriveServiceHelper.java
        File metadata = new File();
        metadata.setName("Orgzly Files");
        metadata.setMimeType("application/vnd.google-apps.folder");
        File file = googleDriveClient
            .files()
            .create(metadata)
            .setFields("id")
            .execute();
        if (file == null) {
            throw new IOException("Null result when requesting file creation");
        }
        return file.getId();
    }
}

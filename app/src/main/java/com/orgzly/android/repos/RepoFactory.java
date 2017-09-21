package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import com.orgzly.R;
import com.orgzly.android.prefs.RepoPreferences;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

import com.orgzly.BuildConfig;

public class RepoFactory {
    public static Repo getFromUri(Context context, Uri uri) {
        return getFromUri(context, uri.toString());
    }

    // TODO: Better throw exception, not return null?
    public static Repo getFromUri(Context context, String uriString) {
        Uri uri = Uri.parse(uriString);

        if (uri != null && uri.getScheme() != null) { // Make sure uri is valid and has a scheme
            try {
                switch (uri.getScheme()) {
                    case ContentRepo.SCHEME:
                        return new ContentRepo(context, uri);

                    case DropboxRepo.SCHEME:
                        if (! BuildConfig.IS_DROPBOX_ENABLED) {
                            return null;
                        }

                        /* There should be no authority. */
                        if (uri.getAuthority() != null) {
                            return null;
                        }
                        return new DropboxRepo(context, uri);
                    case GitRepo.SCHEME:
                        return buildGitRepo(context, uri);
                    case DirectoryRepo.SCHEME:
                        return new DirectoryRepo(uriString, false);

                    case MockRepo.SCHEME:
                        return new MockRepo(context, uriString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    static GitRepo buildGitRepo(Context context, Uri uri) {
        RepoPreferences prefs = RepoPreferences.fromUri(context, uri);
        prefs.getStringValueWithGlobalDefault(R.string.pref_key_git_author);
    }

    static boolean isRepo(FileRepositoryBuilder frb, File f) {
        frb.addCeilingDirectory(f).findGitDir(f);
        return frb.getGitDir() != null;
    }

    static Uri buildDirectoryUri(Uri gitUri) {
        String filepath = gitUri.getSchemeSpecificPart().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        return new Uri.Builder().scheme("file").path(filepath).build();
    }

    public static Git ensureRepositoryExists(Uri repoUri, File directoryFile) throws IOException {
        FileRepositoryBuilder frb = new FileRepositoryBuilder();
        if (!directoryFile.exists()) {
            try {
                return Git.cloneRepository().setURI(repoUri.getPath()).setDirectory(directoryFile).call();
            } catch (GitAPIException e) {
                throw new IOException("Failed to clone repository " + repoUri.toString());
            }
        } else if (!isRepo(frb, directoryFile)) {
            throw new IOException(
                    String.format("Directory %s is not a git repository.", directoryFile.getAbsolutePath()));
        }
        return new Git(frb.build());
    }
}

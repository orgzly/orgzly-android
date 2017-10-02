package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import com.orgzly.android.git.GitPreferencesFromRepoPrefs;
import com.orgzly.android.git.GitTransportSetter;
import com.orgzly.android.prefs.RepoPreferences;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.orgzly.BuildConfig;

public class RepoFactory {

    public static Repo getFromUri(Context context, String uriString) {
        return getFromUri(context, Uri.parse(uriString));
    }

    // TODO: Better throw exception, not return null?
    public static Repo getFromUri(Context context, Uri uri) {
        String uriString = uri.toString();
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
                        return GitRepo.buildFromUri(context, uri);
                    case DirectoryRepo.SCHEME:
                        return new DirectoryRepo(uriString, false);

                    case MockRepo.SCHEME:
                        return new MockRepo(context, uriString);
                    default:
                        return GitRepo.buildFromUri(context, uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

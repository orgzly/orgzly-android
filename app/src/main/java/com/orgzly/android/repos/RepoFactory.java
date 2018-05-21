package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

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
                        if (BuildConfig.IS_DROPBOX_ENABLED) {
                            if (uri.getAuthority() == null) { // There should be no authority
                                return new DropboxRepo(context, uri);
                            }
                        }

                    case GitRepo.SCHEME:
                        if (BuildConfig.IS_GIT_ENABLED) {
                            return GitRepo.buildFromUri(context, uri);
                        }

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

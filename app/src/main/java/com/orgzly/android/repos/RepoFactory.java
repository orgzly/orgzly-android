package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

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
                        /* There should be no authority. */
                        if (uri.getAuthority() != null) {
                            return null;
                        }

                        return new DropboxRepo(context, uri);

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
}

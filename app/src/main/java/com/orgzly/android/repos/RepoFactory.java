package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import com.orgzly.BuildConfig;
import com.orgzly.android.data.DbRepoBookRepository;

import javax.inject.Inject;

public class RepoFactory {

    private DbRepoBookRepository dbRepoBookRepository;

    @Inject
    public RepoFactory(DbRepoBookRepository dbRepoBookRepository) {
        this.dbRepoBookRepository = dbRepoBookRepository;
    }

    public SyncRepo getFromUri(Context context, Uri uri, Long repoId) {
        return getFromUri(context, uri.toString(), repoId);
    }

    // TODO: Better throw exception, not return null?
    public SyncRepo getFromUri(Context context, String uriString, Long repoId) {
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
                            return GitRepo.buildFromIdAndUri(context, repoId, uri);
                        }

                    case DirectoryRepo.SCHEME:
                        return new DirectoryRepo(uriString, false);

                    case MockRepo.SCHEME:
                        return new MockRepo(dbRepoBookRepository, uriString);

                    default:
                        return GitRepo.buildFromIdAndUri(context, repoId, uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

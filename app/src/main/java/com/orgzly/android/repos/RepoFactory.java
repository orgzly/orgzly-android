package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import com.orgzly.BuildConfig;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.data.DbRepoBookRepository;

import javax.inject.Inject;

public class RepoFactory {

    private DbRepoBookRepository dbRepoBookRepository;

    @Inject
    public RepoFactory(DbRepoBookRepository dbRepoBookRepository) {
        this.dbRepoBookRepository = dbRepoBookRepository;
    }

    public SyncRepo getFromUri(Context context, Uri uri, DataRepository repo) {
        return getFromUri(context, uri.toString(), repo);
    }

    // TODO: Better throw exception, not return null?
    // TODO: Can we inject the DataRepository instead? Like the DbRepoBookRepository
    public SyncRepo getFromUri(Context context, String uriString, DataRepository repo) {
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
                            return GitRepo.buildFromUri(context, uri, repo);
                        }

                    case DirectoryRepo.SCHEME:
                        return new DirectoryRepo(uriString, false);

                    case MockRepo.SCHEME:
                        return new MockRepo(dbRepoBookRepository, uriString);

                    case WebdavRepo.SCHEME:
                    case WebdavRepo.SSL_SCHEME:
                        return WebdavRepo.buildFromUri(context, uri, repo);
                    default:
                        // TODO: Should be guarded with Buildconfig.IS_GIT_ENABLED?
                        return GitRepo.buildFromUri(context, uri, repo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

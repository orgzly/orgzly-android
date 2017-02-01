package com.orgzly.android.repos;

import android.net.Uri;

/**
 * Versioned {@link Rook}.
 *
 * A specific version of {@link Rook} with revision and modification time.
 */
public class VersionedRook extends Rook {
    private String revision;
    private long mtime;

    public VersionedRook(Rook rook, String revision, long mtime) {
        this(rook.getRepoUri(), rook.getUri(), revision, mtime);
    }

    public VersionedRook(Uri repoUri, Uri uri, String revision, long mtime) {
        super(repoUri, uri);

        this.revision = revision;
        this.mtime = mtime;
    }

    public String getRevision() {
        return revision;
    }

    public long getMtime() {
        return mtime;
    }

    public String toString() {
        return uri.buildUpon()
                .appendQueryParameter("revision", revision)
                .appendQueryParameter("mtime", String.valueOf(mtime))
                .build().toString();
    }
}

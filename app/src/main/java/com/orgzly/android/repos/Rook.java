package com.orgzly.android.repos;

import android.net.Uri;


/**
 * Remote notebook.
 *
 * Defined by repository URI and URI of the notebook itself.
 *
 * Both are necessary -- for example, if notebook is file:/Downloads/org/notes.org
 * its repository could be either file:/Downloads or file:/Downloads/org
 */
public class Rook {
    protected long repoId;
    protected RepoType repoType;
    protected Uri repoUri;
    protected Uri uri;

    public Rook(long repoId, RepoType repoType, Uri repoUri, Uri uri) {
        this.repoId = repoId;
        this.repoType = repoType;
        this.repoUri = repoUri;
        this.uri = uri;
    }

    public long getRepoId() {
        return repoId;
    }

    public RepoType getRepoType() {
        return repoType;
    }

    public Uri getRepoUri() {
        return repoUri;
    }

    public Uri getUri() {
        return uri;
    }

    public String toString() {
        return uri.toString();
    }
}

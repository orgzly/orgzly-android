package com.orgzly.android.repos;

import android.net.Uri;

import com.orgzly.android.BookName;

/**
 * Remote notebook.
 *
 * Defined by repository URI and URI of the notebook itself.
 *
 * Both are necessary -- for example, if notebook is file:/Downloads/org/notes.org
 * its repository could be either file:/Downloads or file:/Downloads/org
 */
public class Rook {
    protected Uri repoUri;
    protected Uri uri;

    public Rook(Uri repoUri, Uri uri) {
        this.repoUri = repoUri;
        this.uri = uri;
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

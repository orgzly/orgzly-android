package com.orgzly.android.repos

import android.net.Uri

class VersionedRook(
        repoId: Long,
        repoType: RepoType,
        repoUri: Uri,
        uri: Uri,
        val revision: String,
        val mtime: Long
) : Rook(repoId, repoType, repoUri, uri) {

    constructor(rook: Rook, revision: String, mtime: Long) :
            this(rook.repoId, rook.repoType, rook.getRepoUri(), rook.getUri(), revision, mtime)

    override fun toString(): String {
        return uri.buildUpon()
                .appendQueryParameter("revision", revision)
                .appendQueryParameter("mtime", mtime.toString())
                .build().toString()
    }
}

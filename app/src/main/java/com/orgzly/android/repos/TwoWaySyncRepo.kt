package com.orgzly.android.repos

import android.net.Uri
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import java.io.IOException

interface TwoWaySyncRepo {
    @Throws(IOException::class)
    fun syncBook(uri: Uri, current: VersionedRook?, fromDB: File, branchStartPoint: RevCommit?): TwoWaySyncResult

    fun tryPushIfHeadDiffersFromRemote()
    
    fun getCurrentHead(): RevCommit
}

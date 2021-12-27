package com.orgzly.android.repos

import android.net.Uri
import java.io.File
import java.io.IOException

interface TwoWaySyncRepo {
    @Throws(IOException::class)
    fun syncBook(uri: Uri, current: VersionedRook?, fromDB: File): TwoWaySyncResult

    fun tryPushIfHeadDiffersFromRemote()
}
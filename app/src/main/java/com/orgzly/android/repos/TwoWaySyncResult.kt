package com.orgzly.android.repos

import java.io.File

// No loadFile means that there's nothing to sync back to
data class TwoWaySyncResult(val newRook: VersionedRook, val loadFile: File?)

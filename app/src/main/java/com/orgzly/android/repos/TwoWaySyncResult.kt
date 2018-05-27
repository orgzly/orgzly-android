package com.orgzly.android.repos

import java.io.File

data class TwoWaySyncResult(val newRook: VersionedRook, val loadFile: File)

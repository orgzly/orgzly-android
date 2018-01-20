package com.orgzly.android.filter

import android.net.Uri

interface FilterStore {
    fun importFilters(uri: Uri)

    fun exportFilters()
}
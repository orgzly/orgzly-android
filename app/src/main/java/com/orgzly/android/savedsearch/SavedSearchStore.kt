package com.orgzly.android.savedsearch

import android.net.Uri

interface SavedSearchStore {
    fun importSearches(uri: Uri)

    fun exportSearches()
}
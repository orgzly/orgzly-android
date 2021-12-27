package com.orgzly.android.savedsearch

import android.net.Uri

interface SavedSearchStore {
    fun importSearches(uri: Uri): Int

    fun exportSearches(uri: Uri?): Int
}
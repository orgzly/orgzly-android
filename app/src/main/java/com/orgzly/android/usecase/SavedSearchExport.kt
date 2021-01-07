package com.orgzly.android.usecase

import android.net.Uri
import com.orgzly.android.data.DataRepository

class SavedSearchExport(val uri: Uri? = null) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val count = dataRepository.exportSavedSearches(uri)

        return UseCaseResult(userData = count)
    }
}
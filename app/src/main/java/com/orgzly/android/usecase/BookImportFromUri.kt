package com.orgzly.android.usecase

import android.net.Uri
import com.orgzly.android.BookFormat
import com.orgzly.android.data.DataRepository

/**
 * Load notebook from URI saving it using specified name.
 */
class BookImportFromUri(val bookName: String, val format: BookFormat, val uri: Uri) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        // TODO: Go through ActionService?
        dataRepository.importBook(bookName, format, uri)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}
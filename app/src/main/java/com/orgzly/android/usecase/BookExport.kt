package com.orgzly.android.usecase

import com.orgzly.android.BookFormat
import com.orgzly.android.data.DataRepository

/**
 * Exports book. Link is not updated, book stays linked to the same remote book.
 */
class BookExport(val bookId: Long, val format: BookFormat) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val file = dataRepository.exportBook(bookId, BookFormat.ORG)

        return UseCaseResult(userData = file)
    }
}
package com.orgzly.android.usecase

import com.orgzly.android.BookFormat
import com.orgzly.android.data.DataRepository
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Exports book. Link is not updated, book stays linked to the same remote book.
 */
class BookExportToUri(val bookId: Long, val stream: OutputStream, val format: BookFormat) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val book = dataRepository.getBookOrThrow(bookId)

        OutputStreamWriter(stream).use { writer ->
            dataRepository.exportBook(book, writer)
        }

        return UseCaseResult()
    }
}
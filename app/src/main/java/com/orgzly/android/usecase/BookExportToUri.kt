package com.orgzly.android.usecase

import android.net.Uri
import com.orgzly.android.data.DataRepository
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Exports book. Link is not updated, book stays linked to the same remote book.
 */
abstract class BookExportToUri(val uri: Uri, val bookId: Long) : UseCase() {

    @Throws(IOException::class)
    abstract fun getStream(uri: Uri): OutputStream

    override fun run(dataRepository: DataRepository): UseCaseResult {
        val book = dataRepository.getBookOrThrow(bookId)

        getStream(uri).use { stream ->
            OutputStreamWriter(stream).use { writer ->
                dataRepository.exportBook(book, writer)
            }
        }

        return UseCaseResult(userData = uri)
    }
}
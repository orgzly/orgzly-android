package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookDelete(val bookId: Long, val deleteLinked: Boolean) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val book = dataRepository.getBookView(bookId) ?: throw NotFound()

        dataRepository.deleteBook(book, deleteLinked)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }

    class NotFound: Throwable()
}
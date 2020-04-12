package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookRemoveSync(val bookId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val book = dataRepository.getBookView(bookId) ?: throw NotFound()

        dataRepository.removeBookSync(book)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }

    class NotFound: Throwable()
}
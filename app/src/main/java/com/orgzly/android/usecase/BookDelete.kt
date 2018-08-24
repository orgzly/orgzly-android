package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookDelete(val bookId: Long, val deleteLinked: Boolean) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.deleteBook(bookId, deleteLinked)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED
        )
    }

    class NotFound: Throwable()
}
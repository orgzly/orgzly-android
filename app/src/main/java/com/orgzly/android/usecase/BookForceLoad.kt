package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

/**
 * Load book from repository.
 */
class BookForceLoad(val bookId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.forceLoadBook(bookId)

        return UseCaseResult(
                modifiesLocalData = true
        )
    }
}
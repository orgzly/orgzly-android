package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book

class BookCycleVisibility(val book: Book) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.cycleVisibility(book.id)

        return UseCaseResult()
    }
}
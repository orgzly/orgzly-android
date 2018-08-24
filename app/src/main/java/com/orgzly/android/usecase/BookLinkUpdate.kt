package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookLinkUpdate @JvmOverloads constructor(val bookId: Long, val link: String? = null) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setLink(bookId, link)

        return UseCaseResult()
    }
}
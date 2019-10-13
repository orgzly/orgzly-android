package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Repo

class BookLinkUpdate @JvmOverloads constructor(val bookId: Long, val link: Repo? = null) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setLink(bookId, link)

        return UseCaseResult()
    }
}
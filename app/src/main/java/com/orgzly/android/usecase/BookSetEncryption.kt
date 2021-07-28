package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class BookSetEncryption(val bookId: Long, val passphrase: String?) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val book = dataRepository.getBookView(bookId) ?: throw NotFound()

        dataRepository.setEncryptionPassphrase(book, passphrase)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED // todo ?more needed
        )
    }

    class NotFound: Throwable()
}
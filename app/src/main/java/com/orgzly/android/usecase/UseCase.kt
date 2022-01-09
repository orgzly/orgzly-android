package com.orgzly.android.usecase

import android.content.Intent
import com.orgzly.android.data.DataRepository

abstract class UseCase {
    abstract fun run(dataRepository: DataRepository): UseCaseResult

    open fun toAction(): String {
        throw NotImplementedError("No action")
    }

    companion object {
        val TAG: String = UseCase::class.java.name

        const val SYNC_NOT_REQUIRED = 0
        const val SYNC_DATA_MODIFIED = 1
        const val SYNC_NOTE_CREATED = 2
    }
}
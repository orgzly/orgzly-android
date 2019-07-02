package com.orgzly.android.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class NoteViewModelFactory(
        private val dataRepository: DataRepository,
        private val bookId: Long,
        private val isNew: Boolean
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NoteViewModel(dataRepository, bookId, isNew) as T
    }

    companion object {
        @JvmStatic
        fun getInstance(dataRepository: DataRepository, bookId: Long, isNew: Boolean): ViewModelProvider.Factory {
            return NoteViewModelFactory(dataRepository, bookId, isNew)
        }
    }
}
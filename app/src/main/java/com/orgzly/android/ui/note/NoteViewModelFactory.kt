package com.orgzly.android.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class NoteViewModelFactory(
        private val dataRepository: DataRepository,
        private val bookId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NoteViewModel(dataRepository, bookId) as T
    }

    companion object {
        @JvmStatic
        fun getInstance(dataRepository: DataRepository, bookId: Long): ViewModelProvider.Factory {
            return NoteViewModelFactory(dataRepository, bookId)
        }
    }
}
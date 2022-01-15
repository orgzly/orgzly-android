package com.orgzly.android.ui.notes.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class BookViewModelFactory(
        private val dataRepository: DataRepository,
        val bookId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BookViewModel(dataRepository, bookId) as T
    }

    companion object {
        fun forBook(dataRepository: DataRepository, bookId: Long): ViewModelProvider.Factory {
            return BookViewModelFactory(dataRepository, bookId)
        }
    }
}
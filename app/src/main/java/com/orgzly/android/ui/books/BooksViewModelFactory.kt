package com.orgzly.android.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class BooksViewModelFactory(private val dataRepository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BooksViewModel(dataRepository) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository): ViewModelProvider.Factory {
            return BooksViewModelFactory(dataRepository)
        }
    }
}
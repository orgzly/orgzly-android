package com.orgzly.android.ui.notes.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class QueryViewModelFactory(private val dataRepository: DataRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return QueryViewModel(dataRepository) as T
    }

    companion object {
        fun forQuery(dataRepository: DataRepository): ViewModelProvider.Factory {
            return QueryViewModelFactory(dataRepository)
        }
    }
}
package com.orgzly.android.ui.savedsearches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class SavedSearchesViewModelFactory(private val dataRepository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SavedSearchesViewModel(dataRepository) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository): ViewModelProvider.Factory {
            return SavedSearchesViewModelFactory(dataRepository)
        }
    }
}
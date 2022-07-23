package com.orgzly.android.ui.repos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class ReposViewModelFactory(private val dataRepository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReposViewModel(dataRepository) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository): ViewModelProvider.Factory {
            return ReposViewModelFactory(dataRepository)
        }
    }
}
package com.orgzly.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class MainActivityViewModelFactory(private val dataRepository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainActivityViewModel(dataRepository) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository): ViewModelProvider.Factory {
            return MainActivityViewModelFactory(dataRepository)
        }
    }
}
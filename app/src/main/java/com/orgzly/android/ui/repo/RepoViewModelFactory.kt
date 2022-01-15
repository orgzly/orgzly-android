package com.orgzly.android.ui.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class RepoViewModelFactory(
        private val dataRepository: DataRepository,
        private val id: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RepoViewModel(dataRepository, id) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository, id: Long): ViewModelProvider.Factory {
            return RepoViewModelFactory(dataRepository, id)
        }
    }
}
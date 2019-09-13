package com.orgzly.android.ui.repo.webdav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class WebdavRepoViewModelFactory(
        private val dataRepository: DataRepository,
        private val id: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WebdavRepoViewModel(dataRepository, id) as T
    }

    companion object {
        fun getInstance(dataRepository: DataRepository, id: Long): ViewModelProvider.Factory {
            return WebdavRepoViewModelFactory(dataRepository, id)
        }
    }
}
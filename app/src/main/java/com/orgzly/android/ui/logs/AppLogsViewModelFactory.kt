package com.orgzly.android.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.logs.AppLogsRepository

class AppLogsViewModelFactory(private val appLogsRepository: AppLogsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AppLogsViewModel(appLogsRepository) as T
    }

    companion object {
        fun getInstance(appLogsRepository: AppLogsRepository): ViewModelProvider.Factory {
            return AppLogsViewModelFactory(appLogsRepository)
        }
    }
}
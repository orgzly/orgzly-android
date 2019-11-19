package com.orgzly.android.ui.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TimestampDialogViewModelFactory(
        private val orgDateTime: String?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TimestampDialogViewModel(orgDateTime) as T
    }

    companion object {
        fun getInstance(orgDateTime: String?): ViewModelProvider.Factory {
            return TimestampDialogViewModelFactory(orgDateTime)
        }
    }
}
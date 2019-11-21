package com.orgzly.android.ui.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.ui.TimeType

class TimestampDialogViewModelFactory(
        private val timeType: TimeType,
        private val orgDateTime: String?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TimestampDialogViewModel(timeType, orgDateTime) as T
    }

    companion object {
        fun getInstance(timeType: TimeType, orgDateTime: String?): ViewModelProvider.Factory {
            return TimestampDialogViewModelFactory(timeType, orgDateTime)
        }
    }
}
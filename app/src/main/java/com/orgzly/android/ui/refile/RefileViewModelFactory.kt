package com.orgzly.android.ui.refile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository

class RefileViewModelFactory(
        private val dataRepository: DataRepository,
        private val noteIds: Set<Long>,
        private val count: Int) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RefileViewModel(dataRepository, noteIds, count) as T
    }

    companion object {
        fun forNotes(
                dataRepository: DataRepository,
                noteIds: Set<Long>,
                count: Int): ViewModelProvider.Factory {

            return RefileViewModelFactory(dataRepository, noteIds, count)
        }
    }
}
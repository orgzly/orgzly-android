package com.orgzly.android.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TableViewModelFactory(
        private val nvmf: NoteViewModelFactory,
        private val tableStartOffset: Int,
        private val tableEndOffset: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TableViewModel(nvmf.create(NoteViewModel::class.java), tableStartOffset, tableEndOffset) as T
    }

    companion object {
        @JvmStatic
        fun getInstance(
                nvmf: NoteViewModelFactory,
                tableStartOffset: Int,
                tableEndOffset: Int

        ): ViewModelProvider.Factory {
            return TableViewModelFactory(nvmf, tableStartOffset, tableEndOffset)
        }
    }
}
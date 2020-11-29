package com.orgzly.android.ui.note

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.Place

class NoteViewModelFactory(
        private val dataRepository: DataRepository,
        private val bookId: Long,
        private val noteId: Long,
        private val place: Place?,
        private val title: String?,
        private val content: String?,
        private val attachmentUri: Uri?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NoteViewModel(dataRepository, bookId, noteId, place, title, content, attachmentUri) as T
    }

    companion object {
        @JvmStatic
        fun getInstance(
                dataRepository: DataRepository,
                bookId: Long,
                noteId: Long,
                place: Place?,
                title: String?,
                content: String?,
                attachmentUri: Uri?
        ): ViewModelProvider.Factory {

            return NoteViewModelFactory(dataRepository, bookId, noteId, place, title, content, attachmentUri)
        }
    }
}
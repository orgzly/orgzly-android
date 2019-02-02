package com.orgzly.android.ui.notes.book

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.CommonViewModel

class BookViewModel(private val dataRepository: DataRepository, val bookId: Long) : CommonViewModel() {
    data class Data(val book: Book?, val notes: List<NoteView>?)

    enum class LoadState {
        IN_PROGRESS,
        DONE,
        NO_NOTES,
        BOOK_DOES_NOT_EXIST
    }

    val dataLoadState = MutableLiveData<LoadState>(LoadState.IN_PROGRESS)

    fun setLoadState(state: LoadState) {
        dataLoadState.value = state
    }

    val data = MediatorLiveData<Data>().apply {
        addSource(dataRepository.getBookLiveData(bookId)) {
            value = Data(it, value?.notes)
        }
        addSource(dataRepository.getVisibleNotesLiveData(bookId)) {
            value = Data(value?.book, it)
        }
    }
}
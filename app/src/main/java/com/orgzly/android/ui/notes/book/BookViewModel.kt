package com.orgzly.android.ui.notes.book

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.CommonViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class BookViewModel(private val dataRepository: DataRepository, val bookId: Long) : CommonViewModel() {
    data class Data(val book: Book?, val notes: List<NoteView>?)

    val data: MediatorLiveData<Data> = MediatorLiveData()

    init {
        data.addSource(dataRepository.getBookLiveData(bookId)) {
            data.value = Data(it, data.value?.notes)
        }

        data.addSource(dataRepository.getVisibleNotesLiveData(bookId)) {
            data.value = Data(data.value?.book, it)
        }
    }
}
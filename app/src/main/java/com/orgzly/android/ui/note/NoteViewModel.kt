package com.orgzly.android.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.usecase.BookScrollToNote
import com.orgzly.android.usecase.BookSparseTreeForNote
import com.orgzly.android.usecase.NoteDelete
import com.orgzly.android.usecase.UseCaseRunner

class NoteViewModel(
        private val dataRepository: DataRepository,
        private val bookId: Long
) : CommonViewModel() {

    val tags: LiveData<List<String>> by lazy {
        dataRepository.selectAllTagsLiveData()
    }

    data class NoteDetailsData(val book: BookView?, val note: NoteView?, val ancestors: List<Note>)

    val noteDetailsDataEvent: SingleLiveEvent<NoteDetailsData> = SingleLiveEvent()

    val noteDeletedEvent: SingleLiveEvent<Int> = SingleLiveEvent()

    val bookChangeRequestEvent: SingleLiveEvent<List<BookView>> = SingleLiveEvent()

    fun loadData(bookId: Long, noteId: Long, place: Place?) {
        App.EXECUTORS.diskIO().execute {
            val book = dataRepository.getBookView(bookId)
            val note = dataRepository.getNoteView(noteId)

            // If creating a new note under specific one include that note too
            val ancestors = if (place == Place.UNDER) {
                dataRepository.getNoteAndAncestors(noteId)
            } else {
                dataRepository.getNoteAncestors(noteId)
            }

            noteDetailsDataEvent.postValue(NoteDetailsData(book, note, ancestors))
        }
    }

    fun deleteNote(bookId: Long, noteId: Long) {
        App.EXECUTORS.diskIO().execute {
            val useCase = NoteDelete(bookId, setOf(noteId))
            catchAndPostError {
                val result = UseCaseRunner.run(useCase)
                noteDeletedEvent.postValue(result.userData as Int)
            }
        }
    }

    fun requestNoteBookChange() {
        App.EXECUTORS.diskIO().execute {
            bookChangeRequestEvent.postValue(dataRepository.getBooks())
        }
    }

    fun onBreadcrumbsBook(data: NoteDetailsData) {
        data.note?.note?.id?.let { noteId ->
            App.EXECUTORS.diskIO().execute {
                UseCaseRunner.run(BookSparseTreeForNote(noteId))
            }
        }
    }

    fun onBreadcrumbsNote(bookId: Long, note: Note) {
        when (AppPreferences.breadcrumbsTarget(App.getAppContext())) {
            "note_details" ->
                MainActivity.openSpecificNote(bookId, note.id)

            "book_and_sparse_tree" ->
                App.EXECUTORS.diskIO().execute {
                    UseCaseRunner.run(BookSparseTreeForNote(note.id))
                }

            "book_and_scroll" ->
                App.EXECUTORS.diskIO().execute {
                    UseCaseRunner.run(BookScrollToNote(note.id))
                }
        }
    }

    enum class ViewEditMode {
        VIEW,
        EDIT,
        EDIT_WITH_KEYBOARD
    }

    // Start in edit mode
    val viewEditMode = MutableLiveData<ViewEditMode>(ViewEditMode.EDIT)

    // Change view/edit mode
    fun toggleViewEditMode() {
        viewEditMode.value = when (viewEditMode.value) {
            ViewEditMode.VIEW -> ViewEditMode.EDIT_WITH_KEYBOARD
            ViewEditMode.EDIT -> ViewEditMode.VIEW
            ViewEditMode.EDIT_WITH_KEYBOARD -> ViewEditMode.VIEW
            null -> ViewEditMode.EDIT
        }
    }
}
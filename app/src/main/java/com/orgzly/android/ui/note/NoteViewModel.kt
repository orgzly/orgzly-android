package com.orgzly.android.ui.note

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.usecase.*
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgRange
import java.lang.Exception

class NoteViewModel(
        private val dataRepository: DataRepository,
        private val bookId: Long,
        private val isNew: Boolean
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

    val viewEditMode = MutableLiveData<ViewEditMode>(startMode())

    private fun startMode(): ViewEditMode {
        // Always start new notes in edit mode
        if (isNew) {
            return ViewEditMode.EDIT_WITH_KEYBOARD
        }

        return when (AppPreferences.noteDetailsOpeningMode(App.getAppContext())) {
            "last" ->
                return when (AppPreferences.noteDetailsLastMode(App.getAppContext())) {
                    "view" -> ViewEditMode.VIEW
                    "edit" -> ViewEditMode.EDIT
                    else -> ViewEditMode.EDIT
                }
            "view" -> ViewEditMode.VIEW
            "edit" -> ViewEditMode.EDIT
            else -> ViewEditMode.EDIT
        }
    }

    /**
     * Toggle view/edit mode.
     */
    fun toggleViewEditMode() {
        val context = App.getAppContext()

        viewEditMode.value  = when (viewEditMode.value) {
            ViewEditMode.VIEW -> ViewEditMode.EDIT_WITH_KEYBOARD
            ViewEditMode.EDIT -> ViewEditMode.VIEW
            ViewEditMode.EDIT_WITH_KEYBOARD -> ViewEditMode.VIEW
            null -> ViewEditMode.EDIT
        }

        // Only remember last mode when opening existing notes
        if (!isNew) {
            if (viewEditMode.value == ViewEditMode.VIEW) {
                AppPreferences.noteDetailsLastMode(context, "view")
            } else {
                AppPreferences.noteDetailsLastMode(context, "edit")
            }
        }
    }

    var notePayload: NotePayload? = null

    fun savePayloadToBundle(outState: Bundle) {
        notePayload?.let {
            outState.putParcelable("payload", it)
        }
    }

    fun restorePayloadFromBundle(savedInstanceState: Bundle) {
        notePayload = savedInstanceState.getParcelable("payload") as? NotePayload
    }

    fun updatePayload(
            title: String,
            content: String,
            state: String?,
            priority: String?,
            tags: List<String>,
            properties: OrgProperties) {

        notePayload = notePayload?.copy(
                title = title,
                content = content,
                state = state,
                priority = priority,
                tags = tags,
                properties = properties)
    }

    fun initPayloadForNewNote(title: String?, content: String?) {
        notePayload = NoteBuilder.newPayload(App.getAppContext(), title ?: "", content)
    }

    fun initPayloadForExistingNote(noteId: Long) {
        notePayload = dataRepository.getNotePayload(noteId)
    }

    fun updatePayloadState(state: String?) {
        notePayload?.let {
            notePayload = NoteBuilder.changeState(App.getAppContext(), it, state)
        }
    }

    fun updatePayloadScheduledTime(range: OrgRange?) {
        notePayload = notePayload?.copy(scheduled = range.toString())
    }

    fun updatePayloadDeadlineTime(range: OrgRange?) {
        notePayload = notePayload?.copy(deadline = range.toString())
    }

    fun updatePayloadClosedTime(range: OrgRange?) {
        notePayload = notePayload?.copy(closed = range.toString())
    }

    val noteCreatedEvent: SingleLiveEvent<Note> = SingleLiveEvent()
    val noteUpdatedEvent: SingleLiveEvent<Note> = SingleLiveEvent()

    fun createNote(payload: NotePayload, place: NotePlace) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(NoteCreate(payload, place))
                noteCreatedEvent.postValue(result.userData as Note)
            }
        }
    }

    fun updateNote(payload: NotePayload, id: Long) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(NoteUpdate(id, payload))
                noteUpdatedEvent.postValue(result.userData as Note)
            }
        }
    }
}
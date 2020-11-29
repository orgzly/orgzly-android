package com.orgzly.android.ui.note

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.mappers.OrgMapper
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
import com.orgzly.android.util.MiscUtils
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.parser.OrgParserWriter

class NoteViewModel(
        private val dataRepository: DataRepository,
        var bookId: Long,
        private var noteId: Long,
        private val place: Place?,
        private val title: String?,
        private val content: String?,
        private val attachmentUri: Uri?
) : CommonViewModel() {

    enum class ViewEditMode {
        VIEW,
        EDIT,
        EDIT_TITLE_WITH_KEYBOARD,
        EDIT_CONTENT_WITH_KEYBOARD
    }

    val viewEditMode = MutableLiveData(startMode())


    val bookView: MutableLiveData<BookView> = MutableLiveData()


    val tags: LiveData<List<String>> by lazy {
        dataRepository.selectAllTagsLiveData()
    }

    data class NoteDetailsData(val book: BookView?, val note: NoteView?, val ancestors: List<Note>)

    val noteDetailsDataEvent: SingleLiveEvent<NoteDetailsData> = SingleLiveEvent()

    val noteCreatedEvent: SingleLiveEvent<Note> = SingleLiveEvent()
    val noteUpdatedEvent: SingleLiveEvent<Note> = SingleLiveEvent()
    val noteDeletedEvent: SingleLiveEvent<Int> = SingleLiveEvent()

    val noteDeleteRequest: SingleLiveEvent<Int> = SingleLiveEvent()
    val bookChangeRequestEvent: SingleLiveEvent<List<BookView>> = SingleLiveEvent()

    var notePayload: NotePayload? = null

    private var originalHash: Long = 0L

    fun loadData() {
        App.EXECUTORS.diskIO().execute {
            val book = dataRepository.getBookView(bookId)

            val note = dataRepository.getNoteView(noteId)

            // If creating a new note under specific one include that note too
            val ancestors = if (place == Place.UNDER) {
                dataRepository.getNoteAndAncestors(noteId)
            } else {
                dataRepository.getNoteAncestors(noteId)
            }

            notePayload = if (isNew()) {
                NoteBuilder.newPayload(App.getAppContext(), title ?: "", content, attachmentUri)
            } else {
                dataRepository.getNotePayload(noteId)
            }

            // Calculate payload's hash once for the original note
            notePayload?.let { payload ->
                App.EXECUTORS.mainThread().execute {
                    if (originalHash == 0L) {
                        originalHash = notePayloadHash(payload)
                    }
                }
            }

            bookView.postValue(book)

            noteDetailsDataEvent.postValue(NoteDetailsData(book, note, ancestors))
        }
    }

    fun requestNoteDelete() {
        App.EXECUTORS.diskIO().execute {
            val count = dataRepository.getNotesAndSubtreesCount(setOf(noteId))
            noteDeleteRequest.postValue(count)
        }
    }

    fun deleteNote() {
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

    private fun startMode(): ViewEditMode {
        // Always start new notes in edit mode
        if (isNew()) {
            return ViewEditMode.EDIT_CONTENT_WITH_KEYBOARD
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
        val mode = when (viewEditMode.value) {
            ViewEditMode.VIEW ->
                ViewEditMode.EDIT

            ViewEditMode.EDIT,
            ViewEditMode.EDIT_TITLE_WITH_KEYBOARD,
            ViewEditMode.EDIT_CONTENT_WITH_KEYBOARD ->
                ViewEditMode.VIEW

            null ->
                ViewEditMode.EDIT
        }

        viewEditMode.postValue(mode)

        // Only remember last mode when opening existing notes
        if (!isNew()) {
            saveCurrentMode(mode)
        }
    }

    fun editTitle(saveMode: Boolean = true) {
        ViewEditMode.EDIT_TITLE_WITH_KEYBOARD.let { mode ->
            viewEditMode.postValue(mode)
            if (saveMode) {
                saveCurrentMode(mode)
            }
        }
    }

    fun editContent() {
        ViewEditMode.EDIT_CONTENT_WITH_KEYBOARD.let { mode ->
            viewEditMode.postValue(mode)
            saveCurrentMode(mode)
        }
    }

    fun isInEditMode(): Boolean {
        return viewEditMode.value != ViewEditMode.VIEW
    }

    private fun saveCurrentMode(mode: ViewEditMode) {
        val context = App.getAppContext()

        AppPreferences.noteDetailsLastMode(
                context, if (mode == ViewEditMode.VIEW) "view" else "edit")

    }

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

    fun updatePayloadState(state: String?) {
        notePayload?.let {
            notePayload = NoteBuilder.changeState(App.getAppContext(), it, state)
        }
    }

    fun updatePayloadScheduledTime(range: OrgRange?) {
        notePayload = notePayload?.copy(scheduled = range?.toString())
    }

    fun updatePayloadDeadlineTime(range: OrgRange?) {
        notePayload = notePayload?.copy(deadline = range?.toString())
    }

    fun updatePayloadClosedTime(range: OrgRange?) {
        notePayload = notePayload?.copy(closed = range?.toString())
    }

    private fun createNote(postSave: ((note: Note) -> Unit)?) {
        val notePlace = if (place != Place.UNSPECIFIED)
            NotePlace(bookId, noteId, place)
        else
            NotePlace(bookId)

        notePayload?.let { payload ->
            App.EXECUTORS.diskIO().execute {
                catchAndPostError {
                    val result = UseCaseRunner.run(NoteCreate(payload, notePlace))
                    val note = result.userData as Note

                    // Update note ID after creating note
                    noteId = note.id

                    if (postSave != null) {
                        postSave(note)
                    } else {
                        noteCreatedEvent.postValue(note)
                    }
                }
            }
        }
    }

    private fun updateNote(postSave: ((note: Note) -> Unit)?) {
        notePayload?.let { payload ->
            App.EXECUTORS.diskIO().execute {
                catchAndPostError {
                    val result = UseCaseRunner.run(NoteUpdate(noteId, payload))
                    val note = result.userData as Note

                    if (postSave != null) {
                        postSave(note)
                    } else {
                        noteUpdatedEvent.postValue(note)
                    }
                }
            }
        }
    }

    /**
     * Hash used to detect note modifications.
     * TODO: Avoid generating org
     */
    private fun notePayloadHash(payload: NotePayload): Long {
        val head = OrgMapper.toOrgHead(payload)

        val parserWriter = OrgParserWriter()
        val str = parserWriter.whiteSpacedHead(head, 1, false)

        return MiscUtils.sha1(str)
    }

    fun isNoteModified(): Boolean {
        val payload = notePayload

        return if (payload != null) {
            notePayloadHash(payload) != originalHash
        } else {
            false
        }
    }

    fun isNew(): Boolean {
        return place != null
    }

    fun setBook(b: BookView) {
        bookId = b.book.id
        bookView.value = b
    }

    fun followBookBreadcrumb() {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                UseCaseRunner.run(BookScrollToNote(noteId))
            }
        }
    }

    fun followNoteBreadcrumb(ancestor: Note) {
        when (AppPreferences.breadcrumbsTarget(App.getAppContext())) {
            "note_details" ->
                MainActivity.openSpecificNote(bookId, ancestor.id)

            "book_and_sparse_tree" ->
                App.EXECUTORS.diskIO().execute {
                    UseCaseRunner.run(BookSparseTreeForNote(ancestor.id))
                }

            "book_and_scroll" ->
                App.EXECUTORS.diskIO().execute {
                    UseCaseRunner.run(BookScrollToNote(ancestor.id))
                }

        }

    }

    fun saveNote(postSave: ((note: Note) -> Unit)? = null) {
        if (isBookSet() && isTitleValid()) {
            if (isNew()) {
                createNote(postSave)
            } else {
                updateNote(postSave)
            }
        }
    }

    private fun isTitleValid(): Boolean {
        return if (TextUtils.isEmpty(notePayload?.title)) {
            snackBarMessage.postValue(R.string.title_can_not_be_empty)
            false
        } else {
            true
        }
    }

    private fun isBookSet(): Boolean {
        return if (bookId == 0L) {
            snackBarMessage.postValue(R.string.note_book_not_set)
            false
        } else {
            true
        }
    }
}
package com.orgzly.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.dao.NoteDao
import com.orgzly.android.db.entity.*
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.*
import com.orgzly.android.util.LogUtils
import java.lang.IllegalStateException

class MainActivityViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    private val booksSubject: LiveData<List<BookView>>

    private val savedSearches: LiveData<List<SavedSearch>> by lazy {
        dataRepository.getSavedSearchesLiveData()
    }

    val openFileLinkRequestEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    val openNoteWithPropertyRequestEvent: SingleLiveEvent<Pair<UseCase, UseCaseResult>> = SingleLiveEvent()

    val openNoteRequestEvent: SingleLiveEvent<Note> = SingleLiveEvent()

    data class BookLinkOptions(
            val book: Book,
            val links: List<Repo>,
            val urls: Array<CharSequence>,
            val selected: Int)

    val setBookLinkRequestEvent: SingleLiveEvent<BookLinkOptions> = SingleLiveEvent()

    init {
        // Observe parameters, run query when they change
        booksSubject = Transformations.switchMap(booksParams) {
            dataRepository.getBooksLiveData()
        }
    }


    /* Triggers querying only if parameters changed. */
    fun refresh(sortOrder: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sortOrder)
        booksParams.value = sortOrder
    }

    fun books(): LiveData<List<BookView>> {
        return booksSubject
    }

    fun savedSearches(): LiveData<List<SavedSearch>> {
        return savedSearches
    }

    fun followLinkToFile(path: String) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(LinkFindTarget(path))
                openFileLinkRequestEvent.postValue(result)
            }
        }
    }

    fun followLinkToNoteWithProperty(name: String, value: String) {
        App.EXECUTORS.diskIO().execute {
            val useCase = NoteFindWithProperty(name, value)

            catchAndPostError {
                val result = UseCaseRunner.run(useCase)

                val noteIdBookId = result.userData as NoteDao.NoteIdBookId

                when (AppPreferences.linkTarget(App.getAppContext())) {
                    "note_details" ->
                        openNoteWithPropertyRequestEvent.postValue(Pair(useCase, result))

                    "book_and_sparse_tree" ->
                        UseCaseRunner.run(BookSparseTreeForNote(noteIdBookId.noteId))

                    "book_and_scroll" ->
                        UseCaseRunner.run(BookScrollToNote(noteIdBookId.noteId))
                }
            }
        }
    }

    fun openNote(noteId: Long) {
        App.EXECUTORS.diskIO().execute {
            dataRepository.getNote(noteId)?.let { note ->
                openNoteRequestEvent.postValue(note)
            }
        }
    }

    fun setBookLink(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            val bookView = dataRepository.getBookView(bookId)

            if (bookView == null) {
                errorEvent.postValue(IllegalStateException("Book not found"))

            } else {
                val repos = dataRepository.getRepos()

                val options = if (repos.isEmpty()) {
                    BookLinkOptions(bookView.book, emptyList(), emptyArray(), -1)

                } else {
                    val currentLink = bookView.linkRepo

                    val selectedLink = repos.indexOfFirst {
                        it.url == currentLink?.url
                    }

                    BookLinkOptions(
                            bookView.book,
                            repos,
                            repos.map { it.url }.toTypedArray(),
                            selectedLink)
                }

                setBookLinkRequestEvent.postValue(options)
            }
        }
    }

    companion object {
        private val TAG = MainActivityViewModel::class.java.name
    }
}
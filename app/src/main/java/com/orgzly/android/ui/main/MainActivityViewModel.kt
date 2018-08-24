package com.orgzly.android.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.*
import com.orgzly.android.util.LogUtils

class MainActivityViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    private val booksSubject: LiveData<List<BookView>>

    private val squeries: LiveData<List<SavedSearch>> by lazy {
        dataRepository.getSavedSearchesLiveData()
    }

    val openFileLinkRequestEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    val openNoteWithPropertyRequestEvent: SingleLiveEvent<Pair<UseCase, UseCaseResult>> = SingleLiveEvent()

    val openNoteRequestEvent: SingleLiveEvent<Note> = SingleLiveEvent()

    data class BookLinkOptions(val book: Book, val links: List<CharSequence>, val selected: Int)

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

    fun squeries(): LiveData<List<SavedSearch>> {
        return squeries
    }

    fun openFileLink(path: String) {
        App.EXECUTORS.diskIO().execute {
            val action = LinkFindTarget(path)
            val result = UseCaseRunner.run(action)
            openFileLinkRequestEvent.postValue(result)
        }
    }


    fun requestNoteWithProperty(name: String, value: String) {
        App.EXECUTORS.diskIO().execute {
            val action = NoteFindWithProperty(name, value)
            val result = UseCaseRunner.run(action)
            openNoteWithPropertyRequestEvent.postValue(Pair(action, result))
        }
    }

    fun openNote(noteId: Long) {
        App.EXECUTORS.diskIO().execute {
            openNoteRequestEvent.postValue(dataRepository.getNote(noteId))
        }
    }

    fun setBookLink(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            val bookView = dataRepository.getBookView(bookId)

            if (bookView == null) {
                errorEvent.postValue(Exception("no book"))
            } else {
                val repos = dataRepository.getReposList()

                val a = if (repos.isEmpty()) {
                    BookLinkOptions(bookView.book, emptyList(), -1)

                } else {
                    val currentLink = bookView.linkedTo

                    var selectedLink = -1
                    val links = repos.mapIndexed { index, repo ->
                        if (repo.url == currentLink) {
                            selectedLink = index
                        }
                        repo.url
                    }

                    BookLinkOptions(bookView.book, links, selectedLink)
                }

                setBookLinkRequestEvent.postValue(a)
            }
        }
    }

    companion object {
        private val TAG = MainActivityViewModel::class.java.name
    }
}
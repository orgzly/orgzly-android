package com.orgzly.android.ui.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.dao.NoteDao
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.*
import com.orgzly.android.util.LogUtils
import java.io.File

class MainActivityViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    private val booksSubject: LiveData<List<BookView>> = Transformations.switchMap(booksParams) {
        dataRepository.getBooksLiveData()
    }

    private val savedSearches: LiveData<List<SavedSearch>> by lazy {
        dataRepository.getSavedSearchesLiveData()
    }

    val savedSearchedExportEvent: SingleLiveEvent<Int> = SingleLiveEvent()
    val savedSearchedImportEvent: SingleLiveEvent<Int> = SingleLiveEvent()

    val navigationActions: SingleLiveEvent<MainNavigationAction> = SingleLiveEvent()

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
                val result = UseCaseRunner.run(LinkFindTarget(path)).userData

                if (result is File) {
                    navigationActions.postValue(MainNavigationAction.OpenFile(result))

                } else if (result is Book) {
                    navigationActions.postValue(MainNavigationAction.OpenBook(result.id))
                }
            }
        }
    }

    fun displayQuery(query: String) {
        navigationActions.postValue(MainNavigationAction.DisplayQuery(query))
    }

    fun followLinkToNoteWithProperty(name: String, value: String) {
        App.EXECUTORS.diskIO().execute {
            val useCase = NoteFindWithProperty(name, value)

            catchAndPostError {
                val result = UseCaseRunner.run(useCase)

                if (result.userData == null) {
                    val msg = App.getAppContext().getString(R.string.no_such_link_target, name, value)
                    errorEvent.postValue(Throwable(msg))

                } else {
                    val noteIdBookId = result.userData as NoteDao.NoteIdBookId

                    when (AppPreferences.linkTarget(App.getAppContext())) {
                        "note_details" ->
                            navigationActions.postValue(
                                MainNavigationAction.OpenNote(noteIdBookId.bookId, noteIdBookId.noteId))

                        "book_and_sparse_tree" ->
                            UseCaseRunner.run(BookSparseTreeForNote(noteIdBookId.noteId))

                        "book_and_scroll" ->
                            UseCaseRunner.run(BookScrollToNote(noteIdBookId.noteId))
                    }
                }
            }
        }
    }

    fun openNote(noteId: Long) {
        App.EXECUTORS.diskIO().execute {
            dataRepository.getNote(noteId)?.let { note ->
                navigationActions.postValue(
                    MainNavigationAction.OpenNote(note.position.bookId, note.id))
            }
        }
    }

    fun exportSavedSearches(uri: Uri) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(SavedSearchExport(uri))
                savedSearchedExportEvent.postValue(result.userData as Int)
            }
        }
    }

    fun importSavedSearches(uri: Uri) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(SavedSearchImport(uri))
                savedSearchedImportEvent.postValue(result.userData as Int)
            }
        }
    }

    fun clockingUpdateRequest(noteIds: Set<Long>, type: Int) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                UseCaseRunner.run(NoteUpdateClockingState(noteIds, type))
            }
        }
    }

    companion object {
        private val TAG = MainActivityViewModel::class.java.name
    }
}

sealed class MainNavigationAction {
    data class OpenBook(val bookId: Long) : MainNavigationAction()
    data class OpenBookFocusNote(val bookId: Long, val noteId: Long, val foldRest: Boolean) : MainNavigationAction()
    data class OpenNote(val bookId: Long, val noteId: Long) : MainNavigationAction()
    data class OpenFile(val file: File) : MainNavigationAction()
    data class DisplayQuery(val query: String) : MainNavigationAction()
}
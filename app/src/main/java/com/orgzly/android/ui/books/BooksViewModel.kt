package com.orgzly.android.ui.books

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.BookDelete
import com.orgzly.android.usecase.BookRename
import com.orgzly.android.usecase.UseCaseResult
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.LogUtils


class BooksViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    private val booksSubject: LiveData<List<BookView>>

    val bookDeleteRequestEvent: SingleLiveEvent<BookView> = SingleLiveEvent()

    val bookDeletedEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    val bookRenameRequestEvent: SingleLiveEvent<BookView> = SingleLiveEvent()

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

    fun deleteBookRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookDeleteRequestEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun deleteBook(bookId: Long, deleteLinked: Boolean) {
        App.EXECUTORS.diskIO().execute {
            val action = BookDelete(bookId, deleteLinked)
            bookDeletedEvent.postValue(UseCaseRunner.run(action))
        }
    }

    fun renameBookRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookRenameRequestEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun renameBook(book: BookView, name: String) {
        App.EXECUTORS.diskIO().execute {
            val action = BookRename(book, name)
            UseCaseRunner.run(action)
        }
    }

    companion object {
        private val TAG = BooksViewModel::class.java.name
    }
}
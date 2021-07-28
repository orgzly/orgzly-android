package com.orgzly.android.ui.books

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.BookFormat
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.*
import com.orgzly.android.util.LogUtils


class BooksViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    val bookDeleteRequestEvent: SingleLiveEvent<BookView> = SingleLiveEvent()

    val bookDeletedEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    val bookRemoveSyncEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    val bookRenameRequestEvent: SingleLiveEvent<BookView> = SingleLiveEvent()

    val bookExportRequestEvent: SingleLiveEvent<Pair<Book, BookFormat>> = SingleLiveEvent()

    val bookEncryptionRequestEvent: SingleLiveEvent<BookView> = SingleLiveEvent()

    val bookEncryptionSetEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    enum class ViewState {
        LOADING,
        LOADED,
        EMPTY
    }

    val viewState = MutableLiveData<ViewState>(ViewState.LOADING)

    val books = Transformations.switchMap(booksParams) {
        Transformations.map(dataRepository.getBooksLiveData()) { books ->
            viewState.value = if (books.isNotEmpty()) {
                ViewState.LOADED
            } else {
                ViewState.EMPTY
            }
            books
        }
    }

    /* Triggers querying only if parameters changed. */
    fun refresh(sortOrder: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sortOrder)

        if (booksParams.value != sortOrder) {
            booksParams.value = sortOrder
        }
    }

    fun deleteBookRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookDeleteRequestEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun deleteBook(bookId: Long, deleteLinked: Boolean, deleteLocal: Boolean) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(BookDelete(bookId, deleteLinked, deleteLocal))
                bookDeletedEvent.postValue(result)
            }
        }
    }

    fun removeBookSync(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(BookRemoveSync(bookId))
                bookRemoveSyncEvent.postValue(result)
            }
        }
    }

    fun bookEncryptionRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookEncryptionRequestEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun bookEncryption(bookId: Long, passphrase: String?) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(BookSetEncryption(bookId, passphrase))
                bookEncryptionSetEvent.postValue(result) // todo ???
            }
        }
    }

    fun renameBookRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookRenameRequestEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    // todo add encryptionevent here

    fun renameBook(book: BookView, name: String) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                UseCaseRunner.run(BookRename(book, name))
            }
        }
    }

    fun exportBookRequest(bookId: Long, format: BookFormat) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val book = dataRepository.getBookOrThrow(bookId)
                bookExportRequestEvent.postValue(book to format)
            }
        }
    }

    companion object {
        private val TAG = BooksViewModel::class.java.name
    }
}
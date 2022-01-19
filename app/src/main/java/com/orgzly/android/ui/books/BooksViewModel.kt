package com.orgzly.android.ui.books

import android.content.Context
import android.net.Uri
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
import java.io.File
import java.io.OutputStream
import androidx.documentfile.provider.DocumentFile
import javax.inject.Inject


class BooksViewModel(private val dataRepository: DataRepository) : CommonViewModel() {
    private val booksParams = MutableLiveData<String>()

    // Book being operated on (deleted, renamed, etc.)
    private var lastBook = MutableLiveData<Pair<Book, BookFormat>>()

    val bookToDeleteEvent: SingleLiveEvent<BookView> = SingleLiveEvent()
    val bookDeletedEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()
    val bookToRenameEvent: SingleLiveEvent<BookView> = SingleLiveEvent()
    val bookToExportEvent: SingleLiveEvent<Pair<Book, BookFormat>> = SingleLiveEvent()
    val bookExportedEvent: SingleLiveEvent<String> = SingleLiveEvent()

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
            bookToDeleteEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun deleteBook(bookId: Long, deleteLinked: Boolean) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(BookDelete(bookId, deleteLinked))
                bookDeletedEvent.postValue(result)
            }
        }
    }

    fun renameBookRequest(bookId: Long) {
        App.EXECUTORS.diskIO().execute {
            bookToRenameEvent.postValue(dataRepository.getBookView(bookId))
        }
    }

    fun renameBook(book: BookView, name: String) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                UseCaseRunner.run(BookRename(book, name))
            }
        }
    }

    // User requested notebook export
    fun exportBookRequest(bookId: Long, format: BookFormat) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val book = dataRepository.getBookOrThrow(bookId)
                lastBook.postValue(Pair(book, format))
                bookToExportEvent.postValue(Pair(book, format))
            }
        }
    }

    fun exportBook() {
        val (book, format) = lastBook.value ?: return

        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val result = UseCaseRunner.run(BookExport(book.id, format))
                val file = result.userData as File
                bookExportedEvent.postValue(file.absolutePath)
            }
        }
    }

    fun exportBook(uri: Uri) {
        val (book, format) = lastBook.value ?: return

        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                App.getAppContext().contentResolver.openOutputStream(uri).let { stream ->
                    if (stream != null) {
                        UseCaseRunner.run(BookExportToUri(book.id, stream, format))
                        bookExportedEvent.postValue(uri.toString())
                    } else {
                        errorEvent.postValue(Throwable("Failed to open output stream"))
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = BooksViewModel::class.java.name
    }
}
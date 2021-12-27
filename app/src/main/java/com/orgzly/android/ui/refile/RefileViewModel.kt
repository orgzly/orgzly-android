package com.orgzly.android.ui.refile

import androidx.lifecycle.MutableLiveData
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.BookScrollToNote
import com.orgzly.android.usecase.NoteRefile
import com.orgzly.android.usecase.UseCaseResult
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.LogUtils
import java.util.*

class RefileViewModel(
        val dataRepository: DataRepository,
        val noteIds: Set<Long>,
        val count: Int) : CommonViewModel() {

    class Home
    class Parent

    data class Item(val payload: Any? = null, val name: String? = null)

    private val breadcrumbs = Stack<Item>()

    val data = MutableLiveData<Pair<Stack<Item>, List<Item>>>()

    val refiledEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    fun openForTheFirstTime() {
        val location = AppPreferences.refileLastLocation(App.getAppContext()).let {
            RefileLocation.fromJson(it)
        }

        val item = if (location?.type != null) {
            replayUntilNoteId(location)
        } else {
            HOME
        }

        open(item)
    }

    fun openParent() {
        open(PARENT)
    }

    fun open(item: Item) {
        val payload = item.payload

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, payload)

        when (payload) {
            is Parent -> {
                breadcrumbs.pop()

                open(breadcrumbs.pop())
            }

            is Home -> {
                App.EXECUTORS.diskIO().execute {
                    val items = dataRepository.getBooks().map { book ->
                        Item(book.book, book.book.name)
                    }

                    breadcrumbs.clear()
                    breadcrumbs.push(HOME)

                    saveLastLocation(payload)

                    data.postValue(Pair(breadcrumbs, items))
                }
            }

            is Book -> {
                App.EXECUTORS.diskIO().execute {
                    val items = dataRepository.getTopLevelNotes(payload.id).map { note ->
                        Item(note, note.title)
                    }

                    breadcrumbs.push(item)

                    saveLastLocation(payload)

                    data.postValue(Pair(breadcrumbs, items))
                }
            }

            is Note -> {
                App.EXECUTORS.diskIO().execute {
                    val items = dataRepository.getNoteChildren(payload.id).map { note ->
                        Item(note, note.title)
                    }

                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Items for $payload: $items")

                    if (items.isNotEmpty()) {
                        breadcrumbs.push(item)

                        saveLastLocation(payload)

                        data.postValue(Pair(breadcrumbs, items))
                    }
                }
            }
        }
    }

    fun refileHere() {
        val item = breadcrumbs.peek()

        refile(item)
    }

    fun refile(item: Item) {
        val payload = item.payload

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, payload)

        when (payload) {
            is Book -> {
                refile(NotePlace(payload.id))
            }

            is Note -> {
                refile(NotePlace(payload.position.bookId, payload.id, Place.UNDER))
            }
        }
    }

    fun refile(notePlace: NotePlace) {
        App.EXECUTORS.diskIO().execute {
            catchAndPostError {
                val useCase = NoteRefile(noteIds, notePlace)

                val result = UseCaseRunner.run(useCase)

                refiledEvent.postValue(result)
            }
        }
    }

    private fun saveLastLocation(payload: Any) {
        val lastLocation = when (payload) {
            is Home -> {
                RefileLocation.forHome().toJson()
            }

            is Book -> {
                RefileLocation.forBook(payload.id, payload.name).toJson()
            }

            is Note -> {
                RefileLocation.forNote(payload.id, payload.title).toJson()
            }

            else -> {
                throw IllegalStateException("Unsupported payload")
            }
        }

        AppPreferences.refileLastLocation(App.getAppContext(), lastLocation)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Refile last location saved: $lastLocation")
    }

    private fun replayUntilNoteId(location: RefileLocation): Item {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, location)

        when (location.type) {
            RefileLocation.Type.BOOK -> {
                // Try finding book by its ID first
                if (location.id != null) {
                    dataRepository.getBookView(location.id)?.apply {
                        breadcrumbs.clear()
                        breadcrumbs.add(HOME)
                        return Item(book, book.name)
                    }
                }

                // Try finding book by its name
                if (location.title != null) {
                    dataRepository.getBook(location.title)?.apply {
                        breadcrumbs.clear()
                        breadcrumbs.add(HOME)
                        return Item(this, name)
                    }
                }
            }

            RefileLocation.Type.NOTE -> {
                // Try finding note by its ID first
                if (location.id != null) {
                    replayUntilNoteId(location.id)?.let {
                        return it
                    }
                }

                // Try finding note by its title
                if (location.title != null) {
                    val notes = dataRepository.getNotesByTitle(location.title)

                    if (notes.size == 1) {
                        replayUntilNoteId(notes.first().id)?.let {
                            return it
                        }
                    }
                }
            }

            else -> {
                return HOME
            }
        }

        return HOME
    }

    private fun replayUntilNoteId(noteId: Long): Item? {
        val notes = dataRepository.getNoteAndAncestors(noteId)

        if (notes.isNotEmpty()) {
            val lastNote = notes.last()

            val book = dataRepository.getBook(lastNote.position.bookId)

            if (book != null) {
                breadcrumbs.clear()
                breadcrumbs.add(HOME)
                breadcrumbs.add(Item(book, book.name))

                for (i in 0 until notes.count() - 1) {
                    val note = notes[i]

                    val item = Item(note, note.title)

                    breadcrumbs.push(item)
                }


                return Item(lastNote, lastNote.title)
            }
        }

        return null
    }

    fun onBreadcrumbClick(item: Item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        while (breadcrumbs.pop() != item) {
            // Pop up to and including clicked item
        }

        open(item)
    }

    fun goTo(noteId: Long) {
        App.EXECUTORS.diskIO().execute {
            UseCaseRunner.run(BookScrollToNote(noteId))
        }
    }

    fun locationHasParent(): Boolean {
        return breadcrumbs.size > 1
    }

    companion object {
        val HOME = Item(Home())
        val PARENT = Item(Parent())

        private val TAG = RefileViewModel::class.java.name
    }
}
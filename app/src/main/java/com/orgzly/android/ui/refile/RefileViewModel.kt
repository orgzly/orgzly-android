package com.orgzly.android.ui.refile

import androidx.lifecycle.MutableLiveData
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.*
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
    class Up

    data class Item(val payload: Any? = null, val name: String? = null)

    private val breadcrumbs = Stack<Item>()

    data class Location(val breadcrumbs: Stack<Item>, val list: List<Item>)

    val location: MutableLiveData<Location> = MutableLiveData()

    val refiledEvent: SingleLiveEvent<UseCaseResult> = SingleLiveEvent()

    private val history = History()

    init {
        loadHistory()
    }

    fun openHistory() {
    }

    fun goUp() {
        open(UP)
    }

    fun open(item: Item) {
        val payload = item.payload

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, payload)

        when (payload) {
            is Up -> {
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

                    location.postValue(Location(breadcrumbs, items))
                }
            }

            is Book -> {
                App.EXECUTORS.diskIO().execute {
                    val items = dataRepository.getTopLevelNotes(payload.id).map { note ->
                        Item(note, note.title)
                    }

                    breadcrumbs.push(item)

                    location.postValue(Location(breadcrumbs, items))
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

                        location.postValue(Location(breadcrumbs, items))
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

                addToHistory()
            }
        }
    }

    class History : LinkedHashSet<List<String>>() {
        /** Move existing element to be the last one. */
        override fun add(element: List<String>): Boolean {
            if (contains(element)) {
                remove(element)
            }
            return super.add(element)
        }
    }

    data class LocationPath(val path: List<String>) {
        override fun toString(): String {
            return path.joinToString("\n")
        }

        companion object {
            fun fromString(str: String): LocationPath {
                return LocationPath(str.split("\n"))
            }

            fun fromBreadcrumbs(stack: Stack<Item>): LocationPath {
                return LocationPath(stack.mapNotNull { it.name })
            }
        }
    }

    private fun addToHistory() {
        val location = LocationPath.fromBreadcrumbs(breadcrumbs)

        val lastLocation = location.toString()

        AppPreferences.refileLastLocation(App.getAppContext(), lastLocation)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Refile last location saved: $lastLocation")
    }

    private fun loadHistory() {
        val lastLocation = AppPreferences.refileLastLocation(App.getAppContext())

        if (lastLocation != null) {
            val path = LocationPath.fromString(lastLocation)

            replayPath(path)
        }
    }

    private fun replayPath(location: LocationPath) {
        location.path.forEachIndexed { index, _ ->
            when (index) {
                0 -> { // Home
                }

                1 -> { // Notebook
                }

                else -> { // Note

                }
            }
        }
    }

    fun onBreadcrumbClick(item: Item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        while (breadcrumbs.pop() != item) {
            // Pop up to and including clicked item
        }

        open(item)
    }

    companion object {
        val HOME = Item(Home())
        val UP = Item(Up())

        private val TAG = RefileViewModel::class.java.name
    }
}
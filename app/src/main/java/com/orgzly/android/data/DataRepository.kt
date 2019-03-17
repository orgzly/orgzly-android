package com.orgzly.android.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaScannerConnection
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.*
import com.orgzly.android.data.mappers.OrgMapper
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.dao.NoteDao
import com.orgzly.android.db.dao.NoteViewDao
import com.orgzly.android.db.dao.ReminderTimeDao
import com.orgzly.android.db.entity.*
import com.orgzly.android.db.mappers.OrgTimestampMapper
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.Query
import com.orgzly.android.query.sql.SqliteQueryBuilder
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.Rook
import com.orgzly.android.repos.SyncRepo
import com.orgzly.android.repos.VersionedRook
import com.orgzly.android.savedsearch.FileSavedSearchStore
import com.orgzly.android.sync.BookSyncStatus
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NoteBuilder
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.android.usecase.BookDelete
import com.orgzly.android.usecase.RepoCreate
import com.orgzly.android.util.Encoding
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.android.util.OrgFormatter
import com.orgzly.org.OrgFile
import com.orgzly.org.OrgFileSettings
import com.orgzly.org.OrgActiveTimestamps
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.parser.*
import com.orgzly.org.utils.StateChangeLogic
import java.io.*
import java.util.*
import java.util.concurrent.Callable
import javax.inject.Inject

// TODO: Split
class DataRepository @Inject constructor(
        private val context: Context,
        private val db: OrgzlyDatabase,
        private val repoFactory: RepoFactory,
        private val resources: Resources,
        private val localStorage: LocalStorage) {

    fun forceLoadBook(bookId: Long) {
        val book = getBookView(bookId)
                ?: throw IOException(resources.getString(R.string.book_does_not_exist_anymore))

        try {
            if (book.linkedTo == null) {
                throw IOException(resources.getString(R.string.message_book_has_no_link))
            }

            setBookLastActionAndSyncStatus(bookId, BookAction.forNow(
                    BookAction.Type.PROGRESS,
                    resources.getString(R.string.force_loading_from_uri, book.linkedTo)))

            val fileName = BookName.getFileName(context, book)

            val loadedBook = loadBookFromRepo(Uri.parse(book.linkedTo), fileName)

            setBookLastActionAndSyncStatus(loadedBook!!.book.id, BookAction.forNow(
                    BookAction.Type.INFO,
                    resources.getString(R.string.force_loaded_from_uri, loadedBook.syncedTo?.uri)))

        } catch (e: Exception) {
            e.printStackTrace()

            val msg = resources.getString(R.string.force_loading_failed, e.localizedMessage)

            setBookLastActionAndSyncStatus(bookId, BookAction.forNow(BookAction.Type.ERROR, msg))

            throw IOException(msg)
        }
    }

    fun forceSaveBook(bookId: Long) {
        val book = getBookView(bookId)
                ?: throw IOException(resources.getString(R.string.book_does_not_exist_anymore))

        val fileName: String = BookName.getFileName(context, book)

        try {
            /* Prefer link. */
            val repoUrl = book.linkedTo ?: repoForSavingBook()

            setBookLastActionAndSyncStatus(book.book.id, BookAction.forNow(
                    BookAction.Type.PROGRESS,
                    resources.getString(R.string.force_saving_to_uri, repoUrl)))

            saveBookToRepo(repoUrl, fileName, book, BookFormat.ORG)

            val savedBook = getBookView(bookId)

            setBookLastActionAndSyncStatus(bookId, BookAction.forNow(
                    BookAction.Type.INFO,
                    resources.getString(R.string.force_saved_to_uri, savedBook?.syncedTo?.uri)))

        } catch (e: Exception) {
            e.printStackTrace()

            val msg = resources.getString(R.string.force_saving_failed, e.localizedMessage)

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating status for $bookId")

            setBookLastActionAndSyncStatus(bookId, BookAction.forNow(BookAction.Type.ERROR, msg))

            throw IOException(msg)
        }
    }

    /**
     * Exports `Book`, uploads it to repo and link it to newly created
     * [com.orgzly.android.repos.VersionedRook].
     *
     * @return [Book]
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveBookToRepo(repoUrl: String, fileName: String, bookView: BookView, format: BookFormat) {

        val uploadedBook: VersionedRook

        val repo = getRepo(Uri.parse(repoUrl))

        val tmpFile = getTempBookFile()
        try {
            /* Write to temporary file. */
            NotesOrgExporter(context, this).exportBook(bookView.book, tmpFile)

            /* Upload to repo. */
            uploadedBook = repo.storeBook(tmpFile, fileName)

        } finally {
            /* Delete temporary file. */
            tmpFile.delete()
        }

        updateBookLinkAndSync(bookView.book.id, uploadedBook)

        updateBookIsModified(bookView.book.id, false)

    }

    @Throws(IOException::class)
    fun getTempBookFile(): File {
        return localStorage.getTempBookFile()
    }

    @Throws(IOException::class)
    fun getRepo(repoUrl: Uri): SyncRepo {
        return repoFactory.getFromUri(context, repoUrl)
                ?: throw IOException("Unsupported repository URL \"$repoUrl\"")
    }

    /*
     * If there is only one repository, return its URL.
     * If there are more, we don't know which one to use, so throw exception.
     */
    @Throws(IOException::class)
    private fun repoForSavingBook(): String {
        val repos = getRepos()

        /* Use repository if there is only one. */

        return when {
            repos.size == 1 -> repos.keys.iterator().next()
            repos.isEmpty() -> throw IOException(resources.getString(R.string.no_repos))
            else -> throw IOException(resources.getString(R.string.multiple_repos))
        }
    }

    fun importBook(bookName: String, format: BookFormat, uri: Uri) {
        if (doesBookExist(bookName)) {
            throw FileNotFoundException(resources.getString(R.string.book_name_already_exists, bookName))
        }

        val book = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            loadBookFromStream(bookName, format, inputStream)
        } ?: throw IOException(resources.getString(R.string.failed_importing_book, bookName))

        val action = BookAction.forNow(BookAction.Type.INFO, resources.getString(R.string.imported))
        setBookLastActionAndSyncStatus(book.book.id, action)
    }

    fun getBooksLiveData(): LiveData<List<BookView>> {
        val order = AppPreferences.notebooksSortOrder(context)
        val mtime = resources.getString(R.string.pref_value_notebooks_sort_order_modification_time)

        return when (order) {
            mtime -> db.bookView().getAllOrderByTimeLiveData()
            else -> db.bookView().getAllFOrderByNameLiveData()
        }
    }

    fun getBooks(): List<BookView> {
        val order = AppPreferences.notebooksSortOrder(context)
        val mtime = resources.getString(R.string.pref_value_notebooks_sort_order_modification_time)

        return when (order) {
            mtime -> db.bookView().getAllOrderByTime()
            else -> db.bookView().getAllFOrderByName()
        }
    }

    fun getBookView(name: String): BookView? {
        return db.bookView().get(name)
    }

    fun getBookView(id: Long): BookView? {
        return db.bookView().get(id)
    }

    private fun doesBookExist(name: String): Boolean {
        return getBook(name) != null
    }

    fun getBook(name: String): Book? {
        return db.book().get(name)
    }

    fun getBook(id: Long): Book? {
        return db.book().get(id)
    }

    fun getBookLiveData(id: Long): LiveData<Book> {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)
        return db.book().getLiveData(id)
    }

    /**
     * Returns full string content of the book in format specified. Used by tests.
     */
    @Throws(IOException::class)
    fun getBookContent(name: String, format: BookFormat): String? {
        val book = getBook(name)

        if (book != null) {
            val file = getTempBookFile()
            try {
                NotesOrgExporter(context, this).exportBook(book, file)
                return MiscUtils.readStringFromFile(file)
            } finally {
                file.delete()
            }
        }

        return null
    }

    /**
     * Creates new dummy book - temporary incomplete book
     * (before remote book has been downloaded, or linked etc.)
     */
    @Throws(IOException::class)
    fun createDummyBook(name: String): BookView {
        if (doesBookExist(name)) {
            throw IOException("Can't insert notebook with the same name: $name")
        }

        val book = Book(0, name, isDummy = true)

        val bookId = db.book().insert(book)

        db.note().insert(NoteDao.rootNote(bookId))

        return BookView(Book(bookId, name, isDummy =  true), 0)
    }

    /**
     * @param dummy If true, creates a new dummy book - temporary incomplete book
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun createBook(name: String, dummy: Boolean = false): BookView {
        if (doesBookExist(name)) {
            throw IOException(resources.getString(R.string.book_name_already_exists, name))
        }

        val book = Book(
                0,
                name,
                mtime = System.currentTimeMillis(),
                lastAction = BookAction.forNow(BookAction.Type.INFO, resources.getString(R.string.created)),
                isDummy = dummy
        )

        val id = db.runInTransaction(Callable {
            val id = db.book().insert(book)

            db.note().insert(NoteDao.rootNote(id))

            return@Callable id
        })

        return BookView(book.copy(id = id), 0)
    }

    fun deleteBook(bookId: Long, deleteLinked: Boolean) {
        val book = getBookView(bookId) ?: throw BookDelete.NotFound()

        deleteBook(book, deleteLinked)
    }

    private fun deleteBook(book: BookView, deleteLinked: Boolean) {
        if (deleteLinked) {
            val repo = repoFactory.getFromUri(context, book.syncedTo?.repoUri)
            repo?.delete(book.syncedTo?.uri)
        }

        db.book().delete(book.book)
    }

    fun renameBook(bookView: BookView, name: String) {
        try {
            doRenameBook(bookView, name)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()

            val message = if (e.message != null) {
                resources.getString(R.string.failed_renaming_book_with_reason, e.localizedMessage)
            } else {
                resources.getString(R.string.failed_renaming_book)
            }

            setBookLastAction(
                    bookView.book.id, BookAction.forNow(BookAction.Type.ERROR, message))
        }
    }

    @Throws(IOException::class)
    private fun doRenameBook(bookView: BookView, name: String) {
        val book = bookView.book

        val oldName = book.name

        /* Make sure there is no notebook with this name. */
        if (doesBookExist(name)) {
            throw IOException(resources.getString(R.string.book_name_already_exists, name))
        }

        /* Make sure link's repo is the same as sync book repo. */
        if (bookView.hasLink() && bookView.syncedTo != null) {
            if (!TextUtils.equals(bookView.linkedTo, bookView.syncedTo.repoUri.toString())) {
                val s = BookSyncStatus.ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS.toString()
                setBookLastActionAndSyncStatus(book.id, BookAction.forNow(BookAction.Type.ERROR, s), s)
                return
            }
        }

        /* Do not rename if there are local changes. */
        if (bookView.isOutOfSync()) {
            throw IOException("Notebook is not synced")
        }

        /* Prefer link. */
        if (bookView.syncedTo != null) {
            val vrook = bookView.syncedTo
            val repo = repoFactory.getFromUri(context, vrook.repoUri)

            val movedVrook = repo.renameBook(vrook.uri, name)

            updateBookLinkAndSync(book.id, movedVrook)
        }

        if (db.book().updateName(book.id, name) != 1) {
            throw IOException()
        }

        setBookLastAction(book.id, BookAction.forNow(
                BookAction.Type.INFO,
                resources.getString(R.string.renamed_book_from, oldName)))
    }

    fun setBookPreface(bookId: Long, preface: String) {
        val settings = OrgFileSettings.fromPreface(preface)

        db.book().updatePreface(bookId, preface, settings.title)

        updateBookIsModified(bookId, true)
    }

    fun setBookLastAction(bookId: Long, action: BookAction) {
        db.book().updateLastAction(bookId, action.type, action.message, System.currentTimeMillis())
    }

    @JvmOverloads
    fun setBookLastActionAndSyncStatus(bookId: Long, action: BookAction, status: String? = null) {
        val updated = db.book().updateLastActionAndSyncStatus(
                bookId,
                action.type,
                action.message,
                System.currentTimeMillis(),
                status)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating status for $bookId: $updated")
    }

    fun updateBookLinkAndSync(bookId: Long, uploadedBook: VersionedRook) {
        val repoUrl = uploadedBook.repoUri.toString()
        val rookUrl = uploadedBook.uri.toString()
        val rookRevision = uploadedBook.revision
        val rookMtime = uploadedBook.mtime

        val repoId = db.repo().getOrInsert(repoUrl)
        val rookUrlId = db.rookUrl().getOrInsert(rookUrl)
        val rookId = db.rook().getOrInsert(repoId, rookUrlId)

        val versionedRookId = db.versionedRook().replace(
                VersionedRook(0, rookId, rookRevision, rookMtime))

        db.bookLink().upsert(bookId, repoId)
        db.bookSync().upsert(bookId, versionedRookId)
    }

    private fun updateBookIsModified(bookId: Long, isModified: Boolean, time: Long = System.currentTimeMillis()) {
        updateBookIsModified(listOf(bookId), isModified, time)
    }

    private fun updateBookIsModified(bookIds: List<Long>, isModified: Boolean, time: Long = System.currentTimeMillis()) {
        if (bookIds.isNotEmpty()) {
            if (isModified) {
                db.book().setIsModified(bookIds, time)
            } else {
                db.book().setIsNotModified(bookIds)
            }
        }
    }

    /**
     * Returns default book if it exists, or first one found.
     * If there are no books, default book will be created.
     */
    @Throws(IOException::class)
    fun getTargetBook(context: Context): BookView {
        val books = getBooks()
        val defaultBookName = AppPreferences.shareNotebook(context)

        if (books.isEmpty()) {
            return createBook(defaultBookName)
        } else {
            for (book in books) {
                if (defaultBookName == book.book.name) {
                    return book
                }
            }
            return books.first()
        }
    }

    fun getRootNode(bookId: Long): Note? {
        return db.note().getRootNode(bookId)
    }

    fun setLink(bookId: Long, repoUrl: String?) {
        if (repoUrl == null) {
            deleteBookLink(bookId)
        } else {
            setBookLink(bookId, repoUrl)
        }
    }

    private fun setBookLink(bookId: Long, repoUrl: String) {
        val repoId = db.repo().getOrInsert(repoUrl)
        db.bookLink().upsert(bookId, repoId)
    }

    private fun deleteBookLink(bookId: Long) {
        db.bookLink().deleteByBookId(bookId)
    }

    fun cycleVisibility(bookId: Long): Int {
        if (unfoldedNotesExist(bookId)) {
            foldAllNotes(bookId)
        } else {
            unFoldAllNotes(bookId)
        }
        return 0
    }

    private fun unfoldedNotesExist(bookId: Long): Boolean {
        return db.note().getBookUnfoldedNoteCount(bookId) > 0
    }

    private fun unFoldAllNotes(bookId: Long) {
        db.note().unfoldAll(bookId)
    }

    private fun foldAllNotes(bookId: Long) {
        db.note().foldAll(bookId)
    }

    private fun unfoldForNote(noteId: Long) {
        val ancestorsIds = db.note().getAncestors(listOf(noteId))

        if (ancestorsIds.isNotEmpty()) {
            db.note().unfoldNotes(ancestorsIds)
            db.note().updateFoldedUnderForNoteFoldedUnderId(ancestorsIds)
        }
    }

    fun promoteNote(bookId: Long, noteId: Long): Int {
        return promoteNotes(bookId, setOf(noteId))
    }

    fun promoteNotes(bookId: Long, ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            val note = db.note().getFirst(ids) ?: return@Callable 0

            /* Can only promote notes of level 2 or greater. */
            if (note.position.level <= 1 || note.position.parentId <= 0) {
                return@Callable 0
            }

            val clipboard = NotesClipboard.create(this, ids)

            // Paste just under parent if note's level is too high, below otherwise
            val parent = db.note().get(note.position.parentId) ?: return@Callable 0
            val pasted = if (parent.position.level + 1 < note.position.level) {
                pasteNotesClipboard(clipboard, Place.UNDER_AS_FIRST, note.position.parentId)
            } else {
                pasteNotesClipboard(clipboard, Place.BELOW, note.position.parentId)
            }

            deleteNotes(bookId, ids)

            return@Callable pasted.size
        })
    }

    fun demoteNote(bookId: Long, noteId: Long): Int {
        return demoteNotes(bookId, setOf(noteId))
    }

    fun demoteNotes(bookId: Long, ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            val note = db.note().getFirst(ids) ?: return@Callable 0

            val previousSibling =
                    db.note().getPreviousSibling(note.position.bookId, note.position.lft, note.position.parentId)
                            ?: return@Callable 0

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Previous sibling ${previousSibling.title}")

            val clipboard = NotesClipboard.create(this, ids)

            val pasted = pasteNotesClipboard(clipboard, Place.UNDER, previousSibling.id)

            deleteNotes(bookId, ids)

            return@Callable pasted.size
        })
    }

    fun moveNote(bookId: Long, noteIds: Set<Long>, direction: Int): Int {
        return db.runInTransaction(Callable {
            val target: NotePlace? =
                    if (direction == -1) { // Move up
                        db.note().getFirst(noteIds)?.let { note ->
                            db.note().getPreviousSibling(bookId, note.position.lft, note.position.parentId)?.let { sibling ->
                                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Place above ${sibling.title}")
                                NotePlace(bookId, sibling.id, Place.ABOVE)
                            }
                        }
                    } else { // Move down
                        db.note().getLast(noteIds)?.let { note ->
                            db.note().getNextSibling(bookId, note.position.rgt, note.position.parentId)?.let { sibling ->
                                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Place below ${sibling.title}")
                                NotePlace(bookId, sibling.id, Place.BELOW)
                            }
                        }
                    }

            target?.let {
                val clipboard = NotesClipboard.create(this, noteIds)

                val pasted = pasteNotesClipboard(clipboard, it.place, it.noteId)

                deleteNotes(bookId, noteIds)
            }

            return@Callable 0
        })
    }

    fun refileNotes(bookId: Long, noteIds: Set<Long>, targetBookId: Long): Int {
        val root = getRootNode(targetBookId) ?: return 0

        return db.runInTransaction(Callable {
            val clipboard = NotesClipboard.create(this, noteIds)

            pasteNotesClipboard(clipboard, Place.UNDER, root.id)

            deleteNotes(bookId, noteIds)
        })
    }

    fun pasteNotes(clipboard: NotesClipboard, noteId: Long, place: Place): Int {
        return db.runInTransaction(Callable {
            pasteNotesClipboard(clipboard, place, noteId).size
        })
    }

    private fun pasteNotesClipboard(clipboard: NotesClipboard, place: Place, targetNoteId: Long): Set<Long> {
        val pastedNoteIds = HashSet<Long>()

        val targetNote = db.note().get(targetNoteId) ?: return pastedNoteIds

        val bookId = targetNote.position.bookId

        val pastedLft: Long
        val pastedLevel: Int
        val pastedParentId: Long

        /* If target note is hidden, hide pasted under the same note. */
        var foldedUnder: Long = 0
        if (targetNote.position.foldedUnderId != 0L) {
            foldedUnder = targetNote.position.foldedUnderId
        }

        when (place) {
            Place.ABOVE -> {
                pastedLft = targetNote.position.lft
                pastedLevel = targetNote.position.level
                pastedParentId = targetNote.position.parentId
            }

            Place.UNDER -> {
                val lastDescendant = db.note().getLastHighestLevelDescendant(
                        targetNote.position.bookId, targetNote.position.lft, targetNote.position.rgt)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "lastDescendant: $lastDescendant")


                if (lastDescendant != null) {
                    /* Insert batch after last descendant with highest level. */
                    pastedLft = lastDescendant.position.rgt + 1
                    pastedLevel = lastDescendant.position.level

                } else {
                    /* Insert batch just under the target note. */
                    pastedLft = targetNote.position.lft + 1
                    pastedLevel = targetNote.position.level + 1
                }

                if (targetNote.position.isFolded) {
                    foldedUnder = targetNote.id
                }

                pastedParentId = targetNote.id
            }

            Place.UNDER_AS_FIRST -> {
                pastedLft = targetNote.position.lft + 1
                pastedLevel = targetNote.position.level + 1

                if (targetNote.position.isFolded) {
                    foldedUnder = targetNote.id
                }

                pastedParentId = targetNote.id
            }

            Place.BELOW -> {
                pastedLft = targetNote.position.rgt + 1
                pastedLevel = targetNote.position.level
                pastedParentId = targetNote.position.parentId
            }

            else -> throw IllegalArgumentException("Unsupported place for paste: $place")
        }

        val levelOffset = pastedLevel - 1

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, """
                Pasting ${clipboard.noteCount} notes $place ${targetNote.title}

                targetLft: ${targetNote.position.lft}
                targetRgt: ${targetNote.position.rgt}
                targetLevel: ${targetNote.position.level}

                pastedLft: $pastedLft
                pastedLevel: $pastedLevel
                pastedParentId: $pastedParentId

                levelOffset: $levelOffset
                """.trimIndent())

        makeSpaceForNewNotes(clipboard.noteCount, targetNote, place)

        val lftToNoteIds = LongSparseArray<Long>()

        val notesWithParentSet = HashSet<Long>()

        var count = 0

        val batchId = System.currentTimeMillis()

        val org = clipboard.toOrg()

        val useCreatedAtProperty = AppPreferences.createdAt(context)
        val createdAtProperty = AppPreferences.createdAtProperty(context)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Parsing clipboard:\n$org")

        OrgParser.Builder()
                .setInput(org)
                .setTodoKeywords(AppPreferences.todoKeywordsSet(context))
                .setDoneKeywords(AppPreferences.doneKeywordsSet(context))
                .setListener(object : OrgNestedSetParserListener {
                    @Throws(IOException::class)
                    override fun onNode(node: OrgNodeInSet) {

                        // Skip root node
                        if (node.level == 0) {
                            return
                        }

                        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Parsed $node")

                        val lft = pastedLft + node.lft - 2
                        val rgt = pastedLft + node.rgt - 2

                        val position = NotePosition(
                                bookId = bookId,
                                lft = lft,
                                rgt = rgt,
                                level = node.level + levelOffset,
                                parentId = if (node.level == 1) pastedParentId else 0,
                                foldedUnderId = 0,
                                isFolded = false,
                                descendantsCount = node.descendantsCount)

                        val scheduledRangeId = getOrgRangeId(node.head.scheduled)
                        val deadlineRangeId = getOrgRangeId(node.head.deadline)
                        val closedRangeId = getOrgRangeId(node.head.closed)
                        val clockRangeId = getOrgRangeId(node.head.clock)

                        var content: String? = null
                        var contentLineCount = 0

                        if (node.head.hasContent()) {
                            content = node.head.content
                            contentLineCount = MiscUtils.lineCount(node.head.content)
                        }

                        val note = Note(
                                0,
                                title = node.head.title,
                                priority = node.head.priority,
                                state = node.head.state,
                                scheduledRangeId = scheduledRangeId,
                                deadlineRangeId = deadlineRangeId,
                                closedRangeId = closedRangeId,
                                clockRangeId = clockRangeId,
                                tags = if (node.head.hasTags()) Note.dbSerializeTags(node.head.tags) else null,
                                createdAt = getCreatedAtFromProperty(node, useCreatedAtProperty, createdAtProperty),
                                content = content,
                                contentLineCount = contentLineCount,
                                position = position,
                                isCut = batchId)

                        val noteId = db.note().insert(note)

                        pastedNoteIds += noteId

                        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Inserted $noteId $note")

                        lftToNoteIds.put(lft, noteId)

                        insertNoteProperties(noteId, node.head.properties)
                        insertNoteEvents(noteId, note.title, note.content)

                        /*
                         * Update notes' parent IDs and insert ancestors.
                         * Go through all note's descendants - notes between lft and rgt.
                         */
                        for (num in lft + 1 until rgt) {
                            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$lft <- $num -> $rgt")

                            lftToNoteIds.get(num)?.let { descendantNoteId ->
                                // Update parent ID
                                if (!notesWithParentSet.contains(descendantNoteId)) {
                                    if (BuildConfig.LOG_DEBUG)
                                        LogUtils.d(TAG, "Updating parent for $descendantNoteId to $noteId")

                                    db.note().updateParentForNote(descendantNoteId, noteId)

                                    notesWithParentSet.add(descendantNoteId)
                                }

                                if (BuildConfig.LOG_DEBUG)
                                    LogUtils.d(TAG, "Inserting $noteId as ancestor of $descendantNoteId")

                                // Insert ancestor
//                                db.noteAncestor().insert(NoteAncestor(
//                                        noteId = descendantNoteId,
//                                        bookId = bookId,
//                                        ancestorNoteId = noteId))
                            }
                        }

                        count++
                    }

                    override fun onFile(file: OrgFile?) {
                    }
                }).build().parse()


//        db.note().incrementLftForLftGe(targetNote.position.bookId, pastedLft, positionsRequired)
//        db.note().incrementRgtForRgtGeOrRoot(targetNote.position.bookId, pastedLft, positionsRequired)
//
        db.note().unfoldNotBelongingToBatch(batchId)
//
        if (foldedUnder != 0L) {
            db.note().markBatchAsFolded(batchId, foldedUnder)
        }
//
//
//        db.note().moveBatch(batchId, targetNote.position.bookId, positionOffset, levelOffset)
//
        db.noteAncestor().insertAncestorsForBatch(batchId)
//
        db.note().makeBatchVisible(batchId)
//
        // Update descendants count for the note and its ancestors
        db.note().updateDescendantsCountForNoteAndAncestors(listOf(targetNote.id))
//
//        // Delete other batches
//        db.note().deleteCut()
//
        updateBookIsModified(targetNote.position.bookId, true)

        return pastedNoteIds
    }

    private fun pasteNotesBatch(batchId: Long, place: Place, targetNoteId: Long): Int {
        var foldedUnder: Long = 0

        val batchData = db.note().getBatchData(batchId) ?: return 0

        val targetNote = db.note().get(targetNoteId) ?: return 0

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, batchId, "Pasting $place ${targetNote.title}")

        val pastedLft: Long
        val pastedLevel: Int
        val pastedParentId: Long

        /* If target note is hidden, hide pasted under the same note. */
        if (targetNote.position.foldedUnderId != 0L) {
            foldedUnder = targetNote.position.foldedUnderId
        }

        when (place) {
            Place.ABOVE -> {
                pastedLft = targetNote.position.lft
                pastedLevel = targetNote.position.level
                pastedParentId = targetNote.position.parentId
            }

            Place.UNDER -> {
                val lastDescendant = db.note().getLastHighestLevelDescendant(
                        targetNote.position.bookId, targetNote.position.lft, targetNote.position.rgt)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, batchId, "lastDescendant: $lastDescendant")


                if (lastDescendant != null) {
                    /* Insert batch after last descendant with highest level. */
                    pastedLft = lastDescendant.position.rgt + 1
                    pastedLevel = lastDescendant.position.level

                } else {
                    /* Insert batch just under the target note. */
                    pastedLft = targetNote.position.lft + 1
                    pastedLevel = targetNote.position.level + 1
                }

                if (targetNote.position.isFolded) {
                    foldedUnder = targetNote.id
                }

                pastedParentId = targetNote.id
            }

            Place.UNDER_AS_FIRST -> {
                pastedLft = targetNote.position.lft + 1
                pastedLevel = targetNote.position.level + 1

                if (targetNote.position.isFolded) {
                    foldedUnder = targetNote.id
                }

                pastedParentId = targetNote.id
            }

            Place.BELOW -> {
                pastedLft = targetNote.position.rgt + 1
                pastedLevel = targetNote.position.level
                pastedParentId = targetNote.position.parentId
            }

            else -> throw IllegalArgumentException("Unsupported place for paste: $place")
        }

        val positionsRequired = (batchData.maxRgt - batchData.minLft + 1).toInt()
        val positionOffset = pastedLft - batchData.minLft
        val levelOffset = pastedLevel - batchData.minLevel

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, batchId, "positionOffset: $positionOffset levelOffset: $levelOffset positionsRequired: $positionsRequired")

        /*
         * Make space for new notes incrementing lft and rgt.
         * FIXME: This could be slow.
         */

        db.note().incrementLftForLftGe(targetNote.position.bookId, pastedLft, positionsRequired)
        db.note().incrementRgtForRgtGeOrRoot(targetNote.position.bookId, pastedLft, positionsRequired)

        db.note().unfoldNotBelongingToBatch(batchId)

        if (foldedUnder != 0L) {
            db.note().markBatchAsFolded(batchId, foldedUnder)
        }

        db.note().updateParentOfBatchRoot(batchId, batchData.minLft, pastedParentId)

        db.note().moveBatch(batchId, targetNote.position.bookId, positionOffset, levelOffset)

        db.noteAncestor().insertAncestorsForBatch(batchId)

        val count = db.note().makeBatchVisible(batchId)

        // Update descendants count for the note and its ancestors
        db.note().updateDescendantsCountForNoteAndAncestors(listOf(targetNote.id))

        // Delete other batches
        db.note().deleteCut()

        updateBookIsModified(targetNote.position.bookId, true)

        return count
    }

    fun setNotesScheduledTime(noteIds: Set<Long>, time: OrgDateTime?) {
        val timeId = if (time != null) getOrgRangeId(OrgRange(time)) else null

        db.note().updateScheduledTime(noteIds, timeId)

        db.note().get(noteIds).map { it.position.bookId }.let {
            updateBookIsModified(it, true)
        }
    }

    fun setNotesDeadlineTime(noteIds: Set<Long>, time: OrgDateTime?) {
        val timeId = if (time != null) getOrgRangeId(OrgRange(time)) else null

        db.note().updateDeadlineTime(noteIds, timeId)

        db.note().get(noteIds).map { it.position.bookId }.let {
            updateBookIsModified(it, true)
        }
    }

    fun toggleNoteFoldedState(noteId: Long): Int {
        return db.runInTransaction(Callable {
            val note = db.note().get(noteId) ?: return@Callable 0

            val toggled = db.note().updateIsFolded(note.id, !note.position.isFolded)

            if (note.position.isFolded) {
                db.note().unfoldDescendantsUnderId(note.position.bookId, note.id, note.position.lft, note.position.rgt)
            } else {
                db.note().foldDescendantsUnderId(note.position.bookId, note.id, note.position.lft, note.position.rgt)
            }

            return@Callable toggled
        })
    }

    fun setNoteStateToDone(noteId: Long): Int {
        val firstDone = AppPreferences.getFirstDoneState(context) ?: return 0

        return setNotesState(setOf(noteId), firstDone)
    }

    fun toggleNotesState(noteIds: Set<Long>): Int {
        val firstTodo = AppPreferences.getFirstTodoState(context)
        val firstDone = AppPreferences.getFirstDoneState(context)

        if (firstTodo != null && firstDone != null) {
            val allNotesAreDone = db.note().get(noteIds).firstOrNull { note ->
                !AppPreferences.isDoneKeyword(context, note.state)
            } == null

            return if (allNotesAreDone) {
                setNotesState(noteIds, firstTodo)
            } else {
                setNotesState(noteIds, firstDone)
            }
        }

        return 0
    }

    fun setNotesState(noteIds: Set<Long>, state: String?): Int {
        return db.runInTransaction(Callable {
            /*
             * Notebooks must be updated before notes,
             * because this query checks for notes what will be affected.
             */
            updateBookIsModified(db.note().getBookIdsForNotesNotMatchingState(noteIds, state), true)

            return@Callable if (AppPreferences.isDoneKeyword(context, state)) {
                var updated = 0

                db.note().getStateUpdatedNotes(noteIds, state).forEach { note ->

                    val stateSetOp = StateChangeLogic(AppPreferences.doneKeywordsSet(context))

                    stateSetOp.setState(
                            state,
                            note.state,
                            OrgRange.parseOrNull(note.scheduled),
                            OrgRange.parseOrNull(note.deadline))

                    updated += db.note().update(
                            note.noteId,
                            stateSetOp.state,
                            getOrgRangeId(stateSetOp.scheduled),
                            getOrgRangeId(stateSetOp.deadline),
                            getOrgRangeId(stateSetOp.closed))

                    if (stateSetOp.isShifted) {
                        val now = OrgDateTime(false).toString()

                        if (AppPreferences.setLastRepeatOnTimeShift(context)) {
                            setNoteProperty(note.noteId, OrgFormatter.LAST_REPEAT_PROPERTY, now)
                        }

                        if (AppPreferences.logOnTimeShift(context)) {
                            val stateChangeLine = OrgFormatter.stateChangeLine(note.state, state, now)
                            val noteContent = OrgFormatter.insertLogbookEntryLine(note.content, stateChangeLine)

                            db.note().updateContent(note.noteId, noteContent)
                        }
                    }
                }

                updated

            } else { // Set to non-done state
                db.note().updateStateAndRemoveClosedTime(noteIds, state)
            }
        })
    }

    fun updateNoteContent(bookId: Long, noteId: Long, content: String?) {
        db.runInTransaction {
            db.note().updateContent(noteId, content)

            updateBookIsModified(bookId, true)
        }
    }

    fun selectNotesFromQueryLiveData(queryString: String): LiveData<List<NoteView>> {
        val parser = InternalQueryParser()

        val query = parser.parse(queryString)

        val sqlQuery = buildSqlQuery(query)

        return db.noteView().runQueryLiveData(sqlQuery)
    }

    fun selectNotesFromQuery(query: Query): List<NoteView> {
        val sqlQuery = buildSqlQuery(query)

        return db.noteView().runQuery(sqlQuery)
    }

    private fun buildSqlQuery(query: Query): SupportSQLiteQuery {
        val queryBuilder = SqliteQueryBuilder(context)

        val (selection, selectionArgs, orderBy) = queryBuilder.build(query)

        val s = mutableListOf<String>()

        if (query.condition != null) {
            s.add(selection)
        }

        if (query.options.agendaDays > 0) {
            s.add("(scheduled_range_id IS NOT NULL OR deadline_range_id IS NOT NULL OR event_timestamp IS NOT NULL)")
        }

        if (!s.isEmpty() || !query.sortOrders.isEmpty()) {
            s.add(NoteDao.WHERE_EXISTING_NOTES)
        }

        val selection2 = if (s.isEmpty()) "0" else TextUtils.join(" AND ", s)

        // For agenda, group by event timestamp too
        val groupBy = if (query.isAgenda()) {
            "notes.id, event_timestamp"
        } else {
            "notes.id"
        }

        val supportQuery = SupportSQLiteQueryBuilder
                .builder("(${NoteViewDao.QUERY_WITH_NOTE_EVENTS} GROUP BY $groupBy)")
                .selection(selection2, selectionArgs.toTypedArray())
                .orderBy(orderBy)
                .create()

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, "Selecting notes using query $query "
                    + "with selection args $selectionArgs\n${supportQuery.sql}")

        return supportQuery
    }

    fun getNotes(bookName: String): List<NoteView> {
        return db.noteView().getBookNotes(bookName)
    }

    fun getVisibleNotesLiveData(bookId: Long): LiveData<List<NoteView>> {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId)
        return db.noteView().getVisibleLiveData(bookId)
    }

    fun getNoteCount(bookId: Long): Int {
        return db.note().getCount(bookId)
    }

    fun getNotePayload(noteId: Long): NotePayload? {
        val noteView = getNoteView(noteId)

        if (noteView != null) {
            val properties = getNoteProperties(noteId)

            return NoteBuilder.newPayload(noteView, properties)
        }

        return null
    }

    fun getNoteView(id: Long): NoteView? {
        return db.noteView().get(id)
    }

    fun getNoteView(title: String): NoteView? {
        return db.noteView().get(title)
    }

    fun getSubtrees(ids: Set<Long>): List<NoteView> {
        return db.noteView().getSubtrees(ids)
    }

    fun getNote(title: String): Note? {
        return db.note().get(title)
    }

    fun getNote(noteId: Long): Note? {
        return db.note().get(noteId)
    }

    fun getNoteAncestors(noteId: Long): List<Note> {
        return db.note().getAncestors(noteId)
    }

    fun getNoteProperties(noteId: Long): List<NoteProperty> {
        return db.noteProperty().get(noteId)
    }

    fun setNoteProperty(noteId: Long, name: String, value: String) {
        db.noteProperty().upsert(noteId, name, value)
    }

    private fun replaceNoteProperties(noteId: Long, properties: Map<String, String>) {
        db.noteProperty().delete(noteId)

        insertNoteProperties(noteId, properties)
    }

    fun insertNoteProperties(noteId: Long, properties: Map<String, String>) {
        var position = 1

        properties.forEach { (name, value) ->
            val property = NoteProperty(noteId, position++, name, value)
            db.noteProperty().insert(property)
        }
    }

    private fun setNoteCreatedAtTime(noteId: Long, time: Long) {
        db.note().updateCreatedAtTime(noteId, time)
    }

    fun createNoteFromNotification(title: String) {
        val book = getTargetBook(context)

        val notePayload = NoteBuilder.newPayload(context, title, "")

        createNote(notePayload, NotePlace(book.book.id))
    }

    /**
     * Creates new note adding created-at time to it.
     */
    fun createNote(notePayload: NotePayload, target: NotePlace): Note {
        val createdAt = System.currentTimeMillis()

        val payload = if (AppPreferences.createdAt(context)) {
            // Set created-at property
            val propName = AppPreferences.createdAtProperty(context)
            val propValue = OrgDateTime(createdAt, false).toString()
            val property = Pair(propName, propValue)
            notePayload.copy(properties = notePayload.properties + property)

        } else {
            notePayload
        }

        return db.runInTransaction(Callable {
            createNote(payload, target, createdAt)
        })
    }

    private fun createNote(notePayload: NotePayload, target: NotePlace, time: Long): Note {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, notePayload, target, time)

        val targetNote = if (target.place != Place.UNSPECIFIED) {
            db.note().get(target.noteId) ?: throw IOException("Target note not found")
        } else {
            null
        }

        val newNotePosition = when (target.place) {
            Place.ABOVE -> {
                if (targetNote == null) {
                    throw IOException("Target note not found")
                }

                NotePosition(
                        bookId = target.bookId,
                        lft = targetNote.position.lft,
                        rgt = targetNote.position.lft + 1,
                        level = targetNote.position.level,
                        parentId = targetNote.position.parentId)
            }

            Place.BELOW -> {
                if (targetNote == null) {
                    throw IOException("Target note not found")
                }

                NotePosition(
                        bookId = target.bookId,
                        lft = targetNote.position.rgt + 1,
                        rgt = targetNote.position.rgt + 2,
                        level = targetNote.position.level,
                        parentId = targetNote.position.parentId
                )
            }

            Place.UNDER -> {
                if (targetNote == null) {
                    throw IOException("Target note not found")
                }

                NotePosition(
                        bookId = target.bookId,
                        lft = targetNote.position.rgt,
                        rgt = targetNote.position.rgt + 1,
                        level = targetNote.position.level + 1,
                        parentId = targetNote.id,
                        foldedUnderId = if (targetNote.position.isFolded) targetNote.id else 0
                )
            }

            Place.UNDER_AS_FIRST -> {
                TODO("Insert UNDER_AS_FIRST not implemented")
            }

            Place.UNSPECIFIED -> {
                /* If target note is not used, add note at the end with level 1. */
                val rootRgt = db.note().getMaxRgtForBook(target.bookId) ?: 0
                val rootId = db.note().getRootNodeId(target.bookId) ?: 0

                NotePosition(
                        bookId = target.bookId,
                        lft = rootRgt,
                        rgt = rootRgt + 1,
                        level = 1,
                        parentId = rootId
                )
            }
        }

        // Make space for new note
        if (target.place != Place.UNSPECIFIED) {
            makeSpaceForNewNotes(1, targetNote!!, target.place)

            val count = db.note().incrementDescendantsCountForAncestors(
                    target.bookId, newNotePosition.lft, newNotePosition.rgt)

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Updated descendants_count for $count notes (${target.bookId}, ${newNotePosition.lft}, ${newNotePosition.rgt})")

        }
        db.note().incrementRgtForRootNote(target.bookId)

        val noteEntity = Note(
                0,
                0,
                time,
                notePayload.title,
                Note.dbSerializeTags(notePayload.tags),
                notePayload.state,
                notePayload.priority,
                notePayload.content,
                MiscUtils.lineCount(notePayload.content),
                getOrgRangeId(notePayload.scheduled),
                getOrgRangeId(notePayload.deadline),
                getOrgRangeId(notePayload.closed),
                null,
                newNotePosition)


        val noteId = db.note().insert(noteEntity)

        replaceNoteProperties(noteId, notePayload.properties)
        replaceNoteEvents(noteId, notePayload.title, notePayload.content)

        db.noteAncestor().insertAncestorsForNote(noteId)

        updateBookIsModified(target.bookId, true, time)

        return noteEntity.copy(id = noteId)
    }

    /**
     * Increment note's lft and rgt to make space for new notes.
     */
    private fun makeSpaceForNewNotes(numberOfNotes: Int, targetNote: Note, place: Place) {
        val spaceRequired = numberOfNotes * 2

        val bookId = targetNote.position.bookId

        when (place) {
            Place.ABOVE -> {
                db.note().incrementLftForLftGe(bookId, targetNote.position.lft, spaceRequired)
                db.note().incrementRgtForRgtGtOrRoot(bookId, targetNote.position.lft, spaceRequired)
            }

            Place.UNDER -> {
                db.note().incrementLftForLftGt(bookId, targetNote.position.rgt, spaceRequired)
                db.note().incrementRgtForRgtGeOrRoot(bookId, targetNote.position.rgt, spaceRequired)
            }

            Place.BELOW -> {
                db.note().incrementLftForLftGt(bookId, targetNote.position.rgt, spaceRequired)
                db.note().incrementRgtForRgtGtOrRoot(bookId, targetNote.position.rgt, spaceRequired)
            }

            else -> throw IllegalArgumentException("Unsupported paste relative position $place")
        }
    }


    fun updateNote(noteId: Long, notePayload: NotePayload) {
        val note = db.note().get(noteId) ?: return

        db.runInTransaction {
            updateBookIsModified(note.position.bookId, true)

            replaceNoteProperties(noteId, notePayload.properties)
            replaceNoteEvents(noteId, notePayload.title, notePayload.content)

            val newNote = note.copy(
                    title = notePayload.title,
                    content = notePayload.content,
                    contentLineCount = MiscUtils.lineCount(notePayload.content),
                    state = notePayload.state,
                    priority = notePayload.priority,
                    scheduledRangeId = getOrgRangeId(notePayload.scheduled),
                    deadlineRangeId = getOrgRangeId(notePayload.deadline),
                    closedRangeId = getOrgRangeId(notePayload.closed),
                    tags = Note.dbSerializeTags(notePayload.tags)
            )

            val count = db.note().update(newNote)

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updated $count note: $newNote")
        }
    }

    fun deleteNotes(bookId: Long, ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            val batchId = System.currentTimeMillis()

            db.noteAncestor().deleteForNoteAndDescendants(bookId, ids)

            val count = db.note().markAsCut(bookId, ids, batchId)

            db.note().updateDescendantsCountForAncestors(ids)

            db.note().deleteCut()

            updateBookIsModified(bookId, true)

            count
        })
    }

    private fun replaceNoteEvents(noteId: Long, title: String, content: String?) {
        db.noteEvent().deleteForNote(noteId)

        insertNoteEvents(noteId, title, content)
    }

    private fun insertNoteEvents(noteId: Long, title: String, content: String?) {
        if (title.isNotEmpty()) {
            parseAndInsertEvents(noteId, title)
        }

        if (! content.isNullOrEmpty()) {
            parseAndInsertEvents(noteId, content)
        }
    }

    private fun parseAndInsertEvents(noteId: Long, str: String) {
        OrgActiveTimestamps.parse(str).forEach { range ->
            getOrgRangeId(range)?.let { orgRangeId ->
                db.noteEvent().replace(NoteEvent(noteId, orgRangeId))
            }
        }
    }

    /**
     * Loads book from resource.
     */
    @Throws(IOException::class)
    fun loadBookFromResource(name: String, format: BookFormat, resources: Resources, resId: Int): BookView? {
        resources.openRawResource(resId).use {
            return loadBookFromStream(name, format, it)
        }
    }

    @Throws(IOException::class)
    fun loadBookFromRepo(rook: Rook): BookView? {
        val fileName = BookName.getFileName(context, rook.uri)

        return loadBookFromRepo(rook.repoUri, fileName)
    }

    @Throws(IOException::class)
    fun loadBookFromRepo(repoUri: Uri, fileName: String): BookView? {
        val book: BookView?

        val repo = repoFactory.getFromUri(context, repoUri)
                ?: throw IOException("Unsupported repository URL \"$repoUri\"")

        val tmpFile = getTempBookFile()
        try {
            /* Download from repo. */
            val vrook = repo.retrieveBook(fileName, tmpFile)

            val bookName = BookName.fromFileName(fileName)

            /* Store from file to Shelf. */
            book = loadBookFromFile(bookName.name, bookName.format, tmpFile, vrook)

        } finally {
            tmpFile.delete()
        }

        return book
    }

    @Throws(IOException::class)
    fun loadBookFromStream(name: String, format: BookFormat, inputStream: InputStream): BookView? {
        /* Save content to temporary file. */
        val tmpFile = getTempBookFile()

        try {
            MiscUtils.writeStreamToFile(inputStream, tmpFile)
            return loadBookFromFile(name, format, tmpFile)

        } finally {
            tmpFile.delete()
        }
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun loadBookFromFile(
            name: String,
            format: BookFormat,
            file: File,
            vrook: VersionedRook? = null,
            selectedEncoding: String? = null
    ): BookView? {

        val encoding = if (selectedEncoding == null && AppPreferences.forceUtf8(context)) {
            "UTF-8"
        } else {
            selectedEncoding
        }

        val bookId = loadBookFromFile(file.path, name, vrook, encoding)

        return getBookView(bookId)
    }

    private fun loadBookFromFile(
            filePath: String,
            bookName: String,
            vrook: VersionedRook?,
            selectedEncoding: String?): Long {

        try {
            val encoding = Encoding.detect(filePath, selectedEncoding)

            return db.runInTransaction(Callable {
                loadBookFromReader(
                        bookName,
                        vrook,
                        InputStreamReader(FileInputStream(File(filePath)), encoding.used),
                        encoding
                )
            })

        } catch (e: IOException) {
            e.printStackTrace()

            /* Remember that the Android system must be able to communicate the Exception
             * across process boundaries. This is one of those.
             */
            throw IllegalArgumentException(e)
        }
    }

    @Throws(IOException::class)
    private fun loadBookFromReader(
            bookName: String,
            vrook: VersionedRook?,
            inReader: Reader,
            encoding: Encoding): Long {

        val startedAt = System.currentTimeMillis()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loading book $bookName...")

        val bookId = db.book().getOrInsert(bookName)

        // Delete all notes from this book
        db.note().deleteByBookId(bookId)

        /*
         * Maps node's lft to database id.
         * Used to update parent id and insert ancestors.
         * Not using SparseArray as speed is preferred over memory here.
         */
        @SuppressLint("UseSparseArrays") val lft2id = HashMap<Long, Long>()

        /* Set of ids for which parent is already set. */
        val notesWithParentSet = HashSet<Long>()


        val useCreatedAtProperty = AppPreferences.createdAt(context)
        val createdAtProperty = AppPreferences.createdAtProperty(context)

        BufferedReader(inReader).use { reader ->
            /*
             * Create and run parser.
             * When multiple formats are supported, decide which parser to use here.
             */
            OrgParser.Builder()
                    .setInput(reader)
                    .setTodoKeywords(AppPreferences.todoKeywordsSet(context))
                    .setDoneKeywords(AppPreferences.doneKeywordsSet(context))
                    .setListener(object : OrgNestedSetParserListener {
                        @Throws(IOException::class)
                        override fun onNode(node: OrgNodeInSet) {

                            val scheduledRangeId = getOrgRangeId(node.head.scheduled)
                            val deadlineRangeId = getOrgRangeId(node.head.deadline)
                            val closedRangeId = getOrgRangeId(node.head.closed)
                            val clockRangeId = getOrgRangeId(node.head.clock)

                            var content: String? = null
                            var contentLineCount = 0

                            if (node.head.hasContent()) {
                                content = node.head.content
                                contentLineCount = MiscUtils.lineCount(node.head.content)
                            }

                            val position = NotePosition(
                                    bookId,
                                    node.lft,
                                    node.rgt,
                                    node.level,
                                    0,
                                    0,
                                    false,
                                    node.descendantsCount)

                            val note = Note(
                                    0,
                                    title = node.head.title,
                                    priority = node.head.priority,
                                    state = node.head.state,
                                    scheduledRangeId = scheduledRangeId,
                                    deadlineRangeId = deadlineRangeId,
                                    closedRangeId = closedRangeId,
                                    clockRangeId = clockRangeId,
                                    tags = if (node.head.hasTags()) Note.dbSerializeTags(node.head.tags) else null,
                                    createdAt = getCreatedAtFromProperty(node, useCreatedAtProperty, createdAtProperty),
                                    content = content,
                                    contentLineCount = contentLineCount,
                                    position = position
                            )

                            val noteId = db.note().insert(note)

                            insertNoteProperties(noteId, node.head.properties)
                            insertNoteEvents(noteId, note.title, note.content)

                            /*
                             * Update notes' parent IDs and insert ancestors.
                             * Going through all descendants - nodes between lft and rgt.
                             *
                             *  lft:  1    2    3    4    5   6
                             *            L2   l1   r2   R2
                             */
                            lft2id[node.lft] = noteId
                            for (index in node.lft + 1 until node.rgt) {
                                val descendantId = lft2id[index]
                                if (descendantId != null) {
                                    if (!notesWithParentSet.contains(descendantId)) {
                                        db.note().updateParentForNote(descendantId, noteId)
                                        notesWithParentSet.add(descendantId)
                                    }

                                    db.noteAncestor().insert(NoteAncestor(
                                            noteId = descendantId,
                                            bookId = bookId,
                                            ancestorNoteId = noteId))
                                }
                            }
                        }

                        @Throws(IOException::class)
                        override fun onFile(file: OrgFile) {
                            val book = Book(
                                    bookId,
                                    bookName,
                                    mtime = vrook?.mtime, // Set book's mtime to remote book's
                                    preface = file.preface, // TODO: Move to and rename OrgFileSettings
                                    isIndented = file.settings.isIndented,
                                    title = file.settings.title,
                                    isDummy = false,
                                    usedEncoding = encoding.used,
                                    detectedEncoding = encoding.detected,
                                    selectedEncoding = encoding.selected
                            )

                            db.book().update(book)
                        }

                    })
                    .build()
                    .parse()
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, bookName + ": Parsing done in " +
                    (System.currentTimeMillis() - startedAt) + " ms")

        if (vrook != null) {
            // TODO: Reuse updateBookLinkAndSync

            val repoId = db.repo().getOrInsert(vrook.repoUri.toString())
            db.bookLink().upsert(bookId, repoId)

            val rookUrlId = db.rookUrl().getOrInsert(vrook.uri.toString())
            val rookId = db.rook().getOrInsert(repoId, rookUrlId)

            val versionedRookId = db.versionedRook().replace(
                    VersionedRook(0, rookId, vrook.revision, vrook.mtime))

            db.bookLink().upsert(bookId, repoId)
            db.bookSync().upsert(bookId, versionedRookId)
        }

        updateBookIsModified(bookId, false)

        return bookId
    }

    private fun getOrgRangeId(range: String?): Long? {
        return getOrgRangeId(OrgRange.parseOrNull(range))
    }

    private fun getOrgRangeId(range: OrgRange?): Long? {
        if (range == null) {
            return null
        }

        val str = range.toString()

        val entity = db.orgRange().getByString(str)

        if (entity != null) {
            return entity.id
        }

        val startId = getOrgDateTimeId(range.startTime)
        val endId = if (range.endTime != null) getOrgDateTimeId(range.endTime) else null

        return db.orgRange().insert(com.orgzly.android.db.entity.OrgRange(
                0,
                str,
                startId,
                endId
        ))
    }

    fun openSparseTreeForNote(noteId: Long) {
        val noteView = getNoteView(noteId)

        if (noteView != null) {
            val bookId = noteView.note.position.bookId

            unfoldForNote(bookId, noteId)

            // Open book
            val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
            intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId)
            intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent)
        }
    }

    private fun unfoldForNote(bookId: Long, noteId: Long) {
        foldAllNotes(bookId)
        unfoldForNote(noteId)
    }

    private fun getOrgDateTimeId(timestamp: OrgDateTime): Long {
        return db.orgTimestamp().getByString(timestamp.toString()).let {
            it?.id ?: db.orgTimestamp().insert(OrgTimestampMapper.fromOrgDateTime(timestamp))
        }
    }

    private fun getCreatedAtFromProperty(node: OrgNodeInSet, use: Boolean, name: String): Long? {
        if (use) {
            if (node.head.properties.containsKey(name)) {
                OrgRange.doParse(node.head.properties[name])?.let {
                    return it.startTime.calendar.timeInMillis
                }
            }
        }

        return null
    }

    @Throws(IOException::class)
    fun exportBook(bookId: Long, format: BookFormat): File {
        /* Get book from database. */
        val book = getBook(bookId)
                ?: throw IOException(resources.getString(R.string.book_does_not_exist_anymore))

        /* Get file to write book to. */
        val file = localStorage.getExportFile(book.name, format)

        /* Write book. */
        NotesOrgExporter(context, this).exportBook(book, file)

        /* Make file immediately visible when using MTP.
         * See https://github.com/orgzly/orgzly-android/issues/44
         */
        MediaScannerConnection.scanFile(App.getAppContext(), arrayOf(file.absolutePath), null, null)

        return file
    }

    fun findNoteHavingProperty(name: String, value: String): NoteDao.NoteIdBookId? {
        return db.note().firstNoteHavingPropertyLowerCase(name.toLowerCase(), value.toLowerCase())
    }

    /*
     * Saved search
     */

    fun getSavedSearch(id: Long): SavedSearch? {
        return db.savedSearch().get(id)
    }

    fun getSavedSearches(): List<SavedSearch> {
        return db.savedSearch().getAll()
    }

    fun getSavedSearchesLiveData(): LiveData<List<SavedSearch>> {
        return db.savedSearch().getLiveData()
    }

    /**
     * Get [SavedSearch] by name (case insensitive).
     */
    fun getSavedSearchesByNameIgnoreCase(name: String): List<SavedSearch> {
        return db.savedSearch().getAllByNameIgnoreCase(name)
    }

    fun createSavedSearch(savedSearch: SavedSearch): Long {
        val nextPosition = db.savedSearch().getNextAvailablePosition()
        return db.savedSearch().insert(savedSearch.copy(position = nextPosition))
    }

    fun updateSavedSearch(savedSearch: SavedSearch) {
        db.savedSearch().update(savedSearch)
    }

    fun deleteSavedSearches(ids: Set<Long>) {
        db.savedSearch().delete(ids)
    }

    fun moveSavedSearchUp(id: Long) {
        db.savedSearch().get(id)?.let { savedSearch ->
            db.savedSearch().getFirstAbove(savedSearch.position)?.let {
                swapSavedSearchPositions(savedSearch, it)
            }
        }
    }

    fun moveSavedSearchDown(id: Long) {
        db.savedSearch().get(id)?.let { savedSearch ->
            db.savedSearch().getFirstBelow(savedSearch.position)?.let {
                swapSavedSearchPositions(savedSearch, it)
            }
        }
    }

    private fun swapSavedSearchPositions(savedSearch: SavedSearch, other: SavedSearch) {
        db.runInTransaction {
            db.savedSearch().update(savedSearch.copy(position = other.position))
            db.savedSearch().update(other.copy(position = savedSearch.position))
        }
    }

    fun exportSavedSearches() {
        FileSavedSearchStore(context, this).exportSearches()
    }

    fun importSavedSearches(uri: Uri) {
        FileSavedSearchStore(context, this).importSearches(uri)
    }


    fun replaceSavedSearches(savedSearches: List<SavedSearch>): Int {
        db.savedSearch().deleteAll()
        db.savedSearch().insert(savedSearches)
        return savedSearches.size
    }

    /*
     * Repo
     */

    fun selectRepos(): LiveData<List<Repo>> {
        return db.repo().getAllLiveData()
    }

    fun getReposList(): List<Repo> {
        return db.repo().getAll()
    }

    fun getRepos(): Map<String, SyncRepo> {
        val repos = db.repo().getAll()

        val result = java.util.HashMap<String, SyncRepo>()

        for ((_, url) in repos) {
            val repo = repoFactory.getFromUri(context, url)

            if (repo != null) {
                result[url] = repo
            } else {
                Log.e(TAG, "Unsupported repository URL\"$url\"")
            }
        }

        return result
    }

    fun getRepo(url: String): Repo? {
        return db.repo().get(url)
    }

    fun getRepoLiveData(id: Long): LiveData<Repo> {
        return db.repo().getLiveData(id)
    }

    fun getRepo(id: Long): Repo? {
        return db.repo().get(id)
    }

    fun createRepo(url: String): Long {
        if (getRepo(url) != null) {
            throw RepoCreate.AlreadyExists()
        }
        return db.repo().insert(Repo(0, url))
    }

    /**
     * Since old url might be in use, do not update the existing record, but replace it.
     */
    fun updateRepo(id: Long, url: String) {
        db.repo().replace(id, url)
    }

    fun deleteRepo(id: Long) {
        db.repo().delete(id)
    }

    /*
     * Times
     */

    fun times(): List<ReminderTimeDao.NoteTime> {
        return db.reminderTime().getAll()
    }

    /**
     * Return all known tags
     */
    fun selectAllTagsLiveData(): LiveData<List<String>> {
        return Transformations.map(db.note().getDistinctTagsLiveData()) { tagsList ->
            tagsList.flatMap { Note.dbDeSerializeTags(it) }.distinct()
        }
    }

    fun selectAllTags(): List<String> {
        return db.note().getDistinctTags()
                .flatMap { tagsList ->
                    Note.dbDeSerializeTags(tagsList)
                }
                .distinct()
    }

    /**
     * Using current states configuration, update states and titles for all notes.
     * Keywords that were part of the title can become states and vice versa.
     */
    @Throws(IOException::class)
    fun reParseNotesStateAndTitles(): Int {
        val parserBuilder = OrgParser.Builder()
                .setTodoKeywords(AppPreferences.todoKeywordsSet(context))
                .setDoneKeywords(AppPreferences.doneKeywordsSet(context))

        var updated = 0

        val parserWriter = OrgParserWriter()

        db.runInTransaction {
            db.noteView().getAll().forEach { noteView ->
                val note = noteView.note

                val head = OrgMapper.toOrgHead(noteView)

                val headString = parserWriter.whiteSpacedHead(head, note.position.level, false)

                /* Re-parse heading using current setting of keywords. */
                val file = parserBuilder
                        .setInput(headString)
                        .build()
                        .parse()

                if (file.headsInList.size != 1) {
                    throw IOException("Got ${file.headsInList.size} notes after parsing \"$headString\" generated from $note")
                }

                val newHead = file.headsInList[0].head

                /* Update if state, title or priority are different. */
                if (!TextUtils.equals(newHead.state, head.state) ||
                        !TextUtils.equals(newHead.title, head.title) ||
                        !TextUtils.equals(newHead.priority, head.priority)) {

                    updated += db.note().update(note.id, newHead.title, newHead.state, newHead.priority)
                }
            }
        }

        return updated
    }

    /**
     * Syncs created-at time and property, using lower value if both exist.
     */
    @Throws(IOException::class)
    fun syncCreatedAtTimeWithProperty() {
        db.runInTransaction {
            syncCreatedAtTimeWithPropertyInTransaction()
        }
    }

    private fun syncCreatedAtTimeWithPropertyInTransaction() {
        if (! AppPreferences.createdAt(context)) {
            return
        }

        val propName = AppPreferences.createdAtProperty(context)

        val dbProperties = db.noteProperty().getAll()
                .fold(mutableMapOf<Long, MutableMap<String, String>>()) { map, property ->
                    map.getOrPut(property.noteId) {
                        mutableMapOf()
                    }.apply {
                        put(property.name, property.value)

                        if (BuildConfig.LOG_DEBUG)
                            LogUtils.d(TAG, "Property for note ${property.noteId}: "
                                    + "${property.name} = ${property.value}")
                    }

                    map
                }

        // If new property is added to the note below, book has to be marked as modified.
        val bookIds = HashSet<Long>()

        /*
         * Get all notes.
         * This is slow and only notes that have either created-at time or created-at property
         * are actually needed. But since this syncing (triggered on preference change) is done
         * so rarely, we don't bother.
         */
        db.note().getAll().forEach { note ->
            val propValue = dbProperties[note.id]?.get(propName)

            val propValueOrgDateTime = OrgDateTime.doParse(propValue)

            val noteCreatedAt = note.createdAt ?: 0

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Parsing note ${note.id} with property value $propValue "
                        + "($propValueOrgDateTime) and createdAt $noteCreatedAt")

            if (noteCreatedAt > 0 && propValueOrgDateTime == null) {
                // Note has only created-at time

                setNoteProperty(note, propName, noteCreatedAt, propValue, bookIds)

            } else if (noteCreatedAt > 0 && propValueOrgDateTime != null) {
                // Both note property and created-at time exist

                // Use older created-at
                if (propValueOrgDateTime.calendar.timeInMillis < noteCreatedAt) {
                    setNoteCreatedAt(note, propValueOrgDateTime, noteCreatedAt)
                } else {
                    setNoteProperty(note, propName, noteCreatedAt, propValue, bookIds)
                }

                // Or prefer property and set created-at time?

            } else if (noteCreatedAt == 0L && propValueOrgDateTime != null) {
                // Note has only property
                setNoteCreatedAt(note, propValueOrgDateTime, noteCreatedAt)

            } // else: Neither created-at time nor property are set
        }

        updateBookIsModified(bookIds.toList(), true)
    }

    private fun setNoteCreatedAt(note: Note, propValue: OrgDateTime, currValue: Long) {
        val value = propValue.calendar.timeInMillis

        if (value != currValue) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Updating created-at time", note.id, currValue, value)

            setNoteCreatedAtTime(note.id, value)

        } else {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Skipping update", note.id, value)
        }
    }

    private fun setNoteProperty(
            note: Note,
            createdAtPropName: String,
            createdAt: Long,
            currPropValue: String?,
            bookIds: MutableSet<Long>) {

        val value = OrgDateTime(createdAt, false).toString()

        if (value != currPropValue) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Updating property", note.id, createdAtPropName, currPropValue, value)

            setNoteProperty(note.id, createdAtPropName, value)

            // Mark book as modified
            bookIds.add(note.position.bookId)

        } else {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Skipping update", note.id, createdAtPropName, value)
        }
    }

    fun importGettingStartedBook() {
        val name = resources.getString(R.string.getting_started_notebook_name)

        db.runInTransaction {
            val book = loadBookFromResource(
                    name,
                    BookFormat.ORG,
                    resources,
                    GETTING_STARTED_NOTEBOOK_RESOURCE_ID)

            if (book != null) {
                setBookLastActionAndSyncStatus(book.book.id, BookAction.forNow(
                        BookAction.Type.INFO,
                        resources.getString(R.string.loaded_from_resource, name)))
            }
        }
    }

    /**
     * Clear all data from tables.
     */
    fun clearDatabase() {
        db.runInTransaction {
            db.clearAllTables()

            OrgzlyDatabase.insertDefaultSearches(db.openHelper.writableDatabase)
        }

        /* Clear last sync time. */
        AppPreferences.lastSuccessfulSyncTime(context, 0L)

        val intent = Intent(AppIntent.ACTION_DB_CLEARED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Recalculate timestamps after time zone change.
     */
    fun updateTimestamps() {
        db.orgTimestamp().getAll().forEach {
            val timestamp = OrgDateTime.doParse(it.string).calendar.timeInMillis
            db.orgTimestamp().update(it.copy(timestamp = timestamp))
        }
    }

    companion object {
        private val TAG = DataRepository::class.java.name

        const val GETTING_STARTED_NOTEBOOK_RESOURCE_ID = R.raw.orgzly_getting_started
    }
}

package com.orgzly.android.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.*
import com.orgzly.android.data.mappers.OrgMapper
import com.orgzly.android.db.NotesClipboard
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
import com.orgzly.android.repos.*
import com.orgzly.android.repos.Rook
import com.orgzly.android.repos.VersionedRook
import com.orgzly.android.savedsearch.FileSavedSearchStore
import com.orgzly.android.sync.BookSyncStatus
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NoteBuilder
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.android.usecase.RepoCreate
import com.orgzly.android.util.*
import com.orgzly.org.OrgActiveTimestamps
import com.orgzly.org.OrgFile
import com.orgzly.org.OrgFileSettings
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.parser.OrgNestedSetParserListener
import com.orgzly.org.parser.OrgNodeInSet
import com.orgzly.org.parser.OrgParser
import com.orgzly.org.parser.OrgParserWriter
import com.orgzly.org.utils.StateChangeLogic
import java.io.*
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Split
@Singleton
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
            if (book.linkRepo == null) {
                throw IOException(resources.getString(R.string.message_book_has_no_link))
            }

            setBookLastActionAndSyncStatus(bookId, BookAction.forNow(
                    BookAction.Type.PROGRESS,
                    resources.getString(R.string.force_loading_from_uri, book.linkRepo.url)))

            val fileName = BookName.getFileName(context, book)

            val loadedBook = loadBookFromRepo(book.linkRepo.id, book.linkRepo.type, book.linkRepo.url, fileName)

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
            val repoEntity = book.linkRepo ?: defaultRepoForSavingBook()

            setBookLastActionAndSyncStatus(book.book.id, BookAction.forNow(
                    BookAction.Type.PROGRESS,
                    resources.getString(R.string.force_saving_to_uri, repoEntity)))

            saveBookToRepo(repoEntity, fileName, book, BookFormat.ORG)

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
    fun saveBookToRepo(
            repoEntity: Repo,
            fileName: String,
            bookView: BookView,
            @Suppress("UNUSED_PARAMETER") format: BookFormat) {

        val uploadedBook: VersionedRook

        val repo = getRepoInstance(repoEntity.id, repoEntity.type, repoEntity.url)

        val tmpFile = getTempBookFile()
        try {
            /* Write to temporary file. */
            NotesOrgExporter(this).exportBook(bookView.book, tmpFile)

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

    /*
     * If there is only one repository, return its URL.
     * If there are more, we don't know which one to use, so throw exception.
     */
    @Throws(IOException::class)
    private fun defaultRepoForSavingBook(): Repo {
        val repos = getRepos()

        return when {
            repos.size == 1 -> repos.first()
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

    fun getBookOrThrow(id: Long): Book {
        return db.book().get(id) ?: throw IllegalStateException("Book with ID $id not found")
    }

    fun getBookLiveData(id: Long): LiveData<Book> {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)
        return db.book().getLiveData(id)
    }

    /**
     * Returns full string content of the book in format specified. Used by tests.
     */
    @Throws(IOException::class)
    fun getBookContent(name: String, @Suppress("UNUSED_PARAMETER") format: BookFormat): String? {
        val book = getBook(name)

        if (book != null) {
            val file = getTempBookFile()
            try {
                NotesOrgExporter(this).exportBook(book, file)
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

        return BookView(Book(bookId, name, isDummy = true), 0)
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

    fun deleteBook(book: BookView, deleteLinked: Boolean) {
        if (deleteLinked) {
            book.syncedTo?.let { vrook ->
                val repo = getRepoInstance(vrook.repoId, vrook.repoType, vrook.repoUri.toString())

                repo.delete(vrook.uri)
            }
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
        if (bookView.linkRepo != null && bookView.syncedTo != null) {
            if (!TextUtils.equals(bookView.linkRepo.url, bookView.syncedTo.repoUri.toString())) {
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
        bookView.syncedTo?.let { vrook ->
            val repo = getRepoInstance(vrook.repoId, vrook.repoType, vrook.repoUri.toString())

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
        val rookUrl = uploadedBook.uri.toString()
        val repoId = uploadedBook.repoId
        val rookRevision = uploadedBook.revision
        val rookMtime = uploadedBook.mtime

        val rookUrlId = db.rookUrl().getOrInsert(rookUrl)
        val rookId = db.rook().getOrInsert(repoId, rookUrlId)

        val versionedRookId = db.versionedRook().replace(
                com.orgzly.android.db.entity.VersionedRook(
                        0, rookId, rookRevision, rookMtime))

        db.bookLink().upsert(bookId, repoId)
        db.bookSync().upsert(bookId, versionedRookId)
    }

    private fun updateBookIsModified(bookId: Long, isModified: Boolean, time: Long = System.currentTimeMillis()) {
        updateBookIsModified(setOf(bookId), isModified, time)
    }

    private fun updateBookIsModified(bookIds: Set<Long>, isModified: Boolean, time: Long = System.currentTimeMillis()) {
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

    fun setLink(bookId: Long, repo: Repo?) {
        if (repo == null) {
            deleteBookLink(bookId)
        } else {
            setBookLink(bookId, repo)
        }
    }

    private fun setBookLink(bookId: Long, repo: Repo) {
        val repoId = checkNotNull(db.repo().get(repo.url)) {
            "Repo ${repo.url} not found"
        }.id

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
        db.note().getNoteAndAncestorsIds(listOf(noteId)).let { ids ->
            if (ids.isNotEmpty()) {
                db.note().unfoldNotes(ids)
                db.note().updateFoldedUnderForNoteFoldedUnderId(ids)
            }
        }
    }

    fun promoteNotes(ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            val note = db.note().getFirst(ids) ?: return@Callable 0

            /* Can only promote notes of level 2 or greater. */
            if (note.position.level <= 1 || note.position.parentId <= 0) {
                return@Callable 0
            }

            // Paste just under parent if note's level is too high, below otherwise
            val parent = db.note().get(note.position.parentId) ?: return@Callable 0
            val count = if (parent.position.level + 1 < note.position.level) {
                moveSubtrees(ids, Place.UNDER_AS_FIRST, note.position.parentId)
            } else {
                moveSubtrees(ids, Place.BELOW, note.position.parentId)
            }

            return@Callable count
        })
    }

    fun demoteNotes(ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            val note = db.note().getFirst(ids) ?: return@Callable 0

            val previousSibling = db.note().getPreviousSibling(
                    note.position.bookId, note.position.lft, note.position.parentId)

            if (previousSibling != null) {
                return@Callable moveSubtrees(ids, Place.UNDER, previousSibling.id)
            } else {
                return@Callable 0
            }
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
                return@Callable moveSubtrees(noteIds, it.place, it.noteId)
            }

            return@Callable 0
        })
    }

    fun refileNotes(noteIds: Set<Long>, target: NotePlace) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Refiling ${noteIds.size} notes to $target")

        if (target.noteId == 0L) { // To book
            val root = getRootNode(target.bookId) ?: return

            db.runInTransaction(Callable {
                moveSubtrees(noteIds, Place.UNDER, root.id)
            })

        } else {
            db.runInTransaction(Callable {
                moveSubtrees(noteIds, Place.UNDER, target.noteId)
            })
        }
    }

    fun pasteNotes(clipboard: NotesClipboard, bookId: Long, noteId: Long, place: Place): Int {
        return db.runInTransaction(Callable {
            pasteNotesClipboard(clipboard, bookId, place, noteId)
        })
    }

    private fun pasteNotesClipboard(
            clipboard: NotesClipboard,
            bookId: Long,
            place: Place,
            targetNoteId: Long): Int {

        val pastedNoteIds = mutableSetOf<Long>()

        // Empty book, use root node
        val targetNote = if (targetNoteId == 0L) {
            db.note().getRootNode(bookId)
        } else {
            db.note().get(targetNoteId)
        } ?: return 0

        val targetPosition = TargetPosition.getInstance(db, targetNote, place)

        val levelOffset = targetPosition.level - 1

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, """
                Pasting ${clipboard.count} notes $place ${targetNote.title}

                targetLft: ${targetNote.position.lft}
                targetRgt: ${targetNote.position.rgt}
                targetLevel: ${targetNote.position.level}

                targetPosition: $targetPosition

                levelOffset: $levelOffset

                Clipboard entries: ${clipboard.entries}
                """.trimIndent())

        makeSpaceForNewNotes(clipboard.count, targetNote, place)

        var lastNoteId = 0L
        val parentIds = ArrayDeque<Long>().apply {
            add(targetPosition.parentId)
        }
        val idsMap = mutableMapOf<Long, Long>()

        for (entry in clipboard.entries) {
            val level = levelOffset + entry.note.position.level

            val lft = targetPosition.lft + entry.note.position.lft - 1
            val rgt = targetPosition.lft + entry.note.position.rgt - 1

            val foldedUnderId = idsMap[entry.note.position.foldedUnderId]
                    ?: if (targetPosition.foldedUnder != 0L) targetPosition.foldedUnder else 0

            while (lastNoteId != 0L && entry.note.position.level > parentIds.size) {
                parentIds.addLast(lastNoteId)
            }

            while (parentIds.size > 0 && entry.note.position.level < parentIds.size) {
                parentIds.removeLast()
            }

            val note = entry.note.copy(
                    id = 0,
                    position = entry.note.position.copy(
                            bookId = targetNote.position.bookId,
                            lft = lft,
                            rgt = rgt,
                            level = level,
                            parentId = parentIds.peekLast() ?: 0,
                            foldedUnderId = foldedUnderId
                    )
            )

            lastNoteId = db.note().insert(note)

            val properties = OrgProperties().apply {
                entry.properties.forEach {
                    put(it.name, it.value)
                }
            }

            insertNoteProperties(lastNoteId, properties)
            insertNoteEvents(lastNoteId, note.title, note.content)

            idsMap[entry.note.id] = lastNoteId

            pastedNoteIds.add(lastNoteId)

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Inserted $lastNoteId $note")
        }

        db.noteAncestor().insertAncestorsForNotes(pastedNoteIds)

        // Update descendants count for the target note and its ancestors
        db.note().updateDescendantsCountForNoteAndAncestors(listOf(targetNote.id))

        unfoldTargetIfMovingUnder(place, targetNote.id)

        updateBookIsModified(targetNote.position.bookId, true)

        return pastedNoteIds.size
    }

    data class NoteWithPosition(val note: Note, val level: Int, val lft: Long, val rgt: Long)

    fun getSubtreesAligned(ids: Set<Long>): List<Note> {
        var subtreeRgt = 0L
        var levelOffset = 0

        var sequence = 0L
        var prevLevel = 0

        val stack = Stack<NoteWithPosition>()

        val notesPerLft = TreeMap<Long, Note>()

        fun output(note: Note, level: Int, lft: Long, rgt: Long) {
            notesPerLft[lft] = note.copy(
                    position = note.position.copy(level = level, lft = lft, rgt = rgt))
        }

        var notes = 0

        db.note().getNotesForSubtrees(ids).forEach { note ->
            notes++

            // First note or next subtree
            if (subtreeRgt == 0L || note.position.rgt > subtreeRgt) {
                subtreeRgt = note.position.rgt
                levelOffset = note.position.level - 1

                while (!stack.empty()) {
                    val popped = stack.pop()

                    sequence++

                    output(popped.note, popped.level, popped.lft, sequence)
                }

                prevLevel = 0
            }

            val level = note.position.level - levelOffset

            if (prevLevel < level) {
                sequence++

                val lft = sequence
                val rgt = 0L // Unknown yet

                stack.push(NoteWithPosition(note, level, lft, rgt))

            } else if (prevLevel == level) {
                sequence++

                val popped = stack.pop()

                output(popped.note, popped.level, popped.lft, sequence)

                sequence++

                val lft = sequence
                val rgt = 0L // Unknown yet

                stack.push(NoteWithPosition(note, level, lft, rgt))

            } else {
                while (!stack.empty()) {
                    val popped = stack.peek()

                    if (popped.level >= level) {
                        stack.pop()

                        sequence++

                        output(popped.note, popped.level, popped.lft, sequence)

                    } else {
                        break
                    }
                }

                sequence++

                val lft = sequence
                val rgt = 0L // Unknown yet

                stack.push(NoteWithPosition(note, level, lft, rgt))
            }

            prevLevel = level
        }

        while (!stack.empty()) {
            val popped = stack.pop()

            sequence++

            output(popped.note, popped.level, popped.lft, sequence)
        }

        return notesPerLft.values.toList()
    }

    private fun moveSubtrees(selectedIds: Set<Long>, place: Place, targetNoteId: Long): Int {
        val targetNote = db.note().get(targetNoteId) ?: return 0

        val targetPosition = TargetPosition.getInstance(db, targetNote, place)

        val alignedNotes = getSubtreesAligned(selectedIds)

        // Update descendant count for ancestors before move, not counting moved notes themselves
        db.note().updateDescendantsCountForAncestors(selectedIds, selectedIds)

        db.noteAncestor().deleteForSubtrees(selectedIds)

        makeSpaceForNewNotes(alignedNotes.size, targetNote, place)


        val ids = mutableSetOf<Long>()
        val sourceBookIds = mutableSetOf<Long>()

        // Update notes
        alignedNotes.map { note ->
            db.note().updateNote(
                    note.id,
                    targetNote.position.bookId,
                    targetPosition.level + note.position.level - 1,
                    targetPosition.lft + note.position.lft - 1,
                    targetPosition.lft + note.position.rgt - 1,
                    // Set parent ID for top-level notes
                    if (note.position.level == 1) {
                        targetPosition.parentId
                    } else {
                        (note.position.parentId)
                    })

            // Collect new note IDs and source book IDs
            ids.add(note.id)
            sourceBookIds.add(note.position.bookId)
        }

        // Update note ancestors table
        db.noteAncestor().insertAncestorsForNotes(ids)

        db.note().updateDescendantsCountForAncestors(selectedIds)

        db.note().unfoldNotesFoldedUnderOthers(ids)
        if (targetPosition.foldedUnder != 0L) {
            db.note().foldUnfolded(ids, targetPosition.foldedUnder)
        }

        // Update descendants count for the note and its ancestors
        db.note().updateDescendantsCountForNoteAndAncestors(listOf(targetNote.id))

        unfoldTargetIfMovingUnder(place, targetNoteId)

        System.currentTimeMillis().let {
            updateBookIsModified(sourceBookIds, true, it)
            updateBookIsModified(targetNote.position.bookId, true, it)
        }

        return alignedNotes.size
    }

    /** Unfold target note and its ancestors if subtree is moved under it. */
    private fun unfoldTargetIfMovingUnder(place: Place, targetNoteId: Long) {
        if (place == Place.UNDER || place == Place.UNDER_AS_FIRST) {
            unfoldForNote(targetNoteId)
        }
    }

    data class TargetPosition(
            val lft: Long = 0,
            val level: Int = 0,
            val parentId: Long = 0,
            val foldedUnder: Long = 0) {

        companion object {
            fun getInstance(db: OrgzlyDatabase, targetNote: Note, place: Place): TargetPosition {
                val lft: Long
                val level: Int
                val parentId: Long

                // If target note is hidden, hide under the same note
                var foldedUnder: Long = 0
                if (targetNote.position.foldedUnderId != 0L) {
                    foldedUnder = targetNote.position.foldedUnderId
                }

                when (place) {
                    Place.ABOVE -> {
                        lft = targetNote.position.lft
                        level = targetNote.position.level
                        parentId = targetNote.position.parentId
                    }

                    Place.UNDER -> {
                        val lastDescendant = db.note().getLastHighestLevelDescendant(
                                targetNote.position.bookId, targetNote.position.lft, targetNote.position.rgt)

                        if (BuildConfig.LOG_DEBUG)
                            LogUtils.d(TAG, "lastDescendant: $lastDescendant")


                        if (lastDescendant != null) {
                            // Insert after last descendant with highest level
                            lft = lastDescendant.position.rgt + 1
                            level = lastDescendant.position.level

                        } else {
                            // Insert just under the target note
                            lft = targetNote.position.lft + 1
                            level = targetNote.position.level + 1
                        }

                        if (targetNote.position.isFolded) {
                            foldedUnder = targetNote.id
                        }

                        parentId = targetNote.id
                    }

                    Place.UNDER_AS_FIRST -> {
                        lft = targetNote.position.lft + 1
                        level = targetNote.position.level + 1

                        if (targetNote.position.isFolded) {
                            foldedUnder = targetNote.id
                        }

                        parentId = targetNote.id
                    }

                    Place.BELOW -> {
                        lft = targetNote.position.rgt + 1
                        level = targetNote.position.level
                        parentId = targetNote.position.parentId
                    }

                    else -> throw IllegalArgumentException("Unsupported place: $place")
                }

                return TargetPosition(lft, level, parentId, foldedUnder)
            }
        }
    }

    fun setNotesScheduledTime(noteIds: Set<Long>, time: OrgDateTime?) {
        val timeId = if (time != null) getOrgRangeId(OrgRange(time)) else null

        db.note().updateScheduledTime(noteIds, timeId)

        db.note().get(noteIds).mapTo(hashSetOf()) { it.position.bookId }.let {
            updateBookIsModified(it, true)
        }
    }

    fun setNotesDeadlineTime(noteIds: Set<Long>, time: OrgDateTime?) {
        val timeId = if (time != null) getOrgRangeId(OrgRange(time)) else null

        db.note().updateDeadlineTime(noteIds, timeId)

        db.note().get(noteIds).mapTo(hashSetOf()) { it.position.bookId }.let {
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

    fun toggleNoteFoldedStateForSubtree(noteId: Long) {
        db.runInTransaction {
            db.note().get(noteId)?.let { note ->
                val foldedCount = db.note().getSubtreeFoldedNoteCount(listOf(noteId))

                if (foldedCount == 0) { // All notes unfolded
                    db.note().foldSubtrees(listOf(note.id))
                } else {
                    db.note().unfoldSubtrees(listOf(noteId))
                }
            }
        }
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
            updateBookIsModified(db.note().getBookIdsForNotesNotMatchingState(noteIds, state).toSet(), true)

            return@Callable if (AppPreferences.isDoneKeyword(context, state)) {
                var updated = 0

                val doneKeywords = AppPreferences.doneKeywordsSet(context)

                db.note().getNoteForStateChange(noteIds, state).forEach { note ->

                    var title = note.title
                    var content = note.content

                    val eventsInNote = EventsInNote(title, content)

                    val scl = StateChangeLogic(doneKeywords)

                    scl.setState(
                            state,
                            note.state,
                            OrgRange.parseOrNull(note.scheduled),
                            OrgRange.parseOrNull(note.deadline),
                            eventsInNote.timestamps.map { OrgRange(it) })

                    if (scl.isShifted) {
                        eventsInNote.replaceEvents(scl.timestamps).apply {
                            title = first
                            content = second
                        }

                        val now = OrgDateTime(false).toString()

                        // Add last-repeat time
                        if (AppPreferences.setLastRepeatOnTimeShift(context)) {
                            setNoteProperty(note.noteId, OrgFormatter.LAST_REPEAT_PROPERTY, now)
                        }

                        // Log state change
                        if (AppPreferences.logOnTimeShift(context)) {
                            val logEntry = OrgFormatter.stateChangeLine(note.state, state, now)
                            content = OrgFormatter.insertLogbookEntryLine(content, logEntry)
                        }
                    }

                    updated += db.note().update(
                            note.noteId,
                            title,
                            content,
                            scl.state,
                            getOrgRangeId(scl.scheduled),
                            getOrgRangeId(scl.deadline),
                            getOrgRangeId(scl.closed))

                    if (scl.isShifted) {
                        replaceNoteEvents(note.noteId, title, content)
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

        val (selection, selectionArgs, having, orderBy) = queryBuilder.build(query)

        val s = mutableListOf<String>()

        if (query.condition != null) {
            s.add(selection)
        }

        if (query.options.agendaDays > 0) {
            s.add("((scheduled_range_id IS NOT NULL AND scheduled_is_active = 1) OR (deadline_range_id IS NOT NULL AND deadline_is_active = 1) OR event_timestamp IS NOT NULL)")
        }

        if (!s.isEmpty() || !query.sortOrders.isEmpty()) {
            s.add(NoteDao.WHERE_EXISTING_NOTES)
        }

        val selection2 = if (s.isEmpty()) "0" else TextUtils.join(" AND ", s)

        // For agenda, group by event timestamp too
        val groupBy = if (query.isAgenda()) {
            "id, event_timestamp"
        } else {
            "id"
        }

        val supportQuery = SupportSQLiteQueryBuilder
                .builder("(${NoteViewDao.QUERY_WITH_NOTE_EVENTS})")
                .selection(selection2, selectionArgs.toTypedArray())
                .groupBy(groupBy)
                .having(having)
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

    fun getVisibleNotesLiveData(bookId: Long, noteId: Long? = null): LiveData<List<NoteView>> {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId)

        return if (noteId != null) {
            // Only return note's subtree
            db.note().get(noteId)?.let { note ->
                db.noteView().getVisibleLiveData(bookId, note.position.lft, note.position.rgt)
            } ?: MutableLiveData<List<NoteView>>()
        } else {
            db.noteView().getVisibleLiveData(bookId)
        }
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

    fun getLastNoteView(title: String): NoteView? {
        return db.noteView().getLast(title)
    }

    fun getLastNote(title: String): Note? {
        return db.note().getLast(title)
    }

    fun getNote(noteId: Long): Note? {
        return db.note().get(noteId)
    }

    fun getNotesByTitle(title: String): List<Note> {
        return db.note().getByTitle(title)
    }

    fun getFirstNote(noteIds: Set<Long>): Note? {
        return db.note().getFirst(noteIds)
    }

    fun getTopLevelNotes(bookId: Long): List<Note> {
        return db.note().getTopLevel(bookId)
    }

    fun getNoteAncestors(noteId: Long): List<Note> {
        return db.note().getAncestors(noteId)
    }

    fun getNoteAndAncestors(noteId: Long): List<Note> {
        return db.note().getNoteAndAncestors(noteId)
    }

    fun getNotesAndSubtrees(ids: Set<Long>): List<Note> {
        return db.note().getNotesForSubtrees(ids)
    }

    fun getNotesAndSubtreesCount(ids: Set<Long>): Int {
        return db.note().getNotesForSubtreesCount(ids)
    }

    fun getNoteChildren(id: Long): List<Note> {
        return db.note().getChildren(id)
    }

    fun getNoteProperties(noteId: Long): List<NoteProperty> {
        return db.noteProperty().get(noteId)
    }

    private fun setNoteProperty(noteId: Long, name: String, value: String) {
        db.noteProperty().upsert(noteId, name, value)
    }

    private fun replaceNoteProperties(noteId: Long, properties: OrgProperties) {
        db.noteProperty().delete(noteId)

        insertNoteProperties(noteId, properties)
    }

    fun insertNoteProperties(noteId: Long, properties: OrgProperties) {
        var position = 1

        properties.all.forEach { property ->
            NoteProperty(noteId, position++, property.name, property.value).let {
                db.noteProperty().insert(it)
            }
        }
    }

    private fun setNoteCreatedAtTime(noteId: Long, time: Long) {
        db.note().updateCreatedAtTime(noteId, time)
    }

    fun createNoteFromNotification(title: String) {
        val book = getTargetBook(context)

        val notePayload = NoteBuilder.newPayload(context, title, "", null)

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

            val properties = notePayload.properties.apply {
                put(propName, propValue)
            }

            notePayload.copy(properties = properties)

        } else {
            notePayload
        }

        return db.runInTransaction(Callable {
            createNote(payload, target, createdAt)
        })
    }

    private fun createNote(notePayload: NotePayload, target: NotePlace, time: Long): Note {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, notePayload, target, time)

        var targetNote = if (target.place != Place.UNSPECIFIED) {
            db.note().get(target.noteId) ?: throw IOException("Target note not found")
        } else {
            null
        }

        /* Set place to ABOVE if prepend is enabled in settings and book is not empty */
        if (target.place == Place.UNSPECIFIED && AppPreferences.isNewNotePrepend(context)) {
            targetNote = db.note().getFirstNoteInBook(target.bookId)
            if (targetNote != null) {
                target.place = Place.ABOVE
            }
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
     * Increment notes' lft and rgt to make space for new notes.
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

            Place.UNDER_AS_FIRST -> {
                db.note().incrementLftForLftGt(bookId, targetNote.position.lft, spaceRequired)
                db.note().incrementRgtForRgtGtOrRoot(bookId, targetNote.position.lft, spaceRequired)
            }

            Place.BELOW -> {
                db.note().incrementLftForLftGt(bookId, targetNote.position.rgt, spaceRequired)
                db.note().incrementRgtForRgtGtOrRoot(bookId, targetNote.position.rgt, spaceRequired)
            }

            else -> throw IllegalArgumentException("Unsupported paste relative position $place")
        }
    }


    fun updateNote(noteId: Long, notePayload: NotePayload): Note? {
        val note = db.note().get(noteId) ?: return null

        return db.runInTransaction(Callable {
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

            newNote
        })
    }

    fun deleteNotes(bookId: Long, ids: Set<Long>): Int {
        return db.runInTransaction(Callable {
            db.noteAncestor().deleteForSubtrees(ids)

            db.note().updateDescendantsCountForAncestors(ids, ids)

            val count = db.note().deleteById(ids)

            updateBookIsModified(bookId, true)

            count
        })
    }

    fun getNoteEvents(noteId: Long): List<NoteEvent> {
        return db.noteEvent().get(noteId)
    }

    private fun replaceNoteEvents(noteId: Long, title: String, content: String?) {
        db.noteEvent().deleteForNote(noteId)

        insertNoteEvents(noteId, title, content)
    }

    private fun insertNoteEvents(noteId: Long, title: String, content: String?) {
        if (title.isNotEmpty()) {
            parseAndInsertEvents(noteId, title)
        }

        if (!content.isNullOrEmpty()) {
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
     * Store the attachment content, in the repo for [bookId].
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun storeAttachment(bookId: Long, notePayload: NotePayload) {
        // Get the fileName from the provider.
        // TODO provide a way to customize the fileName
        val uri = notePayload.attachmentUri!!
        val documentFile: DocumentFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw IOException("Cannot get the fileName for Uri $uri")
        val fileName = documentFile.name

        val attachDir = notePayload.attachDir(context)

        val book = getBookView(bookId)
                ?: throw IOException(resources.getString(R.string.book_does_not_exist_anymore))

        // Not quite sure what repo to use.
        val repoEntity = book.linkRepo ?: defaultRepoForSavingBook()
        val repo = getRepoInstance(repoEntity.id, repoEntity.type, repoEntity.url)

        val tempFile: File
        // Get the InputStream of the content and write it to a File.
        context.contentResolver.openInputStream(uri).use { inputStream ->
            tempFile = getTempBookFile()
            MiscUtils.writeStreamToFile(inputStream, tempFile)
            LogUtils.d(TAG, "Wrote to file $tempFile")
        }

        repo.storeFile(tempFile, attachDir, fileName)
        LogUtils.d(TAG, "Stored file to repo")
        tempFile.delete()
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

        return loadBookFromRepo(rook.repoId, rook.repoType, rook.repoUri.toString(), fileName)
    }

    @Throws(IOException::class)
    fun loadBookFromRepo(repoId: Long, repoType: RepoType, repoUrl: String, fileName: String): BookView? {
        val book: BookView?

        val repo = getRepoInstance(repoId, repoType, repoUrl)

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
            @Suppress("UNUSED_PARAMETER") format: BookFormat,
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
        val startFolded = AppPreferences.notebooksStartFolded(context)

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
                                    bookId = bookId,
                                    lft = node.lft,
                                    rgt = node.rgt,
                                    level = node.level,
                                    parentId = 0,
                                    foldedUnderId = 0,
                                    isFolded = startFolded && node.level > 0,
                                    descendantsCount = node.descendantsCount)

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

                                        if (startFolded && position.level > 0) {
                                            db.note().setFoldedUnder(descendantId, noteId)
                                        }

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
            updateBookLinkAndSync(bookId, vrook)
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

        val rangeEndTime = if (range.endTime != null) { range.endTime } else { null }
        val endId = if (rangeEndTime != null) getOrgDateTimeId(rangeEndTime) else null

        return db.orgRange().insert(OrgRange(0, str, startId, endId))
    }

    fun openBookForNote(noteId: Long, sparseTree: Boolean) {
        val noteView = getNoteView(noteId)

        if (noteView != null) {
            val bookId = noteView.note.position.bookId

            db.runInTransaction {
                if (sparseTree) {
                    foldAllNotes(bookId)
                }
                unfoldForNote(noteId)
            }

            // Open book
            // FIXME: Runs with delay to be executed after the observer for unfoldForNote
            App.EXECUTORS.mainThread().execute {
                Handler().postDelayed({
                    val intent = Intent(AppIntent.ACTION_OPEN_BOOK)
                    intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId)
                    intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
                    LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent)
                }, 100)
            }
        }
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
        NotesOrgExporter(this).exportBook(book, file)

        /* Make file immediately visible when using MTP.
         * See https://github.com/orgzly/orgzly-android/issues/44
         */
        MediaScannerConnection.scanFile(App.getAppContext(), arrayOf(file.absolutePath), null, null)

        return file
    }

    fun exportBook(book: Book, writer: Writer) {
        NotesOrgExporter(this).exportBook(book, writer)
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

    fun exportSavedSearches(uri: Uri?): Int {
        return FileSavedSearchStore(context, this).exportSearches(uri)
    }

    fun importSavedSearches(uri: Uri): Int {
        return FileSavedSearchStore(context, this).importSearches(uri)
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

    fun getRepos(): List<Repo> {
        return db.repo().getAll()
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

    fun createRepo(repoWithProps: RepoWithProps): Long {
        if (getRepo(repoWithProps.repo.url) != null) {
            throw RepoCreate.AlreadyExists()
        }

        val id = db.repo().insert(repoWithProps.repo)

        AppPreferences.repoPropsMap(context, id, repoWithProps.props)

        return id
    }

    fun updateRepo(repoWithProps: RepoWithProps): Long {
        // Since old url might be in use, do not update the existing record, but replace it
        val newId = db.repo().deleteAndInsert(repoWithProps.repo)

        AppPreferences.repoPropsMapDelete(context, repoWithProps.repo.id)

        AppPreferences.repoPropsMap(context, newId, repoWithProps.props)

        return newId
    }

    fun deleteRepo(id: Long) {
        db.repo().delete(id)

        AppPreferences.repoPropsMapDelete(context, id)
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
            tagsList.flatMap { Note.dbDeSerializeTags(it) }.distinct().sorted()
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
        if (!AppPreferences.createdAt(context)) {
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

        updateBookIsModified(bookIds, true)
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

        // Clear last sync time
        AppPreferences.lastSuccessfulSyncTime(context, 0L)

        // Clear repo preferences
        AppPreferences.repoPropsMapDelete(context)

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

    fun getRepoInstance(id: Long, type: RepoType, url: String): SyncRepo {
        // Load additional repo parameters, if available
        val props = getRepoPropsMap(id)

        val repoWithProps = RepoWithProps(Repo(id, type, url), props)

        return repoFactory.getInstance(repoWithProps)
    }

    fun getRepoPropsMap(id: Long): Map<String, String> {
        return AppPreferences.repoPropsMap(context, id)
    }

    companion object {
        private val TAG = DataRepository::class.java.name

        const val GETTING_STARTED_NOTEBOOK_RESOURCE_ID = R.raw.orgzly_getting_started
    }
}

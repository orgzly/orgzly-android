package com.orgzly.android.provider

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.orgzly.BuildConfig
import com.orgzly.android.Note
import com.orgzly.android.NotePosition
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.provider.GenericDatabaseUtils.field
import com.orgzly.android.provider.GenericDatabaseUtils.join
import com.orgzly.android.provider.actions.*
import com.orgzly.android.provider.clients.BooksClient
import com.orgzly.android.provider.clients.NotesClient
import com.orgzly.android.provider.models.*
import com.orgzly.android.provider.views.DbBookView
import com.orgzly.android.provider.views.DbNoteBasicView
import com.orgzly.android.provider.views.DbNoteView
import com.orgzly.android.provider.views.DbTimeView
import com.orgzly.android.query.sql.SqliteQueryBuilder
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.Place
import com.orgzly.android.util.EncodingDetect
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.OrgFormatter
import com.orgzly.org.OrgFile
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.parser.*
import com.orgzly.org.utils.StateChangeLogic
import java.io.*
import java.util.*

class Provider : ContentProvider() {

    private lateinit var mOpenHelper: Database

    private val uris = ProviderUris()

    private val inBatch = ThreadLocal<Boolean>()

    private val isInBatch: Boolean
        get() = inBatch.get() != null && inBatch.get()

    override fun onCreate(): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = Database(context, DATABASE_NAME)

        return true
    }

    override fun getType(uri: Uri): String? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString())
        return null
    }

    @Throws(OperationApplicationException::class)
    override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, operations.size)

        val results: Array<ContentProviderResult>

        val db = mOpenHelper.writableDatabase

        db.beginTransaction()
        try {
            inBatch.set(true)
            results = super.applyBatch(operations)
            inBatch.set(false)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        notifyChange()

        return results
    }

    override fun query(uri: Uri, _projection: Array<String>?, _selection: String?, _selectionArgs: Array<String>?, _sortOrder: String?): Cursor? {
        var projection = _projection
        var selection = _selection
        var selectionArgs = _selectionArgs
        var sortOrder = _sortOrder

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Running query", uri.toString(), projection, selection, selectionArgs, sortOrder)

        /* Gets a readable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.readableDatabase

        val table: String?
        val query: String
        var cursor: Cursor? = null

        when (uris.matcher.match(uri)) {
            ProviderUris.LOCAL_DB_REPO -> table = DbDbRepo.TABLE

            ProviderUris.REPOS -> {
                table = DbRepo.TABLE
                selection = DbRepo.IS_REPO_ACTIVE + "= 1"
                selectionArgs = null
            }

            ProviderUris.REPOS_ID -> {
                table = DbRepo.TABLE
                selection = DbRepo._ID + "=?"
                selectionArgs = arrayOf(uri.lastPathSegment)
            }

            ProviderUris.NOTES -> {
                table = if ("yes" == uri.getQueryParameter("with-extras")) {
                    DbNoteView.VIEW_NAME
                } else {
                    DbNoteBasicView.VIEW_NAME
                }
            }

            ProviderUris.NOTES_SEARCH_QUERIED -> {
                table = null
                cursor = runUserQuery(db, uri.query, sortOrder)
            }

            ProviderUris.BOOKS_ID_NOTES -> {
                val bookId = java.lang.Long.parseLong(uri.pathSegments[1])

                table = null

                query = ("SELECT " + DbNote.TABLE + ".*,"
                         + " a." + DbOrgRange.STRING + " AS " + DbNoteView.SCHEDULED_RANGE_STRING + ","
                         + " b." + DbOrgRange.STRING + " AS " + DbNoteView.DEADLINE_RANGE_STRING + ","
                         + " c." + DbOrgRange.STRING + " AS " + DbNoteView.CLOSED_RANGE_STRING + ","
                         + " d." + DbOrgRange.STRING + " AS " + DbNoteView.CLOCK_RANGE_STRING
                         + " FROM " + DbNote.TABLE
                         + join(DbOrgRange.TABLE, "a", DbOrgRange._ID, DbNote.TABLE, DbNote.SCHEDULED_RANGE_ID)
                         + join(DbOrgRange.TABLE, "b", DbOrgRange._ID, DbNote.TABLE, DbNote.DEADLINE_RANGE_ID)
                         + join(DbOrgRange.TABLE, "c", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOSED_RANGE_ID)
                         + join(DbOrgRange.TABLE, "d", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOCK_RANGE_ID)
                         + " WHERE " + field(DbNote.TABLE, DbNote.BOOK_ID) + "=" + bookId + " AND " + DatabaseUtils.WHERE_VISIBLE_NOTES
                         + " ORDER BY " + DbNote.LFT)

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Notes for book $bookId: $query")
                cursor = db.rawQuery(query, null)
            }

            ProviderUris.NOTES_WITH_PROPERTY -> {
                val propName = uri.getQueryParameter(ProviderContract.Notes.Param.PROPERTY_NAME)
                val propValue = uri.getQueryParameter(ProviderContract.Notes.Param.PROPERTY_VALUE)

                selection = "LOWER(" + field("tpropertyname", DbPropertyName.NAME) + ") = ? AND " +
                        "LOWER(" + field("tpropertyvalue", DbPropertyValue.VALUE) + ") = ? AND " +
                        field("tnotes", DbNote._ID) + " IS NOT NULL"

                selectionArgs = arrayOf(propName.toLowerCase(), propValue.toLowerCase())

                sortOrder = field("tnotes", DbNote.LFT)

                table = DbNoteProperty.TABLE + " " +
                        join(DbNote.TABLE, "tnotes", DbNote._ID, DbNoteProperty.TABLE, DbNoteProperty.NOTE_ID) +
                        join(DbProperty.TABLE, "tproperties", DbProperty._ID, DbNoteProperty.TABLE, DbNoteProperty.PROPERTY_ID) +
                        join(DbPropertyName.TABLE, "tpropertyname", DbPropertyName._ID, "tproperties", DbProperty.NAME_ID) +
                        join(DbPropertyValue.TABLE, "tpropertyvalue", DbPropertyValue._ID, "tproperties", DbProperty.VALUE_ID)

                projection = arrayOf(field("tnotes", DbNote._ID), field("tnotes", DbNote.BOOK_ID))
            }

            ProviderUris.NOTES_ID_PROPERTIES -> {
                val noteId = java.lang.Long.parseLong(uri.pathSegments[1])

                selection = field(DbNoteProperty.TABLE, DbNoteProperty.NOTE_ID) + "=" + noteId
                selectionArgs = null

                sortOrder = DbNoteProperty.POSITION

                table = DbNoteProperty.TABLE + " " +
                        join(DbProperty.TABLE, "tproperties", DbProperty._ID, DbNoteProperty.TABLE, DbNoteProperty.PROPERTY_ID) +
                        join(DbPropertyName.TABLE, "tpropertyname", DbPropertyName._ID, "tproperties", DbProperty.NAME_ID) +
                        join(DbPropertyValue.TABLE, "tpropertyvalue", DbPropertyValue._ID, "tproperties", DbProperty.VALUE_ID)

                projection = arrayOf("tpropertyname." + DbPropertyName.NAME, "tpropertyvalue." + DbPropertyValue.VALUE)
            }

            ProviderUris.BOOKS -> table = DbBookView.VIEW_NAME

            ProviderUris.BOOKS_ID -> {
                table = DbBookView.VIEW_NAME
                selection = DbBook._ID + "=?"
                selectionArgs = arrayOf(uri.lastPathSegment)
            }

            ProviderUris.FILTERS -> table = DbSearch.TABLE

            ProviderUris.FILTERS_ID -> {
                table = DbSearch.TABLE
                selection = DbSearch._ID + "=?"
                selectionArgs = arrayOf(uri.lastPathSegment)
            }

            ProviderUris.CURRENT_ROOKS -> {
                projection = arrayOf(field(DbRepo.TABLE, DbRepo.REPO_URL), field(DbRookUrl.TABLE, DbRookUrl.ROOK_URL), field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_REVISION), field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_MTIME))

                table = DbCurrentVersionedRook.TABLE +
                        " LEFT JOIN " + DbVersionedRook.TABLE + " ON (" + field(DbVersionedRook.TABLE, DbVersionedRook._ID) + "=" + field(DbCurrentVersionedRook.TABLE, DbCurrentVersionedRook.VERSIONED_ROOK_ID) + ")" +
                        " LEFT JOIN " + DbRook.TABLE + " ON (" + field(DbRook.TABLE, DbRook._ID) + "=" + field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_ID) + ")" +
                        " LEFT JOIN " + DbRookUrl.TABLE + " ON (" + field(DbRookUrl.TABLE, DbRookUrl._ID) + "=" + field(DbRook.TABLE, DbRook.ROOK_URL_ID) + ")" +
                        " LEFT JOIN " + DbRepo.TABLE + " ON (" + field(DbRepo.TABLE, DbRepo._ID) + "=" + field(DbRook.TABLE, DbRook.REPO_ID) + ")" +
                        ""
            }

            ProviderUris.TIMES -> {
                table = null

                query = "SELECT " +
                        DbTimeView.NOTE_ID + ", " +
                        DbTimeView.BOOK_ID + ", " +
                        DbTimeView.BOOK_NAME + ", " +
                        DbTimeView.NOTE_STATE + ", " +
                        DbTimeView.NOTE_TITLE + ", " +
                        DbTimeView.TIME_TYPE + ", " +
                        DbTimeView.ORG_TIMESTAMP_STRING +
                        " FROM " + DbTimeView.VIEW_NAME

                cursor = db.rawQuery(query, null)
            }

            else -> throw IllegalArgumentException("URI is not recognized: $uri")
        }

        if (cursor == null) {
            cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder)
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, "Cursor count: " + cursor?.count
                            + " for " + uri.toString() + " "
                            + table + " " + selection + " "
                            + if (selectionArgs != null) TextUtils.join(",", selectionArgs) else "")

        cursor?.setNotificationUri(context.contentResolver, ProviderContract.AUTHORITY_URI)

        return cursor
    }

    private fun runUserQuery(db: SQLiteDatabase, queryString: String, _sortOrder: String?): Cursor {
        var sortOrder = _sortOrder
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, queryString, sortOrder)

        val parser = InternalQueryParser()
        val query = parser.parse(queryString)

        val queryBuilder = SqliteQueryBuilder(context)
        val (selection1, selectionArgs1, orderBy) = queryBuilder.build(query)

        // If order hasn't been passed, try getting it from the query.
        if (sortOrder == null) {
            sortOrder = orderBy
        }

        val selections = ArrayList<String>()

        // if (!TextUtils.isEmpty(sqlQuery.getSelection())) {
        if (query.condition != null) {
            selections.add(selection1)
        }

        if (query.options.agendaDays > 0) {
            selections.add(DatabaseUtils.WHERE_NOTES_WITH_TIMES)
        }

        if (!selections.isEmpty() || !query.sortOrders.isEmpty()) {
            selections.add(DatabaseUtils.WHERE_EXISTING_NOTES)
        }

        val selection = if (selections.isEmpty()) "0" else TextUtils.join(" AND ", selections)

        val selectionArgs = selectionArgs1.toTypedArray()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, query, query.condition, orderBy, selection, selectionArgs, sortOrder)

        return db.query(DbNoteView.VIEW_NAME, null, selection, selectionArgs, null, null, sortOrder)
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString())

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.writableDatabase

        db.beginTransaction()
        try {
            for (i in values.indices) {
                insertUnderTransaction(db, uri, values[i])
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        notifyChange()

        return values.size
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString())

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.writableDatabase

        val resultUri: Uri

        if (isInBatch) {
            resultUri = insertUnderTransaction(db, uri, contentValues)

        } else {
            db.beginTransaction()
            try {
                resultUri = insertUnderTransaction(db, uri, contentValues)

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            notifyChange()
        }

        return resultUri
    }

    private fun insertUnderTransaction(db: SQLiteDatabase, uri: Uri, contentValues: ContentValues?): Uri {
        val id: Long
        val noteId: Long
        val table: String
        val resultUri: Uri

        when (uris.matcher.match(uri)) {
            ProviderUris.LOCAL_DB_REPO -> table = DbDbRepo.TABLE

            ProviderUris.REPOS -> {
                id = DbRepo.insert(db, contentValues!!.getAsString(ProviderContract.Repos.Param.REPO_URL))
                return ContentUris.withAppendedId(uri, id)
            }

            ProviderUris.BOOKS -> {
                table = DbBook.TABLE
                val bid = db.insertOrThrow(table, null, contentValues)
                insertRootNote(db, bid)
                return ContentUris.withAppendedId(uri, bid)
            }

            ProviderUris.FILTERS -> {
                table = DbSearch.TABLE
                ProviderFilters.updateWithNextPosition(db, contentValues)
            }

            ProviderUris.NOTES -> return insertNote(db, uri, contentValues!!, Place.UNSPECIFIED)

            ProviderUris.NOTE_ABOVE -> return insertNote(db, uri, contentValues!!, Place.ABOVE)

            ProviderUris.NOTE_UNDER -> return insertNote(db, uri, contentValues!!, Place.UNDER)

            ProviderUris.NOTE_BELOW -> return insertNote(db, uri, contentValues!!, Place.BELOW)

            ProviderUris.NOTES_PROPERTIES -> {
                noteId = contentValues!!.getAsLong(ProviderContract.NoteProperties.Param.NOTE_ID)!!
                val name = contentValues.getAsString(ProviderContract.NoteProperties.Param.NAME)
                val value = contentValues.getAsString(ProviderContract.NoteProperties.Param.VALUE)
                val pos = contentValues.getAsInteger(ProviderContract.NoteProperties.Param.POSITION)!!

                val nameId = DbPropertyName.getOrInsert(db, name)
                val valueId = DbPropertyValue.getOrInsert(db, value)
                val propertyId = DbProperty.getOrInsert(db, nameId, valueId)
                val notePropertyId = DbNoteProperty.getOrInsert(db, noteId, pos, propertyId)

                return ContentUris.withAppendedId(uri, notePropertyId)
            }

            ProviderUris.LOAD_BOOK_FROM_FILE -> {
                resultUri = loadBookFromFile(contentValues!!)

                return resultUri
            }

            ProviderUris.CURRENT_ROOKS -> {
                resultUri = insertCurrentRook(db, uri, contentValues!!)
                return resultUri
            }

            ProviderUris.BOOKS_ID_SAVED -> {
                resultUri = bookSavedToRepo(db, uri, contentValues!!)
                return resultUri
            }

            else -> throw IllegalArgumentException("URI is not recognized: $uri")
        }

        id = db.insertOrThrow(table, null, contentValues)

        return ContentUris.withAppendedId(uri, id)
    }

    private fun insertRootNote(db: SQLiteDatabase, bookId: Long): Long {
        val rootNote = Note.newRootNote(bookId)

        val values = ContentValues()
        NotesClient.toContentValues(values, rootNote)
        replaceTimestampRangeStringsWithIds(db, values)

        return db.insertOrThrow(DbNote.TABLE, null, values)
    }

    private fun insertNote(db: SQLiteDatabase, uri: Uri, values: ContentValues, place: Place): Uri {
        val notePos = NotePosition()

        val bookId = values.getAsLong(ProviderContract.Notes.UpdateParam.BOOK_ID)!!

        /* If new note is inserted relative to some other note, get info about that target note. */
        var refNoteId: Long = 0
        var refNotePos: NotePosition? = null
        if (place != Place.UNSPECIFIED) {
            refNoteId = java.lang.Long.valueOf(uri.pathSegments[1])
            refNotePos = DbNote.getPosition(db, refNoteId)
        }

        when (place) {
            Place.ABOVE -> {
                notePos.level = refNotePos!!.level
                notePos.lft = refNotePos.lft
                notePos.rgt = refNotePos.lft + 1
                notePos.parentId = refNotePos.parentId
            }

            Place.UNDER -> {
                notePos.level = refNotePos!!.level + 1
                notePos.lft = refNotePos.rgt
                notePos.rgt = refNotePos.rgt + 1
                notePos.parentId = refNoteId

                /*
                 * If note is being created under already folded note, mark it as such
                 * so it doesn't show up.
                 */
                if (refNotePos.isFolded) {
                    notePos.foldedUnderId = refNoteId
                }
            }

            Place.BELOW -> {
                notePos.level = refNotePos!!.level
                notePos.lft = refNotePos.rgt + 1
                notePos.rgt = refNotePos.rgt + 2
                notePos.parentId = refNotePos.parentId
            }

            Place.UNSPECIFIED -> {
                /* If target note is not used, add note at the end with level 1. */
                val rootRgt = getMaxRgt(db, bookId).toLong()
                val rootId = getRootId(db, bookId)

                notePos.level = 1
                notePos.lft = rootRgt
                notePos.rgt = rootRgt + 1
                notePos.parentId = rootId
            }
        }

        when (place) {
            Place.ABOVE, Place.UNDER, Place.BELOW -> {
                /* Make space for new note - increment notes' LFT and RGT. */
                DatabaseUtils.makeSpaceForNewNotes(db, 1, refNotePos!!, place)

                /*
                 * If new note can be an ancestor, increment descendants count of all
                 * its ancestors.
                 */
                incrementDescendantsCountForAncestors(db, bookId, notePos.lft, notePos.rgt)
                /* Make space for new note - increment root's RGT. */
                val selection = DbNote.BOOK_ID + " = " + bookId + " AND " + DbNote.LFT + " = 1"
                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection, 2, ProviderContract.Notes.UpdateParam.RGT)
            }

            Place.UNSPECIFIED -> {
                val selection = DbNote.BOOK_ID + " = " + bookId + " AND " + DbNote.LFT + " = 1"
                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection, 2, ProviderContract.Notes.UpdateParam.RGT)
            }
        }

        notePos.bookId = bookId

        DbNote.toContentValues(values, notePos)

        replaceTimestampRangeStringsWithIds(db, values)

        val id = db.insertOrThrow(DbNote.TABLE, null, values)

        db.execSQL("INSERT INTO " + DbNoteAncestor.TABLE +
                   " (" + DbNoteAncestor.BOOK_ID + ", " +
                   DbNoteAncestor.NOTE_ID +
                   ", " + DbNoteAncestor.ANCESTOR_NOTE_ID + ") " +
                   "SELECT " + field(DbNote.TABLE, DbNote.BOOK_ID) + ", " +
                   field(DbNote.TABLE, DbNote._ID) + ", " +
                   field("a", DbNote._ID) +
                   " FROM " + DbNote.TABLE +
                   " JOIN " + DbNote.TABLE + " a ON (" +
                   field(DbNote.TABLE, DbNote.BOOK_ID) + " = " + field("a", DbNote.BOOK_ID) + " AND " +
                   field("a", DbNote.LFT) + " < " + field(DbNote.TABLE, DbNote.LFT) + " AND " +
                   field(DbNote.TABLE, DbNote.RGT) + " < " + field("a", DbNote.RGT) + ")" +
                   " WHERE " + field(DbNote.TABLE, DbNote._ID) + " = " + id + " AND " + field("a", DbNote.LEVEL) + " > 0")

        return ContentUris.withAppendedId(uri, id)
    }

    private fun incrementDescendantsCountForAncestors(db: SQLiteDatabase, bookId: Long, lft: Long, rgt: Long) {
        db.execSQL("UPDATE " + DbNote.TABLE +
                   " SET " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " = " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " + 1 " +
                   "WHERE " + DatabaseUtils.whereAncestors(bookId, lft, rgt))
    }

    private fun getMaxRgt(db: SQLiteDatabase, bookId: Long): Int {
        val cursor = db.query(
                DbNote.TABLE,
                arrayOf("MAX(" + DbNoteView.RGT + ")"),
                DbNoteView.BOOK_ID + "= " + bookId + " AND " + DbNoteView.IS_CUT + " = 0", null, null, null, null
        )

        cursor.use {
            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    private fun getRootId(db: SQLiteDatabase, bookId: Long): Long {
        val cursor = db.query(
                DbNote.TABLE,
                DatabaseUtils.PROJECTION_FOR_ID,
                DbNoteView.BOOK_ID + "= " + bookId + " AND " + DbNoteView.LEVEL + " = 0", null, null, null, null
        )

        cursor.use {
            return if (cursor.moveToFirst()) {
                cursor.getInt(0).toLong()
            } else {
                0
            }
        }
    }

    private fun bookSavedToRepo(db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri {
        val bookId = java.lang.Long.parseLong(uri.pathSegments[1])
        val repoUrl = values.getAsString(ProviderContract.BooksIdSaved.Param.REPO_URL)
        val rookUrl = values.getAsString(ProviderContract.BooksIdSaved.Param.ROOK_URL)
        val rookRevision = values.getAsString(ProviderContract.BooksIdSaved.Param.ROOK_REVISION)
        val rookMtime = values.getAsLong(ProviderContract.BooksIdSaved.Param.ROOK_MTIME)!!

        val repoId = getOrInsertRepo(db, repoUrl)
        val rookUrlId = DbRookUrl.getOrInsert(db, rookUrl)
        val rookId = getOrInsertRook(db, rookUrlId, repoId)
        val versionedRookId = getOrInsertVersionedRook(db, rookId, rookRevision, rookMtime)

        updateOrInsertBookLink(db, bookId, repoUrl, rookUrl)
        updateOrInsertBookSync(db, bookId, repoUrl, rookUrl, rookRevision, rookMtime)

        db.rawQuery(DELETE_CURRENT_VERSIONED_ROOKS_FOR_ROOK_ID, arrayOf(rookId.toString()))

        val v = ContentValues()
        v.put(DbCurrentVersionedRook.VERSIONED_ROOK_ID, versionedRookId)
        db.insert(DbCurrentVersionedRook.TABLE, null, v)

        return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId)
    }

    private fun insertCurrentRook(db: SQLiteDatabase, uri: Uri, contentValues: ContentValues): Uri {
        val repoUrl = contentValues.getAsString(ProviderContract.CurrentRooks.Param.REPO_URL)
        val rookUrl = contentValues.getAsString(ProviderContract.CurrentRooks.Param.ROOK_URL)
        val revision = contentValues.getAsString(ProviderContract.CurrentRooks.Param.ROOK_REVISION)
        val mtime = contentValues.getAsLong(ProviderContract.CurrentRooks.Param.ROOK_MTIME)!!

        val repoUrlId = getOrInsertRepo(db, repoUrl)
        val rookUrlId = DbRookUrl.getOrInsert(db, rookUrl)
        val rookId = getOrInsertRook(db, rookUrlId, repoUrlId)
        val versionedRookId = getOrInsertVersionedRook(db, rookId, revision, mtime)

        val values = ContentValues()
        values.put(DbCurrentVersionedRook.VERSIONED_ROOK_ID, versionedRookId)
        val id = db.insert(DbCurrentVersionedRook.TABLE, null, values)

        return ContentUris.withAppendedId(uri, id)
    }

    private fun getOrInsertVersionedRook(db: SQLiteDatabase, rookId: Long, revision: String, mtime: Long): Long {
        var id = DatabaseUtils.getId(
                db,
                DbVersionedRook.TABLE,
                DbVersionedRook.ROOK_ID + "=? AND " + DbVersionedRook.ROOK_REVISION + "=? AND " + DbVersionedRook.ROOK_MTIME + "=?",
                arrayOf(rookId.toString(), revision, mtime.toString()))

        if (id == 0L) {
            val values = ContentValues()
            values.put(DbVersionedRook.ROOK_ID, rookId)
            values.put(DbVersionedRook.ROOK_REVISION, revision)
            values.put(DbVersionedRook.ROOK_MTIME, mtime)

            id = db.insertOrThrow(DbVersionedRook.TABLE, null, values)
        }

        return id
    }

    private fun getOrInsertRook(db: SQLiteDatabase, rookUrlId: Long, repoId: Long): Long {
        var id = DatabaseUtils.getId(
                db,
                DbRook.TABLE,
                DbRook.ROOK_URL_ID + "=? AND " + DbRook.REPO_ID + "=?",
                arrayOf(rookUrlId.toString(), repoId.toString()))

        if (id == 0L) {
            val values = ContentValues()
            values.put(DbRook.ROOK_URL_ID, rookUrlId)
            values.put(DbRook.REPO_ID, repoId)

            id = db.insertOrThrow(DbRook.TABLE, null, values)
        }

        return id
    }

    private fun getOrInsertRepo(db: SQLiteDatabase, repoUrl: String): Long {
        var id = DatabaseUtils.getId(
                db,
                DbRepo.TABLE,
                DbRepo.REPO_URL + "=?",
                arrayOf(repoUrl))

        if (id == 0L) {
            val values = ContentValues()
            values.put(DbRepo.REPO_URL, repoUrl)
            values.put(DbRepo.IS_REPO_ACTIVE, 0)

            id = db.insertOrThrow(DbRepo.TABLE, null, values)
        }

        return id
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), selection, selectionArgs)

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.writableDatabase

        val result: Int

        if (isInBatch) {
            result = deleteUnderTransaction(db, uri, selection, selectionArgs)

        } else {
            db.beginTransaction()
            try {
                result = deleteUnderTransaction(db, uri, selection, selectionArgs)

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            notifyChange()
        }

        return result
    }

    private fun deleteUnderTransaction(db: SQLiteDatabase, uri: Uri, _selection: String?, _selectionArgs: Array<String>?): Int {
        var selection = _selection
        var selectionArgs = _selectionArgs
        val noteId: Long
        val table: String

        when (uris.matcher.match(uri)) {
            ProviderUris.LOCAL_DB_REPO -> table = DbDbRepo.TABLE

            ProviderUris.REPOS -> {
                return DbRepo.delete(db, selection, selectionArgs)
            }

        /* Delete repo by just marking it as such. */
            ProviderUris.REPOS_ID -> {
                selection = DbRepo._ID + " = " + uri.lastPathSegment
                selectionArgs = null

                /* Remove books' links which are using this repo. */
                removeBooksLinksForRepo(db, uri.lastPathSegment)

                /* Delete repo itself. */
                return DbRepo.delete(db, selection, selectionArgs)
            }

            ProviderUris.FILTERS -> table = DbSearch.TABLE

            ProviderUris.FILTERS_ID -> {
                table = DbSearch.TABLE
                selection = DbSearch._ID + " = " + uri.lastPathSegment
                selectionArgs = null
            }

            ProviderUris.BOOKS -> table = DbBook.TABLE

            ProviderUris.BOOKS_ID -> {
                table = DbBook.TABLE
                selection = DbBook._ID + " = " + uri.lastPathSegment
                selectionArgs = null
            }

            ProviderUris.NOTES -> table = DbNote.TABLE

            ProviderUris.CURRENT_ROOKS -> table = DbCurrentVersionedRook.TABLE

            ProviderUris.LINKS_FOR_BOOK -> {
                table = DbBookLink.TABLE
                selection = DbBookLink.BOOK_ID + " = " + java.lang.Long.parseLong(uri.pathSegments[1])
                selectionArgs = null
                return db.delete(table, selection, selectionArgs)
            }

            ProviderUris.NOTES_ID_PROPERTIES -> {
                noteId = java.lang.Long.parseLong(uri.pathSegments[1])

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.query)

                table = DbNoteProperty.TABLE

                selection = DbNoteProperty.NOTE_ID + " = " + noteId
                selectionArgs = null
            }

            else -> throw IllegalArgumentException("URI is not recognized: $uri")
        }

        return db.delete(table, selection, selectionArgs)
    }

    override fun update(uri: Uri, contentValues: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), selection, selectionArgs)

        /* Used by tests. Open a different database so we don't overwrite user's real data. */
        if (uris.matcher.match(uri) == ProviderUris.DB_SWITCH) {
            reopenDatabaseWithDifferentName()
            return 1
        }

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.writableDatabase

        val result: Int

        if (isInBatch) {
            result = updateUnderTransaction(db, uri, contentValues, selection, selectionArgs)

        } else {
            db.beginTransaction()
            try {
                result = updateUnderTransaction(db, uri, contentValues, selection, selectionArgs)

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            notifyChange()
        }

        return result
    }

    /**
     * Re-open database with a different name (for tests), unless already using it.
     */
    private fun reopenDatabaseWithDifferentName() {
        if (DATABASE_NAME_FOR_TESTS != mOpenHelper.databaseName) {
            mOpenHelper.close()
            mOpenHelper = Database(context, DATABASE_NAME_FOR_TESTS)
        }
    }

    private fun updateUnderTransaction(db: SQLiteDatabase, uri: Uri, contentValues: ContentValues?, _selection: String?, _selectionArgs: Array<String>?): Int {
        var selection = _selection
        var selectionArgs = _selectionArgs
        var result: Int
        val noteId: Long
        val match = uris.matcher.match(uri)

        val table: String

        when (match) {
            ProviderUris.LOCAL_DB_REPO -> table = DbDbRepo.TABLE

            ProviderUris.REPOS -> {
                result = DbRepo.update(db, contentValues, selection, selectionArgs)
                return result
            }

            ProviderUris.REPOS_ID -> {
                selection = DbRepo._ID + " = " + uri.lastPathSegment
                selectionArgs = null
                return DbRepo.update(db, contentValues, selection, selectionArgs)
            }

            ProviderUris.FILTERS -> table = DbSearch.TABLE

            ProviderUris.FILTERS_ID -> {
                table = DbSearch.TABLE
                selection = DbSearch._ID + " = " + uri.lastPathSegment
                selectionArgs = null
            }

            ProviderUris.FILTER_UP -> {
                result = ProviderFilters.moveFilterUp(db, java.lang.Long.parseLong(uri.pathSegments[1]))
                return result
            }

            ProviderUris.FILTER_DOWN -> {
                result = ProviderFilters.moveFilterDown(db, java.lang.Long.parseLong(uri.pathSegments[1]))
                return result
            }

            ProviderUris.BOOKS -> table = DbBook.TABLE

            ProviderUris.BOOKS_ID -> {
                table = DbBook.TABLE
                selection = DbBook._ID + " = " + uri.lastPathSegment
                selectionArgs = null
            }

            ProviderUris.NOTES -> {
                table = DbNote.TABLE
                replaceTimestampRangeStringsWithIds(db, contentValues!!)
            }

            ProviderUris.NOTE -> {
                selection = DbNote._ID + " = " + uri.lastPathSegment
                selectionArgs = null

                replaceTimestampRangeStringsWithIds(db, contentValues!!)

                result = db.update(DbNote.TABLE, contentValues, selection, selectionArgs)

                // TODO: Ugh: Use /books/1/notes/23/ or just move to constant
                if (uri.getQueryParameter("bookId") != null) {
                    DatabaseUtils.updateBookMtime(db, java.lang.Long.parseLong(uri.getQueryParameter("bookId")))
                }

                return result
            }

            ProviderUris.NOTES_STATE -> {
                val ids = contentValues!!.getAsString(ProviderContract.NotesState.Param.NOTE_IDS)
                val state = contentValues.getAsString(ProviderContract.NotesState.Param.STATE)

                result = setStateForNotes(db, ids, state)

                return result
            }

            ProviderUris.NOTES_ID_PROPERTIES -> {
                result = 0
                noteId = java.lang.Long.parseLong(uri.pathSegments[1])

                for (propName in contentValues!!.keySet()) {
                    val propValue = contentValues.getAsString(propName)

                    val nameId = DbPropertyName.getOrInsert(db, propName)
                    val valueId = DbPropertyValue.getOrInsert(db, propValue)

                    deleteNoteProperty(db, noteId, nameId)

                    val nextPosition = getLastPropertyPositionForNote(db, noteId) + 1

                    val propertyId = DbProperty.getOrInsert(db, nameId, valueId)
                    DbNoteProperty.getOrInsert(db, noteId, nextPosition, propertyId)

                    result++
                }

                return result
            }

            ProviderUris.CUT -> return ActionRunner.run(db, CutNotesAction(contentValues!!))

            ProviderUris.PASTE -> return ActionRunner.run(db, PasteNotesAction(contentValues!!))

            ProviderUris.DELETE -> return ActionRunner.run(db, DeleteNotesAction(contentValues))

            ProviderUris.PROMOTE -> return ActionRunner.run(db, PromoteNotesAction(contentValues!!))

            ProviderUris.DEMOTE -> return ActionRunner.run(db, DemoteNotesAction(contentValues!!))

            ProviderUris.MOVE -> return ActionRunner.run(db, MoveNotesAction(contentValues!!))

            ProviderUris.NOTE_TOGGLE_FOLDED_STATE -> {
                noteId = java.lang.Long.valueOf(uri.pathSegments[1])
                return ActionRunner.run(db, ToggleFoldedStateAction(noteId))
            }

            ProviderUris.BOOKS_ID_CYCLE_VISIBILITY -> {
                val bookId = java.lang.Long.valueOf(uri.pathSegments[1])
                return ActionRunner.run(db, CycleVisibilityAction(bookId))
            }

            ProviderUris.BOOKS_ID_SPARSE_TREE -> {
                val bookId = java.lang.Long.valueOf(uri.pathSegments[1])
                return ActionRunner.run(db, SparseTreeAction(bookId, contentValues))
            }

            ProviderUris.LINKS_FOR_BOOK -> return updateLinkForBook(db, uri, contentValues!!)

            ProviderUris.DB_RECREATE -> {
                mOpenHelper.reCreateTables(db)

                return 0
            }

            else -> throw IllegalArgumentException("URI is not recognized: $uri")
        }

        // FIXME: Hard to read - some cases above return, some are reaching this
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, table, contentValues, selection, selectionArgs)
        return db.update(table, contentValues, selection, selectionArgs)
    }

    /*
     * Delete note properties with given name ID.
     */
    private fun deleteNoteProperty(db: SQLiteDatabase, noteId: Long, nameId: Long) {
        val sql = ("DELETE FROM " + DbNoteProperty.TABLE
                   + " WHERE " + DbNoteProperty.NOTE_ID + " = " + noteId
                   + " AND " + DbNoteProperty.PROPERTY_ID
                   + " IN (SELECT " + DbProperty._ID + " FROM " + DbProperty.TABLE
                   + " WHERE " + DbProperty.NAME_ID + " = " + nameId + ")")
        db.execSQL(sql)
    }

    private fun getLastPropertyPositionForNote(db: SQLiteDatabase, noteId: Long): Int {
        db.query(
                DbNoteProperty.TABLE,
                arrayOf("MAX(" + DbNoteProperty.POSITION + ")"),
                DbNoteProperty.NOTE_ID + "=" + noteId, null, null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }


    /**
     * @param ids Note ids separated with comma
     * @param targetState keyword
     * @return Number of notes that have been updated
     */
    private fun setStateForNotes(db: SQLiteDatabase, ids: String, targetState: String): Int {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db, ids, targetState)

        val notesUpdated: Int

        /* Select only notes which don't already have the target state. */
        val notesSelection = DbNote._ID + " IN (" + ids + ") AND (" +
                             DbNote.STATE + " IS NULL OR " + DbNote.STATE + " != ?)"

        val selectionArgs = arrayOf(targetState)

        /* Select notebooks which will be affected. */
        val booksSelection = DbBook._ID + " IN (SELECT DISTINCT " +
                             DbNote.BOOK_ID + " FROM " + DbNote.TABLE + " WHERE " + notesSelection + ")"

        /* Notebooks must be updated before notes, because selection checks
         * for notes what will be affected.
         */
        DatabaseUtils.updateBookMtime(db, booksSelection, selectionArgs)

        /* Update notes. */
        notesUpdated = if (AppPreferences.isDoneKeyword(context, targetState)) {
            setDoneStateForNotes(db, targetState, notesSelection, selectionArgs)
        } else {
            setOtherStateForNotes(db, targetState, notesSelection, selectionArgs)
        }

        /* Update modification time. */

        return notesUpdated
    }

    /**
     * Removes CLOSED timestamp and simply sets the state.
     */
    private fun setOtherStateForNotes(db: SQLiteDatabase, targetState: String, notesSelection: String, selectionArgs: Array<String>): Int {
        val values = ContentValues()
        values.put(DbNote.STATE, targetState)
        values.putNull(DbNote.CLOSED_RANGE_ID)

        return db.update(DbNote.TABLE, values, notesSelection, selectionArgs)
    }

    /**
     * If original state is to-do and repeater exists
     * keep the state intact and shift timestamp.
     *
     * If current state is to-do and there is no repeater
     * set the state and keep the timestamp intact.
     */
    private fun setDoneStateForNotes(db: SQLiteDatabase, targetState: String, notesSelection: String, selectionArgs: Array<String>): Int {
        var notesUpdated = 0

        /* Get all notes that don't already have the same state. */
        val cursor = db.query(
                DbNoteBasicView.VIEW_NAME,
                arrayOf(DbNoteBasicView._ID, DbNoteBasicView.SCHEDULED_RANGE_STRING, DbNoteBasicView.DEADLINE_RANGE_STRING, DbNoteBasicView.STATE, DbNoteBasicView.CONTENT),
                notesSelection,
                selectionArgs, null, null, null)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found notes: " + cursor.count)

        cursor.use {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val noteId = cursor.getLong(0)
                val noteScheduled = cursor.getString(1)
                val noteDeadline = cursor.getString(2)
                val noteState = cursor.getString(3)
                var noteContent = cursor.getString(4)

                val stateSetOp = StateChangeLogic(AppPreferences.doneKeywordsSet(context))

                stateSetOp.setState(
                        targetState,
                        noteState,
                        OrgRange.parseOrNull(noteScheduled),
                        OrgRange.parseOrNull(noteDeadline))

                val values = ContentValues()

                if (stateSetOp.state != null) {
                    values.put(ProviderContract.Notes.UpdateParam.STATE, stateSetOp.state)
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.STATE)
                }

                if (stateSetOp.scheduled != null) {
                    values.put(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING, stateSetOp.scheduled.toString())
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)
                }

                if (stateSetOp.deadline != null) {
                    values.put(ProviderContract.Notes.UpdateParam.DEADLINE_STRING, stateSetOp.deadline.toString())
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)
                }

                if (stateSetOp.closed != null) {
                    values.put(ProviderContract.Notes.UpdateParam.CLOSED_STRING, stateSetOp.closed.toString())
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.CLOSED_STRING)
                }

                replaceTimestampRangeStringsWithIds(db, values)

                if (stateSetOp.isShifted) {
                    val time = OrgDateTime(false).toString()

                    if (AppPreferences.setLastRepeatOnTimeShift(context)) {
                        updateOrInsertNoteProperty(db, noteId, OrgFormatter.LAST_REPEAT_PROPERTY, time)
                    }

                    if (AppPreferences.logOnTimeShift(context)) {
                        val stateChangeLine = OrgFormatter.stateChangeLine(noteState, targetState, time)
                        noteContent = OrgFormatter.insertLogbookEntryLine(noteContent, stateChangeLine)
                        values.put(DbNote.CONTENT, noteContent)
                    }
                }

                notesUpdated += db.update(DbNote.TABLE, values, DbNote._ID + "=" + noteId, null)
                cursor.moveToNext()
            }
        }

        return notesUpdated
    }


    private fun updateOrInsertNoteProperty(db: SQLiteDatabase, noteId: Long, name: String, value: String?): Long {
        val nameId = DbPropertyName.getOrInsert(db, name)
        val valueId = DbPropertyValue.getOrInsert(db, value)
        val propertyId = DbProperty.getOrInsert(db, nameId, valueId)

        val cursor = db.query(
                DbNoteProperty.TABLE,
                arrayOf(DbNoteProperty._ID, DbNoteProperty.POSITION),
                DbNoteProperty.NOTE_ID + " = " + noteId + " AND " + DbNoteProperty.PROPERTY_ID + " IN (SELECT " + DbProperty._ID + " FROM " + DbProperty.TABLE + "  WHERE " + DbProperty.NAME_ID + " = " + nameId + ")", null, null, null, null
        )

        val position = cursor.use {
            if (cursor != null && cursor.moveToFirst()) { // Property with the same name already exists
                val notePropertyId = cursor.getLong(0)
                GenericDatabaseUtils.delete(db, DbNoteProperty.TABLE, notePropertyId)

                cursor.getInt(1)

            } else { // New property
                getLastPropertyPositionForNote(db, noteId) + 1
            }
        }

        return DbNoteProperty.getOrInsert(db, noteId, position, propertyId)
    }

    /** User-requested change of link.  */
    private fun updateLinkForBook(db: SQLiteDatabase, uri: Uri, contentValues: ContentValues): Int {
        val bookId = java.lang.Long.parseLong(uri.pathSegments[1])
        val repoUrl = contentValues.getAsString(ProviderContract.BookLinks.Param.REPO_URL)

        if (repoUrl == null) { // Remove link for book
            db.delete(DbBookLink.TABLE, DbBookLink.BOOK_ID + "=" + bookId, null)
        } else {
            updateOrInsertBookLink(db, bookId, repoUrl, null)
        }

        return 1
    }

    private fun updateOrInsertBookLink(db: SQLiteDatabase, bookId: Long, repoUrl: String, rookUrl: String?): Int {
        val repoId = getOrInsertRepo(db, repoUrl)

        val values = ContentValues()
        values.put(DbBookLink.BOOK_ID, bookId)
        values.put(DbBookLink.REPO_ID, repoId)

        if (rookUrl != null) {
            val rookUrlId = DbRookUrl.getOrInsert(db, rookUrl)
            val rookId = getOrInsertRook(db, rookUrlId, repoId)
            values.put(DbBookLink.ROOK_ID, rookId)
        } else {
            values.putNull(DbBookLink.ROOK_ID)
        }

        val id = DatabaseUtils.getId(db, DbBookLink.TABLE, DbBookLink.BOOK_ID + "=" + bookId, null)
        if (id != 0L) {
            db.update(DbBookLink.TABLE, values, DbBookLink._ID + "=" + id, null)
        } else {
            db.insertOrThrow(DbBookLink.TABLE, null, values)
        }

        return 1
    }

    private fun removeBooksLinksForRepo(db: SQLiteDatabase, repoId: String): Int {
        /* Rooks which use passed repo. */
        val rookIds = "SELECT DISTINCT " + DbRook._ID +
                      " FROM " + DbRook.TABLE +
                      " WHERE " + DbRook.REPO_ID + " = " + repoId

        return db.delete(DbBookLink.TABLE, DbBookLink.ROOK_ID + " IN (" + rookIds + ") OR "
                                           + DbBookLink.REPO_ID + " = " + repoId, null)
    }

    private fun updateOrInsertBookSync(
            db: SQLiteDatabase,
            bookId: Long,
            repoUrl: String,
            rookUrl: String,
            revision: String,
            mtime: Long): Int {

        val repoUrlId = getOrInsertRepo(db, repoUrl)
        val rookUrlId = DbRookUrl.getOrInsert(db, rookUrl)
        val rookId = getOrInsertRook(db, rookUrlId, repoUrlId)
        val rookRevisionId = getOrInsertVersionedRook(db, rookId, revision, mtime)

        val values = ContentValues()
        values.put(DbBookSync.BOOK_VERSIONED_ROOK_ID, rookRevisionId)
        values.put(DbBookSync.BOOK_ID, bookId)

        val id = DatabaseUtils.getId(db, DbBookSync.TABLE, DbBookSync.BOOK_ID + "=" + bookId, null)
        if (id != 0L) {
            db.update(DbBookSync.TABLE, values, DbBookSync._ID + "=" + id, null)
        } else {
            db.insertOrThrow(DbBookSync.TABLE, null, values)
        }

        return 1
    }

    private fun getOrInsertBook(db: SQLiteDatabase, name: String): Uri {
        val cursor = db.query(
                DbBook.TABLE,
                arrayOf(DbBook._ID),
                DbBook.NAME + "=?",
                arrayOf(name), null, null, null)

        cursor.use {
            if (cursor.moveToFirst()) {
                return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), cursor.getLong(0))
            }
        }

        /* No such book, create it. */

        val values = ContentValues()
        values.put(DbBook.NAME, name)

        val id = db.insertOrThrow(DbBook.TABLE, null, values)

        return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), id)
    }

    private fun loadBookFromFile(values: ContentValues): Uri {
        val bookName = values.getAsString(ProviderContract.LoadBookFromFile.Param.BOOK_NAME)
        // val format = values.getAsString(ProviderContract.LoadBookFromFile.Param.FORMAT)
        val filePath = values.getAsString(ProviderContract.LoadBookFromFile.Param.FILE_PATH)
        val repoUrl = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_REPO_URL)
        val rookUrl = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_URL)
        val rookRevision = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_REVISION)
        val rookMtime = if (values.containsKey(ProviderContract.LoadBookFromFile.Param.ROOK_MTIME)) values.getAsLong(ProviderContract.LoadBookFromFile.Param.ROOK_MTIME) else 0
        val selectedEncoding = values.getAsString(ProviderContract.LoadBookFromFile.Param.SELECTED_ENCODING)

        try {
            /*
             * Determine encoding to use -- detect or force it.
             */
            val usedEncoding: String
            var detectedEncoding: String? = null

            if (selectedEncoding == null) {
                detectedEncoding = EncodingDetect.getInstance(FileInputStream(File(filePath))).encoding
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Detected encoding: $detectedEncoding")

                /* Can't detect encoding - use default. */
                if (detectedEncoding == null) {
                    usedEncoding = "UTF-8"
                    Log.w(TAG, "Encoding for $bookName could not be detected, using UTF-8")
                } else {
                    usedEncoding = detectedEncoding
                }

            } else {
                usedEncoding = selectedEncoding
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using selected encoding: $usedEncoding")
            }

            return loadBookFromReader(
                    bookName,
                    repoUrl,
                    rookUrl,
                    rookRevision,
                    rookMtime,
                    InputStreamReader(FileInputStream(File(filePath)), usedEncoding),
                    usedEncoding,
                    detectedEncoding,
                    selectedEncoding
            )

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
            repoUrl: String?,
            rookUrl: String?,
            rookRevision: String?,
            rookMtime: Long,
            inReader: Reader,
            usedEncoding: String,
            detectedEncoding: String?,
            selectedEncoding: String?): Uri {

        val startedAt = System.currentTimeMillis()

        val uri: Uri

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        val db = mOpenHelper.writableDatabase

        /* Create book if it doesn't already exist. */
        uri = getOrInsertBook(db, bookName)

        val bookId = ContentUris.parseId(uri)

        /* Delete all notes from book. TODO: Delete all other references to this book ID */
        db.delete(DbNote.TABLE, DbNote.BOOK_ID + "=" + bookId, null)

        val propNameDbIds = HashMap<String, Long>()
        run {
            val cursor = db.query(DbPropertyName.TABLE, arrayOf(DbPropertyName._ID, DbPropertyName.NAME), null, null, null, null, null)
            cursor.use {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    propNameDbIds[cursor.getString(1)] = cursor.getLong(0)
                    cursor.moveToNext()
                }
            }
        }

        val propValueDbIds = HashMap<String, Long>()
        run {
            val cursor = db.query(DbPropertyValue.TABLE, arrayOf(DbPropertyValue._ID, DbPropertyValue.VALUE), null, null, null, null, null)
            cursor.use {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    propValueDbIds[cursor.getString(1)] = cursor.getLong(0)
                    cursor.moveToNext()
                }
            }
        }

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
                            BookSizeValidator.validate(node)

                            /* Insert note with book id at specific position. */

                            val position = NotePosition()
                            position.bookId = bookId
                            position.lft = node.lft
                            position.rgt = node.rgt
                            position.level = node.level
                            position.descendantsCount = node.descendantsCount

                            var values = ContentValues()

                            // Update ContentValues
                            DbNote.toContentValues(values, position)
                            if (useCreatedAtProperty) {
                                DbNote.toContentValues(
                                        values, node.head.properties, createdAtProperty)
                            }
                            DbNote.toContentValues(db, values, node.head)

                            val noteId = db.insertOrThrow(DbNote.TABLE, null, values)

                            /* Insert note's properties. */
                            var pos = 1
                            node.head.properties.forEach { (propName, propValue) ->
                                var nameId: Long? = propNameDbIds[propName]
                                if (nameId == null) {
                                    nameId = DbPropertyName.getOrInsert(db, propName)
                                    propNameDbIds[propName] = nameId
                                }

                                var valueId: Long? = propValueDbIds[propValue]
                                if (valueId == null) {
                                    valueId = DbPropertyValue.getOrInsert(db, propValue)
                                    propValueDbIds[propValue] = valueId
                                }

                                val propertyId = DbProperty.getOrInsert(db, nameId, valueId)

                                DbNoteProperty.getOrInsert(db, noteId, pos++, propertyId)
                            }

                            /*
                             * Update parent ID and insert ancestors.
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
                                        values = ContentValues()
                                        values.put(DbNote.PARENT_ID, noteId)
                                        db.update(DbNote.TABLE, values, DbNote._ID + " = " + descendantId, null)
                                        notesWithParentSet.add(descendantId)
                                    }

                                    values = ContentValues()
                                    values.put(DbNoteAncestor.NOTE_ID, descendantId)
                                    values.put(DbNoteAncestor.ANCESTOR_NOTE_ID, noteId)
                                    values.put(DbNoteAncestor.BOOK_ID, bookId)
                                    db.insert(DbNoteAncestor.TABLE, null, values)
                                }
                            }
                        }

                        @Throws(IOException::class)
                        override fun onFile(file: OrgFile) {
                            BookSizeValidator.validate(file)

                            val values = ContentValues()

                            BooksClient.toContentValues(values, file.settings)

                            /* Set preface. TODO: Move to and rename OrgFileSettings */
                            values.put(DbBook.PREFACE, file.preface)

                            values.put(DbBook.USED_ENCODING, usedEncoding)
                            values.put(DbBook.DETECTED_ENCODING, detectedEncoding)
                            values.put(DbBook.SELECTED_ENCODING, selectedEncoding)

                            db.update(DbBook.TABLE, values, DbBook._ID + "=" + bookId, null)
                        }

                    })
                    .build()
                    .parse()
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, bookName + ": Parsing done in " +
                            (System.currentTimeMillis() - startedAt) + " ms")

        if (repoUrl != null && rookUrl != null && rookRevision != null) {
            updateOrInsertBookLink(db, bookId, repoUrl, rookUrl)
            updateOrInsertBookSync(db, bookId, repoUrl, rookUrl, rookRevision, rookMtime)
        }

        /* Mark book as complete. */
        val values = ContentValues()
        values.put(DbBook.IS_DUMMY, 0)
        db.update(DbBook.TABLE, values, DbBook._ID + "=" + bookId, null)

        return uri
    }

    private fun replaceTimestampRangeStringsWithIds(db: SQLiteDatabase, values: ContentValues) {

        if (values.containsKey(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)) {
            val str = values.getAsString(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)
            if (!TextUtils.isEmpty(str)) {
                values.put(DbNote.SCHEDULED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)))
            } else {
                values.putNull(DbNote.SCHEDULED_RANGE_ID)
            }

            values.remove(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)) {
            val str = values.getAsString(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)
            if (!TextUtils.isEmpty(str)) {
                values.put(DbNote.DEADLINE_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)))
            } else {
                values.putNull(DbNote.DEADLINE_RANGE_ID)
            }

            values.remove(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOSED_STRING)) {
            val str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOSED_STRING)
            if (!TextUtils.isEmpty(str)) {
                values.put(DbNote.CLOSED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)))
            } else {
                values.putNull(DbNote.CLOSED_RANGE_ID)
            }

            values.remove(ProviderContract.Notes.UpdateParam.CLOSED_STRING)
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOCK_STRING)) {
            val str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOCK_STRING)
            if (!TextUtils.isEmpty(str)) {
                values.put(DbNote.CLOCK_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)))
            } else {
                values.putNull(DbNote.CLOCK_RANGE_ID)
            }

            values.remove(ProviderContract.Notes.UpdateParam.CLOCK_STRING)
        }
    }

    /**
     * Gets [OrgDateTime] from database or inserts a new record if it doesn't exist.
     * @return [OrgDateTime] database ID
     */
    private fun getOrInsertOrgRange(db: SQLiteDatabase, range: OrgRange): Long {
        var id = DatabaseUtils.getId(
                db,
                DbOrgRange.TABLE,
                DbOrgRange.STRING + "=?",
                arrayOf(range.toString()))

        if (id == 0L) {
            val values = ContentValues()

            val startTimestampId = getOrInsertOrgTime(db, range.startTime)

            var endTimestampId: Long = 0
            if (range.endTime != null) {
                endTimestampId = getOrInsertOrgTime(db, range.endTime)
            }

            DbOrgRange.toContentValues(values, range, startTimestampId, endTimestampId)

            id = db.insertOrThrow(DbOrgRange.TABLE, null, values)
        }

        return id
    }

    private fun getOrInsertOrgTime(db: SQLiteDatabase, orgDateTime: OrgDateTime): Long {
        var id = DatabaseUtils.getId(
                db,
                DbOrgTimestamp.TABLE,
                DbOrgTimestamp.STRING + "= ?",
                arrayOf(orgDateTime.toString()))

        if (id == 0L) {
            val values = ContentValues()
            DbOrgTimestamp.toContentValues(values, orgDateTime)

            id = db.insertOrThrow(DbOrgTimestamp.TABLE, null, values)
        }

        return id
    }

    private fun notifyChange() {
        val uri = ProviderContract.AUTHORITY_URI

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString())
        context.contentResolver.notifyChange(uri, null)
    }

    /**
     * Don't allow books which have huge fields.  Storing is fine, but Cursor has limits:
     * Couldn't read row 0, col 0 from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.
     * See config_cursorWindowSize.
     *
     * TODO: Also validate before every write to database, not just when parsing fields.
     * User can paste huge content to a note directly.
     */
    object BookSizeValidator {
        // TODO: This now works with lion-wide.org (test asset) - how to decide which size to use?
        private const val BOOK_PREFACE_LIMIT = 1000000
        private const val NOTE_TOTAL_SIZE_LIMIT = 1500000

        @Throws(IOException::class)
        fun validate(agenda: OrgFile) {
            if (agenda.preface.length > BOOK_PREFACE_LIMIT) {
                throw IOException("Notebook content is too big (" + agenda.preface.length + " chars " + BOOK_PREFACE_LIMIT + " max)")
            }
        }

        @Throws(IOException::class)
        fun validate(node: OrgNode) {
            /*
             * Assume indent (more space occupied).
             * This probably slows down the import a lot.
             */
            val parserWriter = OrgParserWriter()
            val totalSize = parserWriter.whiteSpacedHead(node, true).length

            if (totalSize > NOTE_TOTAL_SIZE_LIMIT) {
                throw IOException("Note total size is too big ($totalSize chars $NOTE_TOTAL_SIZE_LIMIT max)")
            }
        }
    }

    companion object {
        private val TAG = Provider::class.java.name

        var DATABASE_NAME = "orgzly.db"
        var DATABASE_NAME_FOR_TESTS = "orgzly_test.db"

        private const val VERSIONED_ROOK_IDS_FOR_ROOK_ID =
                "SELECT _id FROM " + DbVersionedRook.TABLE +
                " WHERE " + DbVersionedRook.ROOK_ID + "=?"

        private const val DELETE_CURRENT_VERSIONED_ROOKS_FOR_ROOK_ID =
                "DELETE FROM " + DbCurrentVersionedRook.TABLE +
                " WHERE " + DbCurrentVersionedRook.VERSIONED_ROOK_ID +
                " IN (" + VERSIONED_ROOK_IDS_FOR_ROOK_ID + ")"
    }
}

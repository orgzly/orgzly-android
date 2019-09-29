package com.orgzly.android.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.orgzly.android.App
import com.orgzly.android.db.mappers.OrgTimestampMapper
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.MiscUtils
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.parser.OrgNestedSetParser
import java.util.*
import java.util.regex.Pattern

object PreRoomMigration {
    val MIGRATION_130_131 = object : Migration(130, 131) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE books ADD COLUMN title") // TITLE
        }
    }

    val MIGRATION_131_132 = object : Migration(131, 132) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE books ADD COLUMN is_indented INTEGER DEFAULT 0") // IS_INDENTED
            db.execSQL("ALTER TABLE books ADD COLUMN used_encoding TEXT") // USED_ENCODING
            db.execSQL("ALTER TABLE books ADD COLUMN detected_encoding TEXT") // DETECTED_ENCODING
            db.execSQL("ALTER TABLE books ADD COLUMN selected_encoding TEXT") // SELECTED_ENCODING
        }
    }

    val MIGRATION_132_133 = object : Migration(132, 133) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_133_134 = object : Migration(133, 134) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notes ADD COLUMN parent_id") // PARENT_ID

            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_visible ON notes(is_visible)") // LFT
            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_parent_position ON notes(parent_position)") // RGT
            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_collapsed ON notes(is_collapsed)") // IS_FOLDED
            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_under_collapsed ON notes(is_under_collapsed)") // FOLDED_UNDER_ID
            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_parent_id ON notes(parent_id)") // PARENT_ID
            db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_has_children ON notes(has_children)") // DESCENDANTS_COUNT

            convertNotebooksFromPositionToNestedSet(db)
        }
    }

    private fun convertNotebooksFromPositionToNestedSet(db: SupportSQLiteDatabase) {
        val query = SupportSQLiteQueryBuilder
                .builder("books")
                .columns(arrayOf("_id"))
                .create()

        eachForQuery(db, query) { cursor ->
            val bookId = cursor.getLong(0)
            convertNotebookFromPositionToNestedSet(db, bookId)
            updateParentIds(db, bookId)
        }
    }

    private fun convertNotebookFromPositionToNestedSet(db: SupportSQLiteDatabase, bookId: Long) {
        /* Insert root note. */
        val rootNoteValues = ContentValues()
        rootNoteValues.put("level", 0)
        rootNoteValues.put("book_id", bookId)
        rootNoteValues.put("position", 0)
        db.insert("notes", SQLiteDatabase.CONFLICT_ROLLBACK, rootNoteValues)

        updateNotesPositionsFromLevel(db, bookId)
    }

    private fun updateNotesPositionsFromLevel(db: SupportSQLiteDatabase, bookId: Long) {
        val stack = Stack<NotePosition>()
        var prevLevel = -1
        var sequence = OrgNestedSetParser.STARTING_VALUE - 1

        val query = SupportSQLiteQueryBuilder
                .builder("notes")
                .selection("book_id = $bookId", null)
                .orderBy("position")
                .create()

        eachForQuery(db, query) { cursor ->
            val note = noteFromCursor(cursor)

            if (prevLevel < note.level) {
                /*
                 * This is a descendant of previous thisNode.
                 */

                /* Put the current thisNode on the stack. */
                sequence += 1
                note.lft = sequence
                stack.push(note)

            } else if (prevLevel == note.level) {
                /*
                 * This is a sibling, which means that the last thisNode visited can be completed.
                 * Take it off the stack, update its rgt value and announce it.
                 */
                val nodeFromStack = stack.pop()
                sequence += 1
                nodeFromStack.rgt = sequence
                calculateAndSetDescendantsCount(nodeFromStack)
                updateNotePositionValues(db, nodeFromStack)

                /* Put the current thisNode on the stack. */
                sequence += 1
                note.lft = sequence
                stack.push(note)

            } else {
                /*
                 * Note has lower level then the previous one - we're out of the set.
                 * Start popping the stack, up to and including the thisNode with the same level.
                 */
                while (!stack.empty()) {
                    val nodeFromStack = stack.peek()

                    if (nodeFromStack.level >= note.level) {
                        stack.pop()

                        sequence += 1
                        nodeFromStack.rgt = sequence
                        calculateAndSetDescendantsCount(nodeFromStack)
                        updateNotePositionValues(db, nodeFromStack)

                    } else {
                        break
                    }
                }

                /* Put the current thisNode on the stack. */
                sequence += 1
                note.lft = sequence
                stack.push(note)
            }

            prevLevel = note.level
        }

        /* Pop remaining nodes. */
        while (!stack.empty()) {
            val nodeFromStack = stack.pop()
            sequence += 1
            nodeFromStack.rgt = sequence
            calculateAndSetDescendantsCount(nodeFromStack)
            updateNotePositionValues(db, nodeFromStack)
        }
    }

    private fun updateNotePositionValues(db: SupportSQLiteDatabase, note: NotePosition): Int {
        val values = contentValuesFromNote(note)

        return db.update("notes", SQLiteDatabase.CONFLICT_ROLLBACK, values, "_id = ${note.id}", null)
    }

    private fun calculateAndSetDescendantsCount(node: NotePosition) {
        node.descendantsCount = (node.rgt - node.lft - 1).toInt() / (2 * 1)
    }

    private fun updateParentIds(db: SupportSQLiteDatabase, bookId: Long) {
        val parentId = "(SELECT _id FROM notes AS n WHERE " +
                "book_id = " + bookId + " AND " +
                "n.is_visible < notes.is_visible AND " +
                "notes.parent_position < n.parent_position ORDER BY n.is_visible DESC LIMIT 1)"

        db.execSQL("UPDATE notes SET parent_id = " + parentId +
                " WHERE book_id = " + bookId + " AND is_cut = 0 AND level > 0")
    }

    private fun noteFromCursor(cursor: Cursor): NotePosition {
        val position = NotePosition()

        position.id = cursor.getLong(cursor.getColumnIndex("_id"))
        position.bookId = cursor.getLong(cursor.getColumnIndex("book_id"))
        position.level = cursor.getInt(cursor.getColumnIndex("level"))
        position.lft = cursor.getLong(cursor.getColumnIndex("is_visible"))
        position.rgt = cursor.getLong(cursor.getColumnIndex("parent_position"))
        position.descendantsCount = cursor.getInt(cursor.getColumnIndex("has_children"))
        position.foldedUnderId = cursor.getLong(cursor.getColumnIndex("is_under_collapsed"))
        position.parentId = cursor.getLong(cursor.getColumnIndex("parent_id"))
        position.isFolded = cursor.getInt(cursor.getColumnIndex("is_collapsed")) != 0

        return position
    }

    private fun contentValuesFromNote(position: NotePosition): ContentValues {
        val values = ContentValues()

        // values.put("_id", position.id)
        values.put("book_id", position.bookId)
        values.put("level", position.level)
        values.put("is_visible", position.lft)
        values.put("parent_position", position.rgt)
        values.put("has_children", position.descendantsCount)
        values.put("is_under_collapsed", position.foldedUnderId)
        values.put("parent_id", position.parentId)
        values.put("is_collapsed", if (position.isFolded) 1 else 0)
        values.put("position", 0)

        return values
    }

    private class NotePosition {
        var id: Long = 0

        var bookId: Long = 0

        var lft: Long = 0
        var rgt: Long = 0

        var level: Int = 0

        var descendantsCount: Int = 0

        var foldedUnderId: Long = 0

        var parentId: Long = 0

        var isFolded: Boolean = false
    }

    val MIGRATION_134_135 = object : Migration(134, 135) {
        override fun migrate(db: SupportSQLiteDatabase) {
            fixOrgRanges(db)
        }
    }

    /**
     * 1.4-beta.1 misused insertWithOnConflict due to
     * https://code.google.com/p/android/issues/detail?id=13045.
     *
     * org_ranges ended up with duplicates having -1 for
     * start_timestamp_id and end_timestamp_id.
     *
     * Delete those, update notes tables and add a unique constraint to the org_ranges.
     */
    private fun fixOrgRanges(db: SupportSQLiteDatabase) {
        val notesFields = arrayOf("scheduled_range_id", "deadline_range_id", "closed_range_id", "clock_range_id")

        val query = SupportSQLiteQueryBuilder
                .builder("org_ranges")
                .columns(arrayOf("_id", "string"))
                .selection("start_timestamp_id = -1 OR end_timestamp_id = -1", null)
                .create()

        eachForQuery(db, query) { cursor ->
            val id = cursor.getLong(0)
            val string = cursor.getString(1)

            val validTimestampId = "(start_timestamp_id IS NULL OR start_timestamp_id != -1) AND (end_timestamp_id IS NULL OR end_timestamp_id != -1)"
            val validEntry = "(SELECT _id FROM org_ranges WHERE string = " + android.database.DatabaseUtils.sqlEscapeString(string) + " and " + validTimestampId + ")"

            for (field in notesFields) {
                db.execSQL("UPDATE notes SET $field = $validEntry WHERE $field = $id")
            }
        }

        db.execSQL("DELETE FROM org_ranges WHERE start_timestamp_id = -1 OR end_timestamp_id = -1")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS i_org_ranges_string ON org_ranges(string)")
    }

    /* Properties moved from content. */
    val MIGRATION_135_136 = object : Migration(135, 136) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS note_properties (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "note_id INTEGER," +
                    "position INTEGER," +
                    "property_id INTEGER," +
                    "UNIQUE(note_id, position, property_id))")

            db.execSQL("CREATE TABLE IF NOT EXISTS property_names (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE)")

            db.execSQL("CREATE TABLE IF NOT EXISTS property_values (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "value TEXT UNIQUE)")

            db.execSQL("CREATE TABLE IF NOT EXISTS properties (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name_id INTEGER," +
                    "value_id INTEGER," +
                    "UNIQUE(name_id, value_id))")

            movePropertiesFromBody(db)
        }
    }

    private fun movePropertiesFromBody(db: SupportSQLiteDatabase) {
        val query = SupportSQLiteQueryBuilder
                .builder("notes")
                .columns(arrayOf("_id", "content"))
                .create()

        val contentUpdates = hashMapOf<Long, ContentValues>()

        eachForQuery(db, query) { cursor ->
            val noteId = cursor.getLong(0)
            val content = cursor.getString(1)

            if (!TextUtils.isEmpty(content)) {
                val newContent = StringBuilder()
                val properties = getPropertiesFromContent(content, newContent)

                if (properties.isNotEmpty()) {
                    var pos = 1
                    for (property in properties) {
                        val nameId = getOrInsert(
                                db, "property_names", arrayOf("name"), arrayOf(property[0]))
                        val valueId = getOrInsert(
                                db, "property_values", arrayOf("value"), arrayOf(property[1]))

                        val propertyId = getOrInsert(
                                db,
                                "properties",
                                arrayOf("name_id", "value_id"),
                                arrayOf(nameId.toString(), valueId.toString()))

                        getOrInsert(
                                db,
                                "note_properties",
                                arrayOf("note_id", "position", "property_id"),
                                arrayOf(noteId.toString(), pos++.toString(), propertyId.toString()))
                    }

                    // New content and its line count
                    contentUpdates[noteId] = ContentValues().apply {
                        put("content", newContent.toString())
                        put("content_line_count", MiscUtils.lineCount(newContent.toString()))
                    }
                }
            }
        }

        // This was causing issues (skipped notes) when done inside eachForQuery loop
        for ((id, values) in contentUpdates) {
            db.update("notes", SQLiteDatabase.CONFLICT_ROLLBACK, values, "_id = $id", null)
        }
    }

    private fun getOrInsert(
            db: SupportSQLiteDatabase,
            table: String,
            fieldNames: Array<String>,
            fieldValues: Array<String>): Long {

        val selection = fieldNames.joinToString(" AND ") { "$it = ?" }

        val query = SupportSQLiteQueryBuilder
                .builder(table)
                .columns(arrayOf("_id"))
                .selection(selection, fieldValues)
                .create()

        val id = db.query(query).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                0
            }
        }

        return if (id > 0) {
            id

        } else {
            val values = ContentValues()

            fieldNames.forEachIndexed { index, field ->
                values.put(field, fieldValues[index])
            }

            db.insert(table, SQLiteDatabase.CONFLICT_ROLLBACK, values)
        }
    }

    private fun getPropertiesFromContent(content: String, newContent: StringBuilder): List<Array<String>> {
        val properties = ArrayList<Array<String>>()

        val propertiesPattern = Pattern.compile("^\\s*:PROPERTIES:(.*?):END: *\n*(.*)", Pattern.DOTALL)
        val propertyPattern = Pattern.compile("^:([^:\\s]+):\\s+(.*)\\s*$")

        val m = propertiesPattern.matcher(content)

        if (m.find()) {
            val propertyLines = m.group(1)?.split("\n") ?: emptyList()

            for (propertyLine in propertyLines) {
                val pm = propertyPattern.matcher(propertyLine.trim())

                if (pm.find()) {
                    val name = pm.group(1)
                    val value = pm.group(2)

                    // Add name-value pair
                    if (name != null && value != null) {
                        properties.add(arrayOf(name, value))
                    }
                }
            }

            newContent.append(m.group(2))
        }

        return properties
    }

    val MIGRATION_136_137 = object : Migration(136, 137) {
        override fun migrate(db: SupportSQLiteDatabase) {
            encodeRookUris(db)
        }
    }

    /**
     * file:/dir/file name.org
     * file:/dir/file%20name.org
     */
    private fun encodeRookUris(db: SupportSQLiteDatabase) {
        val query = SupportSQLiteQueryBuilder
                .builder("rook_urls")
                .columns(arrayOf("_id", "rook_url"))
                .create()

        eachForQuery(db, query) { cursor ->
            val id = cursor.getLong(0)
            val uri = cursor.getString(1)

            val encodedUri = MiscUtils.encodeUri(uri)

            if (uri != encodedUri) {
                encodeRookUri(db, id, encodedUri)
            }
        }
    }

    /* Update unless same URL already exists. */
    private fun encodeRookUri(db: SupportSQLiteDatabase, id: Long, encodedUri: String?) {
        val query = SupportSQLiteQueryBuilder
                .builder("rook_urls")
                .columns(arrayOf("_id"))
                .selection("rook_url = ?", arrayOf(encodedUri))
                .create()

        if (!db.query(query).moveToFirst()) {
            val values = ContentValues()
            values.put("rook_url", encodedUri)
            db.update("rook_urls", SQLiteDatabase.CONFLICT_ROLLBACK, values, "_id = $id", null)
        }
    }

    val MIGRATION_137_138 = object : Migration(137, 138) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                    CREATE TABLE IF NOT EXISTS note_ancestors (
                    book_id INTEGER,
                    note_id INTEGER,
                    ancestor_note_id INTEGER)""".trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS i_note_ancestors_" + "book_id" + " ON note_ancestors(" + "book_id" + ")")
            db.execSQL("CREATE INDEX IF NOT EXISTS i_note_ancestors_" + "note_id" + " ON note_ancestors(" + "note_id" + ")")
            db.execSQL("CREATE INDEX IF NOT EXISTS i_note_ancestors_" + "ancestor_note_id" + " ON note_ancestors(" + "ancestor_note_id" + ")")

            db.execSQL("INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id) " +
                    "SELECT n.book_id, n._id, a._id FROM notes n " +
                    "JOIN notes a on (n.book_id = a.book_id AND a.is_visible < n.is_visible AND n.parent_position < a.parent_position) " +
                    "WHERE a.level > 0")
        }
    }

    val MIGRATION_138_139 = object : Migration(138, 139) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateOrgTimestamps(db)
        }
    }

    private fun migrateOrgTimestamps(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE org_timestamps RENAME TO org_timestamps_prev")

        db.execSQL("""
                CREATE TABLE IF NOT EXISTS org_timestamps (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                string TEXT NOT NULL UNIQUE,
                is_active INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER,
                minute INTEGER,
                second INTEGER,
                end_hour INTEGER,
                end_minute INTEGER,
                end_second INTEGER,
                repeater_type INTEGER,
                repeater_value INTEGER,
                repeater_unit INTEGER,
                habit_deadline_value INTEGER,
                habit_deadline_unit INTEGER,
                delay_type INTEGER,
                delay_value INTEGER,
                delay_unit INTEGER,
                timestamp INTEGER,
                end_timestamp INTEGER)""".trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS i_org_timestamps_string ON org_timestamps(string)")
        db.execSQL("CREATE INDEX IF NOT EXISTS i_org_timestamps_timestamp ON org_timestamps(timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS i_org_timestamps_end_timestamp ON org_timestamps(end_timestamp)")

        val query = SupportSQLiteQueryBuilder
                .builder("org_timestamps_prev")
                .columns(arrayOf("_id", "string"))
                .create()

        eachForQuery(db, query) { cursor ->
            val id = cursor.getLong(0)
            val string = cursor.getString(1)

            val orgDateTime = OrgDateTime.parse(string)

            val values = ContentValues()
            values.put("_id", id)
            toContentValues(values, orgDateTime)

            db.insert("org_timestamps", SQLiteDatabase.CONFLICT_ROLLBACK, values)
        }

        db.execSQL("DROP TABLE org_timestamps_prev")
    }

    private fun toContentValues(values: ContentValues, orgDateTime: OrgDateTime) {
        values.put("string", orgDateTime.toString())

        values.put("is_active", if (orgDateTime.isActive) 1 else 0)

        values.put("year", orgDateTime.calendar.get(Calendar.YEAR))
        values.put("month", orgDateTime.calendar.get(Calendar.MONTH) + 1)
        values.put("day", orgDateTime.calendar.get(Calendar.DAY_OF_MONTH))

        if (orgDateTime.hasTime()) {
            values.put("hour", orgDateTime.calendar.get(Calendar.HOUR_OF_DAY))
            values.put("minute", orgDateTime.calendar.get(Calendar.MINUTE))
            values.put("second", orgDateTime.calendar.get(Calendar.SECOND))
        } else {
            values.putNull("hour")
            values.putNull("minute")
            values.putNull("second")
        }

        values.put("timestamp", orgDateTime.calendar.timeInMillis)

        if (orgDateTime.hasEndTime()) {
            values.put("end_hour", orgDateTime.endCalendar.get(Calendar.HOUR_OF_DAY))
            values.put("end_minute", orgDateTime.endCalendar.get(Calendar.MINUTE))
            values.put("end_second", orgDateTime.endCalendar.get(Calendar.SECOND))
            values.put("end_timestamp", orgDateTime.endCalendar.timeInMillis)
        } else {
            values.putNull("end_hour")
            values.putNull("end_minute")
            values.putNull("end_second")
            values.putNull("end_timestamp")
        }

        if (orgDateTime.hasRepeater()) {
            values.put("repeater_type", OrgTimestampMapper.repeaterType(orgDateTime.repeater.type))
            values.put("repeater_value", orgDateTime.repeater.value)
            values.put("repeater_unit", OrgTimestampMapper.timeUnit(orgDateTime.repeater.unit))

            if (orgDateTime.repeater.hasHabitDeadline()) {
                values.put("habit_deadline_value", orgDateTime.repeater.habitDeadline.value)
                values.put("habit_deadline_unit", OrgTimestampMapper.timeUnit(orgDateTime.repeater.habitDeadline.unit))
            } else {
                values.putNull("habit_deadline_value")
                values.putNull("habit_deadline_unit")
            }

        } else {
            values.putNull("repeater_type")
            values.putNull("repeater_value")
            values.putNull("repeater_unit")
            values.putNull("habit_deadline_value")
            values.putNull("habit_deadline_unit")
        }

        if (orgDateTime.hasDelay()) {
            values.put("delay_type", OrgTimestampMapper.delayType(orgDateTime.delay.type))
            values.put("delay_value", orgDateTime.delay.value)
            values.put("delay_unit", OrgTimestampMapper.timeUnit(orgDateTime.delay.unit))
        } else {
            values.putNull("delay_type")
            values.putNull("delay_value")
            values.putNull("delay_unit")
        }
    }

    val MIGRATION_139_140 = object : Migration(139, 140) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_140_141 = object : Migration(140, 141) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_141_142 = object : Migration(141, 142) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_142_143 = object : Migration(142, 143) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_143_144 = object : Migration(143, 144) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_144_145 = object : Migration(144, 145) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_145_146 = object : Migration(145, 146) {
        override fun migrate(db: SupportSQLiteDatabase) {
            insertAgendaSavedSearch(db)
        }
    }

    val MIGRATION_146_147 = object : Migration(146, 147) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addAndSetCreatedAt(db, App.getAppContext())
        }
    }

    val MIGRATION_147_148 = object : Migration(147, 148) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Views-only updates
        }
    }

    val MIGRATION_148_149 = object : Migration(148, 149) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE book_links ADD COLUMN repo_id INTEGER")
            db.execSQL("UPDATE book_links SET repo_id = (SELECT repo_id FROM rooks WHERE rooks._id = rook_id)")
        }
    }

    private fun addAndSetCreatedAt(db: SupportSQLiteDatabase, context: Context) {
        db.execSQL("ALTER TABLE notes ADD COLUMN created_at INTEGER DEFAULT 0")

        if (!AppPreferences.createdAt(context)) {
            return
        }

        val createdAtPropName = AppPreferences.createdAtProperty(context)

        val query = SupportSQLiteQueryBuilder
                .builder("note_properties"
                        + " JOIN notes ON (notes._id = note_properties.note_id)"
                        + " JOIN properties ON (properties._id = note_properties.property_id)"
                        + " JOIN property_names ON (property_names._id = properties.name_id)"
                        + " JOIN property_values ON (property_values._id = properties.value_id)")
                .columns(arrayOf("notes._id AS note_id", "property_values.value AS value"))
                .selection("note_id IS NOT NULL AND name IS NOT NULL AND value IS NOT NULL AND name = ?", arrayOf(createdAtPropName))
                .distinct()
                .create()

        eachForQuery(db, query) { cursor ->
            val noteId = cursor.getLong(0)
            val propValue = cursor.getString(1)

            val dateTime = OrgDateTime.doParse(propValue)

            if (dateTime != null) {
                val millis = dateTime.calendar.timeInMillis
                val values = ContentValues()
                values.put("created_at", millis)
                db.update("notes", SQLiteDatabase.CONFLICT_ROLLBACK, values, "_id = $noteId", null)
            }
        }
    }

    private fun insertAgendaSavedSearch(db: SupportSQLiteDatabase) {
        val values = ContentValues()

        values.put("name", "Agenda")
        values.put("search", ".it.done ad.7")
        values.put("position", -2) // Display first

        db.insert("searches", SQLiteDatabase.CONFLICT_ROLLBACK, values)

        values.put("name", "Next 3 days")
        values.put("search", ".it.done s.ge.today ad.3")
        values.put("position", -1) // Display second

        db.insert("searches", SQLiteDatabase.CONFLICT_ROLLBACK, values)
    }

    private fun eachForQuery(db: SupportSQLiteDatabase, query: SupportSQLiteQuery, action: (Cursor) -> Unit) {
        db.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    action(cursor)
                    cursor.moveToNext()
                }
            }
        }
    }
}
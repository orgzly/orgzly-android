package com.orgzly.android.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.orgzly.BuildConfig
import com.orgzly.android.db.dao.*
import com.orgzly.android.db.entity.*
import com.orgzly.android.db.mappers.OrgTimestampMapper
import com.orgzly.android.util.LogUtils
import com.orgzly.org.OrgActiveTimestamps
import com.orgzly.org.datetime.OrgDateTime
import java.util.*

@Database(
        entities = [
            Book::class,
            BookLink::class,
            BookSync::class,
            DbRepoBook::class,
            Note::class,
            NoteAncestor::class,
            NoteProperty::class,
            NoteEvent::class,
            OrgRange::class,
            OrgTimestamp::class,
            Repo::class,
            Rook::class,
            RookUrl::class,
            SavedSearch::class,
            VersionedRook::class,
            AppLog::class
        ],

        version = 156
)
@TypeConverters(com.orgzly.android.db.TypeConverters::class)
abstract class OrgzlyDatabase : RoomDatabase() {

    abstract fun book(): BookDao
    abstract fun bookLink(): BookLinkDao
    abstract fun bookView(): BookViewDao
    abstract fun bookSync(): BookSyncDao
    abstract fun noteAncestor(): NoteAncestorDao
    abstract fun note(): NoteDao
    abstract fun noteView(): NoteViewDao
    abstract fun noteProperty(): NotePropertyDao
    abstract fun noteEvent(): NoteEventDao
    abstract fun orgRange(): OrgRangeDao
    abstract fun reminderTime(): ReminderTimeDao
    abstract fun orgTimestamp(): OrgTimestampDao
    abstract fun repo(): RepoDao
    abstract fun rook(): RookDao
    abstract fun rookUrl(): RookUrlDao
    abstract fun savedSearch(): SavedSearchDao
    abstract fun versionedRook(): VersionedRookDao
    abstract fun dbRepoBook(): DbRepoBookDao
    abstract fun appLog(): AppLogDao

    companion object {
        private val TAG = OrgzlyDatabase::class.java.name

        const val NAME = "orgzly.db"

        const val NAME_FOR_TESTS = "test_orgzly.db"

        /// Defined in sqlite3.c: The maximum value of a ?nnn wildcard that the parser will accept
        const val SQLITE_MAX_VARIABLE_NUMBER = 999

        @JvmStatic
        fun forMemory(context: Context): OrgzlyDatabase {
            return Room.inMemoryDatabaseBuilder(context.applicationContext, OrgzlyDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
        }

        @JvmStatic
        fun forFile(context: Context, fileName: String): OrgzlyDatabase {
            return Room.databaseBuilder(
                    context.applicationContext, OrgzlyDatabase::class.java, fileName)
                    .allowMainThreadQueries() // TODO: Remove
                    .addMigrations(
                            PreRoomMigration.MIGRATION_130_131,
                            PreRoomMigration.MIGRATION_131_132,
                            PreRoomMigration.MIGRATION_132_133,
                            PreRoomMigration.MIGRATION_133_134,
                            PreRoomMigration.MIGRATION_134_135,
                            PreRoomMigration.MIGRATION_135_136,
                            PreRoomMigration.MIGRATION_136_137,
                            PreRoomMigration.MIGRATION_137_138, // v1.4.13
                            PreRoomMigration.MIGRATION_138_139, // v1.5
                            PreRoomMigration.MIGRATION_139_140,
                            PreRoomMigration.MIGRATION_140_141,
                            PreRoomMigration.MIGRATION_141_142,
                            PreRoomMigration.MIGRATION_142_143,
                            PreRoomMigration.MIGRATION_143_144,
                            PreRoomMigration.MIGRATION_144_145,
                            PreRoomMigration.MIGRATION_145_146,
                            PreRoomMigration.MIGRATION_146_147,
                            PreRoomMigration.MIGRATION_147_148,
                            PreRoomMigration.MIGRATION_148_149,

                            MIGRATION_149_150, // Switch to Room
                            MIGRATION_150_151,
                            MIGRATION_151_152,
                            MIGRATION_152_153,
                            MIGRATION_153_154,
                            MIGRATION_154_155,
                            MIGRATION_155_156
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Database created")

                            insertDefaultSearches(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Database opened")
                        }
                    })
                    .build()
        }

        fun insertDefaultSearches(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT INTO searches (`name`, `query`, `position`) VALUES (\"Agenda\", \".it.done ad.7\", 1)")
            db.execSQL("INSERT INTO searches (`name`, `query`, `position`) VALUES (\"Next 3 days\", \".it.done s.ge.today ad.3\", 2)")
            db.execSQL("INSERT INTO searches (`name`, `query`, `position`) VALUES (\"Scheduled\", \"s.today .it.done\", 3)")
            db.execSQL("INSERT INTO searches (`name`, `query`, `position`) VALUES (\"To Do\", \"i.todo\", 4)")
        }

        private val MIGRATION_149_150 = object : Migration(149, 150) {
            override fun migrate(db: SupportSQLiteDatabase) {
                /*
                 * Recreate all tables (foreign keys were introduced,
                 * different indexes, but also major schema changes).
                 */

                db.execSQL("CREATE TABLE IF NOT EXISTS `books_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `title` TEXT, `mtime` INTEGER, `is_dummy` INTEGER NOT NULL, `is_deleted` INTEGER, `preface` TEXT, `is_indented` INTEGER, `used_encoding` TEXT, `detected_encoding` TEXT, `selected_encoding` TEXT, `sync_status` TEXT, `is_modified` INTEGER NOT NULL, `last_action_type` TEXT, `last_action_message` TEXT, `last_action_timestamp` INTEGER)")
                db.execSQL("INSERT INTO books_new (id, name, title, mtime, is_dummy, is_deleted, preface, is_indented, used_encoding, detected_encoding, selected_encoding, sync_status, is_modified, last_action_type, last_action_message, last_action_timestamp) " +
                        "SELECT _id, name, title, mtime, is_dummy, is_deleted, preface, is_indented, used_encoding, detected_encoding, selected_encoding, sync_status, 0, last_action_type, last_action, last_action_timestamp FROM books")
                db.execSQL("DROP TABLE books")
                db.execSQL("ALTER TABLE books_new RENAME TO books")
                db.execSQL("CREATE UNIQUE INDEX `index_books_name` ON `books` (`name`)")

                // Set new is_modified field: syncedTo != null && book.mtime ?: 0 > syncedTo.mtime
                db.execSQL("""
                    UPDATE books SET is_modified = (
                        SELECT rook_mtime IS NOT NULL AND (coalesce(mtime, 0) > rook_mtime)
                        FROM books AS b
                        LEFT JOIN book_syncs ON (book_syncs.book_id = books.id)
                        LEFT JOIN versioned_rooks ON (versioned_rooks._id = book_syncs.book_versioned_rook_id)
                        WHERE books.id = b.id
                    )
                """)

                db.execSQL("CREATE TABLE IF NOT EXISTS `book_links_new` (`book_id` INTEGER NOT NULL, `repo_id` INTEGER NOT NULL, PRIMARY KEY(`book_id`), FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`repo_id`) REFERENCES `repos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO book_links_new (book_id, repo_id) " +
                        "SELECT book_id, repo_id FROM book_links")
                db.execSQL("DROP TABLE book_links")
                db.execSQL("ALTER TABLE book_links_new RENAME TO book_links")
                db.execSQL("CREATE  INDEX `index_book_links_repo_id` ON `book_links` (`repo_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `book_syncs_new` (`book_id` INTEGER NOT NULL, `versioned_rook_id` INTEGER NOT NULL, PRIMARY KEY(`book_id`), FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`versioned_rook_id`) REFERENCES `versioned_rooks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO book_syncs_new (book_id, versioned_rook_id) " +
                        "SELECT book_id, book_versioned_rook_id FROM book_syncs WHERE book_id IS NOT NULL AND book_versioned_rook_id IS NOT NULL")
                db.execSQL("DROP TABLE book_syncs")
                db.execSQL("ALTER TABLE book_syncs_new RENAME TO book_syncs")
                db.execSQL("CREATE  INDEX `index_book_syncs_versioned_rook_id` ON `book_syncs` (`versioned_rook_id`)")

                // Removed current_versioned_rooks usage
                db.execSQL("DROP TABLE current_versioned_rooks")

                db.execSQL("CREATE TABLE IF NOT EXISTS `db_repo_books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `repo_url` TEXT NOT NULL, `url` TEXT NOT NULL, `revision` TEXT NOT NULL, `mtime` INTEGER NOT NULL, `content` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO db_repo_books (id, repo_url, url, revision, mtime, content, created_at) " +
                        "SELECT _id, repo_url, url, revision, mtime, content, created_at FROM db_repos")
                db.execSQL("DROP TABLE db_repos")
                db.execSQL("CREATE UNIQUE INDEX `index_db_repo_books_repo_url_url` ON `db_repo_books` (`repo_url`, `url`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `notes_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `is_cut` INTEGER NOT NULL, `created_at` INTEGER, `title` TEXT NOT NULL, `tags` TEXT, `state` TEXT, `priority` TEXT, `content` TEXT, `content_line_count` INTEGER NOT NULL, `scheduled_range_id` INTEGER, `deadline_range_id` INTEGER, `closed_range_id` INTEGER, `clock_range_id` INTEGER, `book_id` INTEGER NOT NULL, `lft` INTEGER NOT NULL, `rgt` INTEGER NOT NULL, `level` INTEGER NOT NULL, `parent_id` INTEGER NOT NULL, `folded_under_id` INTEGER NOT NULL, `is_folded` INTEGER NOT NULL, `descendants_count` INTEGER NOT NULL, FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`scheduled_range_id`) REFERENCES `org_ranges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`deadline_range_id`) REFERENCES `org_ranges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`closed_range_id`) REFERENCES `org_ranges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO notes_new (id, is_cut, created_at, title, tags, state, priority, content, content_line_count, scheduled_range_id, deadline_range_id, closed_range_id, clock_range_id, book_id, lft, rgt, level, parent_id, descendants_count, is_folded, folded_under_id) " +
                        "SELECT _id, COALESCE(is_cut, 0), NULLIF(created_at, 0), title, tags, state, priority, content, COALESCE(content_line_count, 0), scheduled_range_id, deadline_range_id, closed_range_id, clock_range_id, book_id, is_visible, parent_position, level, COALESCE(parent_id, 0), has_children, is_collapsed, is_under_collapsed FROM notes")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
                db.execSQL("CREATE  INDEX `index_notes_title` ON `notes` (`title`)")
                db.execSQL("CREATE  INDEX `index_notes_tags` ON `notes` (`tags`)")
                db.execSQL("CREATE  INDEX `index_notes_content` ON `notes` (`content`)")
                db.execSQL("CREATE  INDEX `index_notes_book_id` ON `notes` (`book_id`)")
                db.execSQL("CREATE  INDEX `index_notes_is_cut` ON `notes` (`is_cut`)")
                db.execSQL("CREATE  INDEX `index_notes_lft` ON `notes` (`lft`)")
                db.execSQL("CREATE  INDEX `index_notes_rgt` ON `notes` (`rgt`)")
                db.execSQL("CREATE  INDEX `index_notes_is_folded` ON `notes` (`is_folded`)")
                db.execSQL("CREATE  INDEX `index_notes_folded_under_id` ON `notes` (`folded_under_id`)")
                db.execSQL("CREATE  INDEX `index_notes_parent_id` ON `notes` (`parent_id`)")
                db.execSQL("CREATE  INDEX `index_notes_descendants_count` ON `notes` (`descendants_count`)")
                db.execSQL("CREATE  INDEX `index_notes_scheduled_range_id` ON `notes` (`scheduled_range_id`)")
                db.execSQL("CREATE  INDEX `index_notes_deadline_range_id` ON `notes` (`deadline_range_id`)")
                db.execSQL("CREATE  INDEX `index_notes_closed_range_id` ON `notes` (`closed_range_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `note_ancestors_new` (`note_id` INTEGER NOT NULL, `book_id` INTEGER NOT NULL, `ancestor_note_id` INTEGER NOT NULL, PRIMARY KEY(`book_id`, `note_id`, `ancestor_note_id`), FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`ancestor_note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO note_ancestors_new (note_id, book_id, ancestor_note_id) " +
                        "SELECT note_id, book_id, ancestor_note_id FROM note_ancestors")
                db.execSQL("DROP TABLE note_ancestors")
                db.execSQL("ALTER TABLE note_ancestors_new RENAME TO note_ancestors")
                db.execSQL("CREATE  INDEX `index_note_ancestors_book_id` ON `note_ancestors` (`book_id`)")
                db.execSQL("CREATE  INDEX `index_note_ancestors_note_id` ON `note_ancestors` (`note_id`)")
                db.execSQL("CREATE  INDEX `index_note_ancestors_ancestor_note_id` ON `note_ancestors` (`ancestor_note_id`)")


                db.execSQL("CREATE TABLE IF NOT EXISTS `note_properties_new` (`note_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`note_id`, `position`), FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE UNIQUE INDEX `index_note_properties_note_id_name` ON `note_properties_new` (`note_id`, `name`)")
                db.execSQL("CREATE  INDEX `index_note_properties_position` ON `note_properties_new` (`position`)")
                db.execSQL("CREATE  INDEX `index_note_properties_name` ON `note_properties_new` (`name`)")
                db.execSQL("CREATE  INDEX `index_note_properties_value` ON `note_properties_new` (`value`)")

                db.query("""
                    SELECT
                        note_properties.note_id,
                        note_properties.position,
                        property_names.name,
                        property_values.value
                    FROM note_properties
                    LEFT JOIN properties ON (properties._id = note_properties.property_id)
                    LEFT JOIN property_names ON (property_names._id = properties.name_id)
                    LEFT JOIN property_values ON (property_values._id = properties.value_id)
                    ORDER BY note_properties.note_id, note_properties.position
                """).use {
                    var id = 0L
                    var position = 1
                    while (it.moveToNext()) {
                        if (id != it.getLong(0)) { // First new note ID
                            id = it.getLong(0)
                            position = 1
                        }

                        val values = ContentValues()
                        values.put("note_id", id)
                        values.put("position", position++)
                        values.put("name", it.getString(2))
                        values.put("value", it.getString(3))

                        db.insert("note_properties_new", SQLiteDatabase.CONFLICT_REPLACE, values)
                    }
                }

                db.execSQL("DROP TABLE note_properties")
                db.execSQL("DROP TABLE properties")
                db.execSQL("DROP TABLE property_names")
                db.execSQL("DROP TABLE property_values")

                db.execSQL("ALTER TABLE note_properties_new RENAME TO note_properties")


                db.execSQL("CREATE TABLE IF NOT EXISTS `org_ranges_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `string` TEXT NOT NULL, `start_timestamp_id` INTEGER NOT NULL, `end_timestamp_id` INTEGER, `difference` INTEGER, FOREIGN KEY(`start_timestamp_id`) REFERENCES `org_timestamps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`end_timestamp_id`) REFERENCES `org_timestamps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO org_ranges_new (id, string, start_timestamp_id, end_timestamp_id, difference) " +
                        "SELECT _id, string, start_timestamp_id, end_timestamp_id, difference FROM org_ranges")
                db.execSQL("DROP TABLE org_ranges")
                db.execSQL("ALTER TABLE org_ranges_new RENAME TO org_ranges")
                db.execSQL("CREATE UNIQUE INDEX `index_org_ranges_string` ON `org_ranges` (`string`)")
                db.execSQL("CREATE  INDEX `index_org_ranges_start_timestamp_id` ON `org_ranges` (`start_timestamp_id`)")
                db.execSQL("CREATE  INDEX `index_org_ranges_end_timestamp_id` ON `org_ranges` (`end_timestamp_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `org_timestamps_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `string` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `hour` INTEGER, `minute` INTEGER, `second` INTEGER, `end_hour` INTEGER, `end_minute` INTEGER, `end_second` INTEGER, `repeater_type` INTEGER, `repeater_value` INTEGER, `repeater_unit` INTEGER, `habit_deadline_value` INTEGER, `habit_deadline_unit` INTEGER, `delay_type` INTEGER, `delay_value` INTEGER, `delay_unit` INTEGER, `timestamp` INTEGER NOT NULL, `end_timestamp` INTEGER)")
                db.execSQL("INSERT INTO org_timestamps_new (id, string, is_active, year, month, day, hour, minute, second, end_hour, end_minute, end_second, repeater_type, repeater_value, repeater_unit, habit_deadline_value, habit_deadline_unit, delay_type, delay_unit, timestamp, end_timestamp) " +
                        "SELECT _id, string, is_active, year, month, day, hour, minute, second, end_hour, end_minute, end_second, repeater_type, repeater_value, repeater_unit, habit_deadline_value, habit_deadline_unit, delay_type, delay_unit, timestamp, end_timestamp FROM org_timestamps")
                db.execSQL("DROP TABLE org_timestamps")
                db.execSQL("ALTER TABLE org_timestamps_new RENAME TO org_timestamps")
                db.execSQL("CREATE UNIQUE INDEX `index_org_timestamps_string` ON `org_timestamps` (`string`)")
                db.execSQL("CREATE  INDEX `index_org_timestamps_timestamp` ON `org_timestamps` (`timestamp`)")
                db.execSQL("CREATE  INDEX `index_org_timestamps_end_timestamp` ON `org_timestamps` (`end_timestamp`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `repos_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL)")
                db.execSQL("INSERT INTO repos_new (id, url) " +
                        "SELECT _id, repo_url FROM repos WHERE is_repo_active = 1")
                db.execSQL("DROP TABLE repos")
                db.execSQL("ALTER TABLE repos_new RENAME TO repos")
                db.execSQL("CREATE UNIQUE INDEX `index_repos_url` ON `repos` (`url`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `rooks_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `repo_id` INTEGER NOT NULL, `rook_url_id` INTEGER NOT NULL, FOREIGN KEY(`repo_id`) REFERENCES `repos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`rook_url_id`) REFERENCES `rook_urls`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO rooks_new (id, repo_id, rook_url_id) " +
                        "SELECT _id, repo_id, rook_url_id FROM rooks")
                db.execSQL("DROP TABLE rooks")
                db.execSQL("ALTER TABLE rooks_new RENAME TO rooks")
                db.execSQL("CREATE UNIQUE INDEX `index_rooks_repo_id_rook_url_id` ON `rooks` (`repo_id`, `rook_url_id`)")
                db.execSQL("CREATE  INDEX `index_rooks_rook_url_id` ON `rooks` (`rook_url_id`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `rook_urls_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL)")
                db.execSQL("INSERT INTO rook_urls_new (id, url) " +
                        "SELECT _id, rook_url FROM rook_urls")
                db.execSQL("DROP TABLE rook_urls")
                db.execSQL("ALTER TABLE rook_urls_new RENAME TO rook_urls")
                db.execSQL("CREATE UNIQUE INDEX `index_rook_urls_url` ON `rook_urls` (`url`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `searches_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `query` TEXT NOT NULL, `position` INTEGER NOT NULL)")

                db.query("SELECT _id, name, search FROM searches ORDER BY position, _id").use {
                    for (i in 1..it.count) {
                        it.moveToNext()

                        val values = ContentValues()
                        values.put("id", it.getLong(0))
                        values.put("name", it.getString(1))
                        values.put("query", it.getString(2))
                        values.put("position", i)

                        db.insert("searches_new", SQLiteDatabase.CONFLICT_ROLLBACK, values)
                    }
                }

                db.execSQL("DROP TABLE searches")
                db.execSQL("ALTER TABLE searches_new RENAME TO searches")

                db.execSQL("CREATE TABLE IF NOT EXISTS `versioned_rooks_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rook_id` INTEGER NOT NULL, `rook_revision` TEXT NOT NULL, `rook_mtime` INTEGER NOT NULL, FOREIGN KEY(`rook_id`) REFERENCES `rooks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO versioned_rooks_new (id, rook_id, rook_mtime, rook_revision) " +
                        "SELECT _id, rook_id, rook_mtime, rook_revision FROM versioned_rooks")
                db.execSQL("DROP TABLE versioned_rooks")
                db.execSQL("ALTER TABLE versioned_rooks_new RENAME TO versioned_rooks")
                db.execSQL("CREATE  INDEX `index_versioned_rooks_rook_id` ON `versioned_rooks` (`rook_id`)")

                // Drop all views
                db.execSQL("DROP VIEW IF EXISTS notes_basic_view")
                db.execSQL("DROP VIEW IF EXISTS notes_view")
                db.execSQL("DROP VIEW IF EXISTS books_view")
                db.execSQL("DROP VIEW IF EXISTS times_view")
            }
        }

        private val MIGRATION_150_151 = object : Migration(150, 151) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `note_events` (`note_id` INTEGER NOT NULL, `org_range_id` INTEGER NOT NULL, PRIMARY KEY(`note_id`, `org_range_id`), FOREIGN KEY(`note_id`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`org_range_id`) REFERENCES `org_ranges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE  INDEX `index_note_events_note_id` ON `note_events` (`note_id`)")
                db.execSQL("CREATE  INDEX `index_note_events_org_range_id` ON `note_events` (`org_range_id`)")

                val query = SupportSQLiteQueryBuilder
                        .builder("notes")
                        .columns(arrayOf("id", "title", "content"))
                        .selection("(content IS NOT NULL AND content GLOB '*<[0-9][0-9][0-9][0-9]*') OR (title IS NOT NULL AND title GLOB '*<[0-9][0-9][0-9][0-9]*')", null)
                        .create()

                db.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast) {
                            val noteId = cursor.getLong(0)
                            val title = cursor.getString(1)
                            val content = cursor.getString(2)

                            OrgActiveTimestamps.parse(title).forEach { range ->
                                insertNoteTime(db, noteId, range)
                            }

                            OrgActiveTimestamps.parse(content).forEach { range ->
                                insertNoteTime(db, noteId, range)
                            }

                            cursor.moveToNext()
                        }
                    }
                }
            }

            private fun insertNoteTime(db: SupportSQLiteDatabase, noteId: Long, range: com.orgzly.org.datetime.OrgRange) {
                val orgRangeId = getOrInsertOrgRange(db, range)

                val values = ContentValues().apply {
                    put("note_id", noteId)
                    put("org_range_id", orgRangeId)
                }

                db.insert("note_events", SQLiteDatabase.CONFLICT_REPLACE, values)
            }

            private fun getOrInsertOrgRange(db: SupportSQLiteDatabase, range: com.orgzly.org.datetime.OrgRange): Long {
                val rangeStr = range.toString()

                val query = SupportSQLiteQueryBuilder
                        .builder("org_ranges")
                        .columns(arrayOf("id"))
                        .selection("string = ?", arrayOf(rangeStr))
                        .create()

                db.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0)
                    }
                }

                val startTimeId = getOrInsertOrgTime(db, range.startTime)
                val endTimeId = getOrInsertOrgTime(db, range.startTime)

                val values = ContentValues().apply {
                    put("string", rangeStr)
                    put("start_timestamp_id", startTimeId)
                    put("end_timestamp_id", endTimeId)
                }

                return db.insert("org_ranges", SQLiteDatabase.CONFLICT_ROLLBACK, values)
            }

            private fun getOrInsertOrgTime(db: SupportSQLiteDatabase, time: OrgDateTime): Long {
                val query = SupportSQLiteQueryBuilder
                        .builder("org_timestamps")
                        .columns(arrayOf("id"))
                        .selection("string = ?", arrayOf(time.toString()))
                        .create()

                db.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0)
                    }
                }

                val values = ContentValues().apply {
                    put("string", time.toString())

                    put("is_active", time.isActive)

                    put("year", time.calendar.get(Calendar.YEAR))
                    put("month", time.calendar.get(Calendar.MONTH) + 1)
                    put("day", time.calendar.get(Calendar.DAY_OF_MONTH))

                    if (time.hasTime()) {
                        put("hour", time.calendar.get(Calendar.HOUR_OF_DAY))
                        put("minute", time.calendar.get(Calendar.MINUTE))
                        put("second", time.calendar.get(Calendar.SECOND))
                    }

                    put("timestamp", time.calendar.timeInMillis)

                    if (time.hasEndTime()) {
                        put("end_hour", time.endCalendar.get(Calendar.HOUR_OF_DAY))
                        put("end_minute", time.endCalendar.get(Calendar.MINUTE))
                        put("end_second", time.endCalendar.get(Calendar.SECOND))
                        put("end_timestamp", time.endCalendar.timeInMillis)
                    }

                    if (time.hasRepeater()) {
                        put("repeater_type", OrgTimestampMapper.repeaterType(time.repeater.type))
                        put("repeater_value", time.repeater.value)
                        put("repeater_unit", OrgTimestampMapper.timeUnit(time.repeater.unit))

                        if (time.repeater.hasHabitDeadline()) {
                            put("habit_deadline_value", time.repeater.habitDeadline.value)
                            put("habit_deadline_unit", OrgTimestampMapper.timeUnit(time.repeater.habitDeadline.unit))
                        }
                    }

                    if (time.hasDelay()) {
                        put("delay_type", OrgTimestampMapper.delayType(time.delay.type))
                        put("delay_value", time.delay.value)
                        put("delay_unit", OrgTimestampMapper.timeUnit(time.delay.unit))
                    }
                }

                return db.insert("org_timestamps", SQLiteDatabase.CONFLICT_ROLLBACK, values)
            }
        }

        private val MIGRATION_151_152 = object : Migration(151, 152) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX index_note_properties_note_id_name")
                db.execSQL("CREATE  INDEX `index_note_properties_note_id` ON `note_properties` (`note_id`)")
            }
        }

        private val MIGRATION_152_153 = object : Migration(152, 153) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repos ADD COLUMN type INTEGER NOT NULL DEFAULT 0")

                val types = mutableMapOf<Long, Int>()

                val query = SupportSQLiteQueryBuilder
                        .builder("repos")
                        .columns(arrayOf("id", "url"))
                        .create()

                db.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast) {
                            val id = cursor.getLong(0)
                            val url = cursor.getString(1)

                            types[id] = when {
                                url.startsWith("mock") -> 1
                                url.startsWith("dropbox") -> 2
                                url.startsWith("file") -> 3
                                url.startsWith("content") -> 4
                                url.matches("^(webdav|dav|http)s?.*".toRegex()) -> 5
                                else -> throw IllegalArgumentException("Unknown repo $url")
                            }

                            cursor.moveToNext()
                        }
                    }
                }

                types.keys.forEach { id ->
                    val values = ContentValues().apply {
                        put("type", types[id])
                    }
                    db.update("repos", SQLiteDatabase.CONFLICT_ROLLBACK, values, "id = $id", null)
                }
            }
        }

        private val MIGRATION_153_154 = object : Migration(153, 154) {
            override fun migrate(db: SupportSQLiteDatabase) {
                /*
                 * Delete book-related entries with missing repos data.
                 * Deleting or renaming repositories before v1.7 was deleting entries from repos
                 * without deleting dependent entries from other tables. This started causing
                 * crashes in v1.8 with addition of RepoType.
                 */
                db.execSQL("DELETE FROM rooks WHERE repo_id NOT IN (SELECT id FROM repos)")
                db.execSQL("DELETE FROM versioned_rooks WHERE rook_id NOT IN (SELECT id FROM rooks)")
                db.execSQL("DELETE FROM book_syncs WHERE versioned_rook_id NOT IN (SELECT id FROM versioned_rooks)")
                db.execSQL("DELETE FROM book_syncs WHERE book_id NOT IN (SELECT id FROM books)")
            }
        }

        private val MIGRATION_154_155 = object : Migration(154, 155) {
            override fun migrate(db: SupportSQLiteDatabase) {
                /*
                 * Fix timestamp values in org_timestamps
                 * https://github.com/orgzly/orgzly-android/issues/704
                 */

                val query = SupportSQLiteQueryBuilder
                        .builder("org_timestamps")
                        .columns(arrayOf("id", "string"))
                        .create()

                db.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast) {
                            val id = cursor.getLong(0)
                            val string = cursor.getString(1)

                            val timestamp = OrgDateTime.doParse(string).calendar.timeInMillis

                            val values = ContentValues().apply {
                                put("timestamp", timestamp)
                            }

                            db.update("org_timestamps", SQLiteDatabase.CONFLICT_ROLLBACK, values, "id = $id", null)

                            cursor.moveToNext()
                        }
                    }
                }
            }
        }

        private val MIGRATION_155_156 = object : Migration(155, 156) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `name` TEXT NOT NULL, `message` TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_logs_timestamp` ON `app_logs` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_logs_name` ON `app_logs` (`name`)")
            }
        }
    }
}

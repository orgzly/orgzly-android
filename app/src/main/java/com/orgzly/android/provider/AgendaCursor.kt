package com.orgzly.android.provider

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.provider.BaseColumns
import com.orgzly.android.provider.models.DbNoteColumns
import com.orgzly.android.provider.views.DbNoteViewColumns
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.util.AgendaUtils
import com.orgzly.android.util.UserTimeFormatter
import org.joda.time.DateTime
import java.util.*

object AgendaCursor {
    data class NoteForDay(val noteId: Long, val day: DateTime)

    data class AgendaMergedCursor(val cursor: Cursor, val originalNoteIDs: Map<Long, NoteForDay>)

    @JvmStatic
    fun create(context: Context, cursor: Cursor, query: String): AgendaMergedCursor {
        val parser = InternalQueryParser()
        val (_, _, options) = parser.parse(query)

        var agendaDays = options.agendaDays

//        if (agendaDays > MAX_DAYS) {
//            agendaDays = MAX_DAYS
//        }

        // Add IS_DIVIDER column
        val columnNames = arrayOfNulls<String>(cursor.columnNames.size + 1)
        System.arraycopy(cursor.columnNames, 0, columnNames, 0, cursor.columnNames.size)
        columnNames[columnNames.size - 1] = Columns.IS_DIVIDER

        val agenda = LinkedHashMap<Long, MatrixCursor>()
        var day = DateTime.now().withTimeAtStartOfDay()
        var i = 0
        // create entries from today to today+agenda_len
        do {
            val matrixCursor = MatrixCursor(columnNames)
            agenda[day.millis] = matrixCursor
            day = day.plusDays(1)
        } while (++i < agendaDays)

        val now = DateTime.now()

        val scheduledRangeStrIdx = cursor.getColumnIndex(DbNoteViewColumns.SCHEDULED_RANGE_STRING)
        val deadlineRangeStrIdx = cursor.getColumnIndex(DbNoteViewColumns.DEADLINE_RANGE_STRING)

        var nextId = 1L
        val originalNoteIDs = mutableMapOf<Long, NoteForDay>()

        GenericDatabaseUtils.forEachRow(cursor) {
            // Expand each note if it has a repeater or is a range
            val dates = AgendaUtils.expandOrgDateTime(
                    arrayOf(cursor.getString(scheduledRangeStrIdx), cursor.getString(deadlineRangeStrIdx)),
                    now,
                    agendaDays
            )

            // Add notes to each day
            dates.forEach { dd ->
                agenda[dd.millis]?.let {
                    val rowBuilder = it.newRow()

                    for (col in columnNames) {
                        when {
                            col.equals(BaseColumns._ID) -> {
                                // Add next id
                                rowBuilder.add(nextId)

                                // Update map of original ids
                                val noteId = cursor.getLong(cursor.getColumnIndex(col))
                                originalNoteIDs[nextId] = NoteForDay(noteId, dd)

                                nextId++
                            }

                            col.equals(Columns.IS_DIVIDER) ->
                                // Mark row as not being a divider
                                rowBuilder.add(0)

                            else ->
                                // Actual note's data
                                rowBuilder.add(cursor.getString(cursor.getColumnIndex(col)))
                        }
                    }
                }
            }
        }

        val userTimeFormatter = UserTimeFormatter(context)

        val mergedCursor = mergeDates(nextId, agenda, userTimeFormatter)

        return AgendaMergedCursor(mergedCursor, originalNoteIDs)
    }

    @JvmStatic
    fun isDivider(cursor: Cursor): Boolean {
        return cursor.getInt(cursor.getColumnIndex(AgendaCursor.Columns.IS_DIVIDER)) == 1
    }

    @JvmStatic
    fun getDividerDate(cursor: Cursor): String {
        return cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE))
    }

    private fun mergeDates(id: Long, agenda: Map<Long, MatrixCursor>, timeFormatter: UserTimeFormatter): MergeCursor {
        var nextId = id
        val cursors = ArrayList<Cursor>()

        for (dateMilli in agenda.keys) {
            val date = DateTime(dateMilli)

            // Add divider
            val dateCursor = MatrixCursor(arrayOf(
                    BaseColumns._ID,
                    Columns.DIVIDER_VALUE,
                    Columns.IS_DIVIDER))

            val dateRow = dateCursor.newRow()

            dateRow.add(nextId++)
            dateRow.add(timeFormatter.formatDate(date))
            dateRow.add(1)

            cursors.add(dateCursor)

            // Add notes
            agenda[dateMilli]?.let {
                cursors.add(it)
            }
        }

        return MergeCursor(cursors.toTypedArray())
    }

    object Columns: BaseColumns, DbNoteColumns {
        const val IS_DIVIDER = "is_divider"
        const val DIVIDER_VALUE = "divider_value"
    }

//    private const val MAX_DAYS = 30

//    private val TAG = AgendaCursor::class.java.name
}
package com.orgzly.android.provider

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.provider.BaseColumns
import android.support.v4.util.LongSparseArray
import com.orgzly.android.provider.models.DbNoteColumns
import com.orgzly.android.provider.views.DbNoteViewColumns
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.util.AgendaUtils
import com.orgzly.android.util.UserTimeFormatter
import org.joda.time.DateTime
import java.util.*

object AgendaCursor {
    data class AgendaMergedCursor(val cursor: Cursor, val originalNoteIDs: LongSparseArray<Long>)

    fun create(context: Context, cursor: Cursor, mQuery: String): AgendaMergedCursor {
        val parser = InternalQueryParser()
        val (_, _, options) = parser.parse(mQuery)

        var agendaDays = options.agendaDays

        if (agendaDays > MAX_DAYS) {
            agendaDays = MAX_DAYS
        }

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
        originalNoteIDs.clear()

        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            // Expand each note if it has a repeater or is a range
            val dates = AgendaUtils.expandOrgDateTime(
                    arrayOf(cursor.getString(scheduledRangeStrIdx), cursor.getString(deadlineRangeStrIdx)),
                    now,
                    agendaDays
            )

            // Add notes to each day
            dates
                    .asSequence()
                    .mapNotNull {
                        agenda[it.millis]
                    }
                    .forEach { matrixCursor ->
                        val rowBuilder = matrixCursor.newRow()

                        for (col in columnNames) {
                            when {
                                col.equals(BaseColumns._ID) -> {
                                    // Add next id
                                    rowBuilder.add(nextId)

                                    // Update map of original ids
                                    val noteId = cursor.getLong(cursor.getColumnIndex(col))
                                    originalNoteIDs.put(nextId, noteId)

                                    nextId++
                                }

                                col.equals(Columns.IS_DIVIDER) ->
                                    // Mark row as not a separator
                                    rowBuilder.add(0)

                                else ->
                                    // Actual note's data
                                    rowBuilder.add(cursor.getString(cursor.getColumnIndex(col)))
                            }
                        }

                    }

            cursor.moveToNext()
        }

        val userTimeFormatter = UserTimeFormatter(context)

        val mergedCursor = mergeDates(nextId, agenda, userTimeFormatter)

        return AgendaMergedCursor(mergedCursor, originalNoteIDs)
    }

    fun isDivider(cursor: Cursor): Boolean {
        return cursor.getInt(cursor.getColumnIndex(AgendaCursor.Columns.IS_DIVIDER)) == 1
    }

    fun getDividerDate(cursor: Cursor): String {
        return cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE))
    }

    private fun mergeDates(id: Long, agenda: Map<Long, MatrixCursor>, userTimeFormatter: UserTimeFormatter): MergeCursor {
        var nextId = id
        val allCursors = ArrayList<Cursor>()

        for (dateMilli in agenda.keys) {
            val date = DateTime(dateMilli)

            /* Add divider. */
            val dateCursor = MatrixCursor(arrayOf(
                    BaseColumns._ID,
                    Columns.DIVIDER_VALUE,
                    Columns.IS_DIVIDER))

            val dateRow = dateCursor.newRow()

            dateRow.add(nextId++)
            dateRow.add(userTimeFormatter.formatDate(date))
            dateRow.add(1) // Mark as a separator

            allCursors.add(dateCursor)

            agenda[dateMilli]?.let {
                allCursors.add(it)
            }
        }

        return MergeCursor(allCursors.toTypedArray())
    }

    private val originalNoteIDs = LongSparseArray<Long>()

    object Columns : BaseColumns, DbNoteColumns {
        const val IS_DIVIDER = "is_separator"
        const val DIVIDER_VALUE = "day"
    }

    private const val MAX_DAYS = 30
}
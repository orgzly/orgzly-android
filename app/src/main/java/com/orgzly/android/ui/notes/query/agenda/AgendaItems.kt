package com.orgzly.android.ui.notes.query.agenda

import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.util.AgendaUtils
import org.joda.time.DateTime

object AgendaItems {
    data class ExpandableOrgRange(val range: String?, val overdueToday: Boolean)

    fun getList(
            notes: List<NoteView>, queryString: String?, idMap: MutableMap<Long, Long>
    ): List<AgendaItem> {

        return if (queryString != null) {
            val parser = InternalQueryParser()

            val query = parser.parse(queryString)

            getList(notes, query, idMap)

        } else {
            listOf()
        }
    }

    fun getList(
            notes: List<NoteView>, query: Query, item2databaseIds: MutableMap<Long, Long>
    ): List<AgendaItem> {

        return getList(notes, item2databaseIds, query.options.agendaDays)
    }

    private fun getList(
            notes: List<NoteView>,
            item2databaseIds: MutableMap<Long, Long>,
            agendaDays: Int
    ): List<AgendaItem> {

        item2databaseIds.clear()

        var index = 1L

        val now = DateTime.now().withTimeAtStartOfDay()

        // Create day buckets
        val dayBuckets = (0 until agendaDays)
                .map { i -> now.plusDays(i) }
                .associateBy(
                        { it.millis },
                        { mutableListOf<AgendaItem>(AgendaItem.Divider(index++, it)) })

        notes.forEach { note ->

            val times = arrayOf(
                    ExpandableOrgRange(note.scheduledRangeString, overdueToday = true),
                    ExpandableOrgRange(note.deadlineRangeString, overdueToday = true),
                    ExpandableOrgRange(note.eventString, overdueToday = false)
            )

            // Expand each note if it has a repeater or is a range
            val days = AgendaUtils.expandOrgDateTime(times, now, agendaDays)

            // Add each note instance to its day bucket
            days.forEach { day ->
                dayBuckets[day.millis]?.let {
                    it.add(AgendaItem.Note(index, note))
                    item2databaseIds[index] = note.note.id
                    index++
                }
            }
        }

        return dayBuckets.values.flatten() // FIXME
    }
}
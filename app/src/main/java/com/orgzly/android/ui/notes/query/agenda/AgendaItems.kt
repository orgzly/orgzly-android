package com.orgzly.android.ui.notes.query.agenda

import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.TimeType
import com.orgzly.android.util.AgendaUtils
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRange
import org.joda.time.DateTime

class AgendaItems(private val hideEmptyDaysInAgenda : Boolean) {
    data class ExpandableOrgRange(
            val range: OrgRange,
            val canBeOverdueToday: Boolean,
            val warningPeriod: OrgInterval?,
            val delayPeriod: OrgInterval?) {

        companion object {
            fun fromRange(timeType: TimeType, range: OrgRange): ExpandableOrgRange {
                val canBeOverdueToday = timeType == TimeType.SCHEDULED || timeType == TimeType.DEADLINE

                val warningPeriod = when (timeType) {
                    TimeType.DEADLINE -> range.startTime.delay
                    else -> null
                }

                val delayPeriod = when (timeType) {
                    TimeType.SCHEDULED -> range.startTime.delay
                    else -> null
                }

                return ExpandableOrgRange(range, canBeOverdueToday, warningPeriod, delayPeriod)
            }
        }
    }

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

        var agendaItemId = 1L

        val now = DateTime.now().withTimeAtStartOfDay()

        val overdueNotes = mutableListOf<AgendaItem>()

        val dailyNotes = (0 until agendaDays)
                .map { i -> now.plusDays(i) }
                .associateBy({ it.millis }, { mutableListOf<AgendaItem>() })

        val addedPlanningTimes = HashSet<Long>()

        fun addInstances(note: NoteView, timeType: TimeType, timeString: String) {
            val range = OrgRange.parseOrNull(timeString) ?: return

            if (!range.startTime.isActive) {
                return
            }

            val expandable = ExpandableOrgRange.fromRange(timeType, range)

            val times = AgendaUtils.expandOrgDateTime(expandable, now, agendaDays)

            if (times.isOverdueToday) {
                overdueNotes.add(AgendaItem.Note(agendaItemId, note, timeType))
                item2databaseIds[agendaItemId] = note.note.id
                agendaItemId++
            }

            // Add each note instance to its day bucket
            times.expanded.forEach { time ->
                val bucketKey = time.withTimeAtStartOfDay().millis

                dailyNotes[bucketKey]?.let {
                    it.add(AgendaItem.Note(agendaItemId, note, timeType))
                    item2databaseIds[agendaItemId] = note.note.id
                    agendaItemId++
                }
            }
        }

        notes.forEach { note ->
            // Add planning times for a note only once
            if (!addedPlanningTimes.contains(note.note.id)) {
                note.scheduledRangeString?.let {
                    addInstances(note, TimeType.SCHEDULED, it)
                }
                note.deadlineRangeString?.let {
                    addInstances(note, TimeType.DEADLINE, it)
                }

                addedPlanningTimes.add(note.note.id)
            }

            // Add each note's event
            note.eventString?.let {
                addInstances(note, TimeType.EVENT, it)
            }
        }

        val result = mutableListOf<AgendaItem>()

        // Add overdue heading and notes
        if (overdueNotes.isNotEmpty()) {
            result.add(AgendaItem.Overdue(agendaItemId++))
            result.addAll(overdueNotes)
        }

        // Add daily
        dailyNotes.forEach { d ->
            if (d.value.isNotEmpty() || !hideEmptyDaysInAgenda) {
                result.add(AgendaItem.Day(agendaItemId++, DateTime(d.key)))
            }

            if (d.value.isNotEmpty()) {
                result.addAll(d.value)
            }
        }

        return result
    }
}
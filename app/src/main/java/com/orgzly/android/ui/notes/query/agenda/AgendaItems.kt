package com.orgzly.android.ui.notes.query.agenda

import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.TimeType
import com.orgzly.android.util.AgendaUtils
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRange
import org.joda.time.DateTime

object AgendaItems {
    data class ExpandableOrgRange(
            val range: OrgRange,
            val overdueToday: Boolean,
            val warningPeriod: OrgInterval?,
            val delayPeriod: OrgInterval?) {

        companion object {
            fun fromRange(timeType: TimeType, range: OrgRange): ExpandableOrgRange {
                val overdueToday = timeType == TimeType.SCHEDULED || timeType == TimeType.DEADLINE

                val warningPeriod = when (timeType) {
                    TimeType.DEADLINE -> range.startTime.delay
                    else -> null
                }

                val delayPeriod = when (timeType) {
                    TimeType.SCHEDULED -> range.startTime.delay
                    else -> null
                }

                return ExpandableOrgRange(range, overdueToday, warningPeriod, delayPeriod)
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

        // Create day buckets
        val dayBuckets = (0 until agendaDays)
                .map { i -> now.plusDays(i) }
                .associateBy(
                        { it.millis },
                        { mutableListOf<AgendaItem>(AgendaItem.Divider(agendaItemId++, it)) })

        val addedPlanningTimes = HashSet<Long>()

        notes.forEach { note ->

            fun addInstances(timeType: TimeType, timeString: String) {
                val range = OrgRange.parseOrNull(timeString) ?: return

                val expandable = ExpandableOrgRange.fromRange(timeType, range)

                val times = AgendaUtils.expandOrgDateTime(expandable, now, agendaDays)

                // Add each note instance to its day bucket
                times.forEach { time ->
                    val bucketKey = time.withTimeAtStartOfDay().millis

                    dayBuckets[bucketKey]?.let {
                        it.add(AgendaItem.Note(agendaItemId, note, timeType))
                        item2databaseIds[agendaItemId] = note.note.id
                        agendaItemId++
                    }
                }
            }

            // Add planning times for a note only once
            if (!addedPlanningTimes.contains(note.note.id)) {
                note.scheduledRangeString?.let {
                    addInstances(TimeType.SCHEDULED, it)
                }
                note.deadlineRangeString?.let {
                    addInstances(TimeType.DEADLINE, it)
                }

                addedPlanningTimes.add(note.note.id)
            }

            // Add each note's event
            note.eventString?.let {
                addInstances(TimeType.EVENT, it)
            }
        }

        return dayBuckets.values.flatten() // FIXME
    }
}
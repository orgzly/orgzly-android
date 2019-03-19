package com.orgzly.android.util

import com.orgzly.org.OrgPatterns
import com.orgzly.org.datetime.OrgRange
import java.util.*

class EventsInNote(private val originalTitle: String, private val originalContent: String?) {

    private val eventsInTitle: List<EventPosition>
    private val eventsInContent: List<EventPosition>

    val timestamps: List<OrgRange>

    init {
        eventsInTitle = parseString(originalTitle)
        eventsInContent = parseString(originalContent)

        timestamps = eventsInTitle.map { it.event } + eventsInContent.map { it.event }
    }

    private fun parseString(str: String?): List<EventPosition> {
        return ArrayList<EventPosition>().apply {
            if (!str.isNullOrEmpty()) {
                val m = OrgPatterns.DT_OR_RANGE_P.matcher(str)
                while (m.find()) {
                    val range = OrgRange.parse(m.group())
                    if (range.startTime.isActive) {
                        add(EventPosition(range, m.start(), m.end()))
                    }
                }
            }
        }
    }

    fun replaceEvents(modifiedEvents: List<OrgRange>): Pair<String, String?> {
        var i = modifiedEvents.size - 1

        val modifiedTitle =
                StringBuilder(originalTitle).also {
                    for (j in eventsInTitle.size - 1 downTo 0) {
                        val originalEvent = eventsInTitle[j]
                        val modifiedEvent = modifiedEvents[i].toString()

                        if (originalEvent.event.toString() != modifiedEvent) {
                            it.replace(originalEvent.start, originalEvent.end, modifiedEvent)
                        }

                        i--
                    }
                }.toString()


        val modifiedContent = if (originalContent != null) {
            StringBuilder(originalContent).also {
                for (j in eventsInContent.size - 1 downTo 0) {
                    val originalEvent = eventsInContent[j]
                    val modifiedEvent = modifiedEvents[i].toString()

                    if (originalEvent.event.toString() != modifiedEvent) {
                        it.replace(originalEvent.start, originalEvent.end, modifiedEvent)
                    }

                    i--
                }
            }.toString()
        } else {
            null
        }

        return Pair(modifiedTitle, modifiedContent)
    }

    data class EventPosition(val event: OrgRange, val start: Int, val end: Int)
}
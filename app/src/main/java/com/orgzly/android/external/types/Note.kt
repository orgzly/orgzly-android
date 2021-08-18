package com.orgzly.android.external.types

import com.orgzly.android.db.entity.NoteView

data class Note(
        val id: Long,
        val title: String,
        val content: String?,
        val tags: List<String>,
        val inheritedTags: List<String>,
        val bookName: String,
        val scheduled: Timestamp?,
        val deadline: Timestamp?,
        val closed: Timestamp?,
        val priority: String?,
        val state: String?,
        val createdAt: Long?

) {
    companion object {
        fun from(view: NoteView): Note {
            val note = view.note
            return Note(
                    note.id,
                    note.title,
                    note.content,
                    note.tags?.split(" +".toRegex())
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList(),
                    view.getInheritedTagsList()
                            .filter { it.isNotEmpty() },
                    view.bookName,
                    Timestamp.from(
                            view.scheduledTimeTimestamp,
                            view.scheduledTimeString,
                            view.scheduledTimeEndString,
                            view.scheduledRangeString
                    ),
                    Timestamp.from(
                            view.deadlineTimeTimestamp,
                            view.deadlineTimeString,
                            view.deadlineTimeEndString,
                            view.deadlineRangeString
                    ),
                    Timestamp.from(
                            view.closedTimeTimestamp,
                            view.closedTimeString,
                            view.closedTimeEndString,
                            view.closedRangeString
                    ),
                    note.priority,
                    note.state,
                    note.createdAt
            )
        }
    }

    data class Timestamp(
            val timeTimestamp: Long,
            val timeString: String,
            val timeEndString: String? = null,
            val rangeString: String?,
    ) {
        companion object {
            fun from(
                    timeTimestamp: Long?,
                    timeString: String?,
                    timeEndString: String?,
                    rangeString: String?
            ): Timestamp? {
                return if (timeTimestamp != null && timeString != null)
                    Timestamp(timeTimestamp, timeString, timeEndString, rangeString)
                else null
            }
        }
    }
}
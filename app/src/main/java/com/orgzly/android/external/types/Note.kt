package com.orgzly.android.external.types

import com.orgzly.android.db.entity.NoteProperty
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
        val createdAt: Long?,
        val properties: Map<String, String>
) {
    companion object {
        fun from(view: NoteView, props: List<NoteProperty>): Note {
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
                    view.scheduledRangeString,
                    view.scheduledTimeTimestamp,
                    view.scheduledTimeString,
                    view.scheduledTimeEndString,
                ),
                Timestamp.from(
                    view.deadlineRangeString,
                    view.deadlineTimeTimestamp,
                    view.deadlineTimeString,
                    view.deadlineTimeEndString,
                ),
                Timestamp.from(
                    view.closedRangeString,
                    view.closedTimeTimestamp,
                    view.closedTimeString,
                    view.closedTimeEndString,
                ),
                note.priority,
                note.state,
                note.createdAt,
                props.associate { it.name to it.value }
            )
        }

        fun from(noteAndProps: Pair<NoteView, List<NoteProperty>>) =
            from(noteAndProps.first, noteAndProps.second)
    }

    data class Timestamp(
        val rangeString: String,
        val timeTimestamp: Long,
        val timeString: String?,
        val timeEndString: String? = null,
    ) {
        companion object {
            fun from(
                rangeString: String?,
                timeTimestamp: Long?,
                timeString: String?,
                timeEndString: String?,
            ): Timestamp? {
                return if (rangeString != null && timeTimestamp != null)
                    Timestamp(rangeString, timeTimestamp, timeString, timeEndString)
                else null
            }
        }
    }
}
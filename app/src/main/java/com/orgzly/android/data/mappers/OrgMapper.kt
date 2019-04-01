package com.orgzly.android.data.mappers

import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.org.OrgHead
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgRange

object OrgMapper {
    @JvmStatic
    fun toOrgProperties(from: Collection<NoteProperty>): OrgProperties {
        val to = OrgProperties()

        from.forEach { property ->
            to.put(property.name, property.value)
        }

        return to
    }

    /**
     * [OrgHead] without properties.
     */
    fun toOrgHead(noteView: NoteView): OrgHead {
        val note = noteView.note

        return OrgHead().apply {
            title = note.title

            setTags(Note.dbDeSerializeTags(note.tags).toTypedArray())

            state = note.state

            priority = note.priority

            scheduled = noteView.scheduledRangeString?.let { OrgRange.parse(it) }
            deadline = noteView.deadlineRangeString?.let { OrgRange.parse(it) }
            closed = noteView.closedRangeString?.let { OrgRange.parse(it) }
            clock = noteView.clockRangeString?. let { OrgRange.parse(it) }

            content = note.content
        }
    }

    fun toOrgHead(notePayload: NotePayload): OrgHead {
        return OrgHead().apply {
            title = notePayload.title

            setTags(notePayload.tags.toTypedArray())

            state = notePayload.state

            priority = notePayload.priority

            scheduled = notePayload.scheduled?.let { OrgRange.parse(it) }
            deadline = notePayload.deadline?.let { OrgRange.parse(it) }
            closed = notePayload.closed?.let { OrgRange.parse(it) }

            properties = notePayload.properties

            content = notePayload.content
        }
    }
}
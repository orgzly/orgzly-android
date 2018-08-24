package com.orgzly.android.data.mappers

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.org.OrgHead
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange

class OrgMapper(val dataRepository: DataRepository) {
    fun forEachOrgHead(
            bookName: String,
            createdAtPropertyName: String,
            useCreatedAtProperty: Boolean,
            action: (head: OrgHead, level: Int) -> Any) {

        dataRepository.getNotes(bookName).forEach { noteView ->
            val note = noteView.note

            val head = toOrgHead(noteView).apply {
                properties = toOrgProperties(dataRepository.getNoteProperties(note.id))

                // Update note properties with created-at property, if created-at time exists
                if (useCreatedAtProperty && note.createdAt != null && note.createdAt > 0) {
                    val time = OrgDateTime(note.createdAt, false)
                    addProperty(createdAtPropertyName, time.toString())
                }
            }

            action(head, note.position.level)
        }
    }

    companion object {
        @JvmStatic
        fun toOrgProperties(from: Collection<NoteProperty>): OrgProperties {
            val to = OrgProperties()

            from.forEach { property ->
                to[property.name] = property.value
            }

            return to
        }

        @JvmStatic
        fun toOrgProperties(from: Map<String, String>): OrgProperties {
            val to = OrgProperties()

            from.forEach { (name, value) ->
                to[name] = value
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

                properties = toOrgProperties(notePayload.properties)

                content = notePayload.content
            }
        }
    }
}
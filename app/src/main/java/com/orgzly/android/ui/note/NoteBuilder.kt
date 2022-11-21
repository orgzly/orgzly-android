package com.orgzly.android.ui.note

import android.content.Context
import android.net.Uri
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.NoteStates
import com.orgzly.android.util.EventsInNote
import com.orgzly.android.util.OrgFormatter
import com.orgzly.org.OrgProperties
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.utils.StateChangeLogic
import java.util.*


class NoteBuilder {

    companion object {

        @JvmStatic
        fun changeState(context: Context, notePayload: NotePayload, state: String?): NotePayload {

            val doneKeywords = AppPreferences.doneKeywordsSet(context)

            var title = notePayload.title
            var content = notePayload.content
            val properties = notePayload.properties

            val eventsInNote = EventsInNote(title, content)

            val scl = StateChangeLogic(doneKeywords)

            scl.setState(
                    state,
                    notePayload.state,
                    OrgRange.parseOrNull(notePayload.scheduled),
                    OrgRange.parseOrNull(notePayload.deadline),
                    eventsInNote.timestamps.map { OrgRange(it) })


            if (scl.isShifted) {
                eventsInNote.replaceEvents(scl.timestamps).apply {
                    title = first
                    content = second
                }

                val now = OrgDateTime(false).toString()

                // Set last-repeat property
                if (AppPreferences.setLastRepeatOnTimeShift(context)) {
                    properties.set(OrgFormatter.LAST_REPEAT_PROPERTY, now)
                }

                // Log state change
                if (AppPreferences.logOnTimeShift(context)) {
                    val logEntry = OrgFormatter.stateChangeLine(notePayload.state, state, now)
                    content = OrgFormatter.insertLogbookEntryLine(content, logEntry)
                }
            }

            return notePayload.copy(
                    title = title,
                    content = content,
                    state = scl.state,
                    scheduled = scl.scheduled?.toString(),
                    deadline = scl.deadline?.toString(),
                    closed = scl.closed?.toString(),
                    properties = properties
            )
        }


//        fun newRootNote(bookId: Long): Note {
//            val note = Note()
//
//            val position = note.position
//
//            position.setBookId(bookId)
//            position.setLevel(0)
//            position.setLft(1)
//            position.setRgt(2)
//
//            return note
//        }


        fun newPayload(noteView: NoteView, properties: List<NoteProperty>): NotePayload {
            return NotePayload(
                    noteView.note.title,
                    noteView.note.content,
                    noteView.note.state,
                    noteView.note.priority,
                    noteView.scheduledRangeString,
                    noteView.deadlineRangeString,
                    noteView.closedRangeString,
                    noteView.note.getTagsList(),
                    OrgProperties().apply { properties.forEach { put(it.name, it.value) } })
        }

        @JvmStatic
        fun newPayload(context: Context, title: String, content: String?, attachmentUri: Uri?): NotePayload {

            val scheduled = initialScheduledTime(context)

            val state = initialState(context)

            return NotePayload(
                    title = title,
                    content = content,
                    state = state,
                    scheduled = scheduled,
                    attachmentUri = attachmentUri
            )
        }

        private fun initialScheduledTime(context: Context): String? {
            return if (AppPreferences.isNewNoteScheduled(context)) {
                val cal = Calendar.getInstance()

                val timestamp = OrgDateTime.Builder()
                        .setIsActive(true)
                        .setYear(cal.get(Calendar.YEAR))
                        .setMonth(cal.get(Calendar.MONTH))
                        .setDay(cal.get(Calendar.DAY_OF_MONTH))
                        .build()

                OrgRange(timestamp).toString()

            } else {
                null
            }
        }

        private fun initialState(context: Context): String? {
            AppPreferences.newNoteState(context).let {
                return if (NoteStates.isKeyword(it)) {
                    it
                } else {
                    null
                }
            }
        }

        private val TAG = NoteBuilder::class.java.name
    }
}
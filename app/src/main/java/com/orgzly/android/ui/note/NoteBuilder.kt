package com.orgzly.android.ui.note

import android.content.Context
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.NoteStates
import com.orgzly.android.util.OrgFormatter
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.utils.StateChangeLogic
import java.util.*

class NoteBuilder {

    companion object {
        @JvmStatic
        fun withState(context: Context, notePayload: NotePayload, state: String?): NotePayload {
            val originalState = notePayload.state

            val doneKeywords = AppPreferences.doneKeywordsSet(context)

            val stateChangeLogic = StateChangeLogic(doneKeywords)

            stateChangeLogic.setState(
                    state,
                    notePayload.state,
                    OrgRange.parseOrNull(notePayload.scheduled),
                    OrgRange.parseOrNull(notePayload.deadline))

            val datetime = OrgDateTime(false).toString()

            // Add last-repeat time
            val properties = notePayload.properties.toMutableMap()
            if (stateChangeLogic.isShifted && AppPreferences.setLastRepeatOnTimeShift(context)) {
                properties[OrgFormatter.LAST_REPEAT_PROPERTY] = datetime
            }

            // Log state change
            val content =
                    if (stateChangeLogic.isShifted && AppPreferences.logOnTimeShift(context)) {
                        val logEntry = OrgFormatter.stateChangeLine(originalState, state, datetime)
                        OrgFormatter.insertLogbookEntryLine(notePayload.content, logEntry)
                    } else {
                        notePayload.content
                    }


            return notePayload.copy(
                    state = stateChangeLogic.state,
                    scheduled = stateChangeLogic.scheduled?.toString(),
                    deadline = stateChangeLogic.deadline?.toString(),
                    closed = stateChangeLogic.closed?.toString(),
                    properties = properties,
                    content = content
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
                    linkedMapOf(*properties.map { Pair(it.name, it.value) }.toTypedArray())
            )
        }

        @JvmStatic
        fun newPayload(context: Context, title: String, content: String?): NotePayload {

            val scheduled = initialScheduledTime(context)

            val state = initialState(context)

            return NotePayload(
                    title = title,
                    content = content,
                    state = state,
                    scheduled = scheduled
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
    }
}
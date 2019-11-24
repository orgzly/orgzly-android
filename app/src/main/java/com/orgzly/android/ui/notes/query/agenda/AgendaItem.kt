package com.orgzly.android.ui.notes.query.agenda

import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.TimeType
import org.joda.time.DateTime

sealed class AgendaItem(open val id: Long) {
    data class Note(
            override val id: Long,
            val note: NoteView,
            val timeType: TimeType,
            val isWarning: Boolean = false
    ) : AgendaItem(id)

    data class Divider(override val id: Long, val day: DateTime) : AgendaItem(id)

}
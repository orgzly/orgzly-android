package com.orgzly.android

import com.orgzly.android.data.DataRepository
import java.io.StringWriter

data class NotesClipboard(val content: String, val count: Int? = null) {
    fun toOrg(): String {
        return content
    }

    companion object {
        fun getOrgInstance(dataRepository: DataRepository, ids: Set<Long>): NotesClipboard {
            val str = StringWriter()

            val exporter = NotesOrgExporter(App.getAppContext(), dataRepository)

            val exportedCount = exporter.exportSubtreesAligned(ids, str)

            return NotesClipboard(str.toString(), exportedCount)
        }

        fun fromOrgString(str: String): NotesClipboard {
            return NotesClipboard(str)
        }
    }
}
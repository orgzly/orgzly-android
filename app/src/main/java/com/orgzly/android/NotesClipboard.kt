package com.orgzly.android

import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.MiscUtils
import java.io.File
import java.io.StringWriter

data class NotesClipboard(val noteCount: Int, val content: String) {

    fun toOrg(): String {
        return content
    }

    fun save() {
        try {
            MiscUtils.writeStringToFile(content, dataFile())
            AppPreferences.notesClipboard(App.getAppContext(), "$noteCount")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun create(dataRepository: DataRepository, ids: Set<Long>): NotesClipboard {
            val str = StringWriter()

            val exporter = NotesOrgExporter(App.getAppContext(), dataRepository)

            val exportedCount = exporter.exportSubtreesAligned(ids, str)

            return NotesClipboard(exportedCount, str.toString())
        }

        fun load(): NotesClipboard? {
            val pref = AppPreferences.notesClipboard(App.getAppContext())

            if (pref != null) {
                try {
                    val count = Integer.valueOf(pref)
                    val data = MiscUtils.readStringFromFile(dataFile())
                    return NotesClipboard(count, data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return null
        }

        private fun dataFile(): File {
            return File(App.getAppContext().filesDir, "clipboard.org")
        }
    }
}
package com.orgzly.android.db

import com.google.gson.Gson
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.MiscUtils
import java.io.File
import java.io.StringWriter
import java.io.Writer

data class NotesClipboard(val noteCount: Int, val content: String) {
    fun save() {
        try {
            MiscUtils.writeStringToFile(content, dataFile())
            AppPreferences.notesClipboard(App.getAppContext(), "$noteCount")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNotes(): List<Note> {
        return Gson().fromJson(content, Array<Note>::class.java).toMutableList()
    }

    companion object {
        fun create(dataRepository: DataRepository, ids: Set<Long>): NotesClipboard {
            val str = StringWriter()

            val exportedCount = exportSubtreesAligned(dataRepository, ids, str)

            return NotesClipboard(exportedCount, str.toString())
        }

        private fun exportSubtreesAligned(dataRepository: DataRepository, ids: Set<Long>, writer: Writer): Int {
            var offset = 0L
            var subtreeRgt = 0L
            var levelOffset = 0

            val notes = dataRepository.getSubtrees(ids).map { noteView ->
                val note = noteView.note

                // First note or next subtree
                if (subtreeRgt == 0L || note.position.rgt > subtreeRgt) {
                    offset += note.position.lft - subtreeRgt - 1
                    levelOffset = note.position.level - 1
                    subtreeRgt = note.position.rgt
                }

                note.copy(
                        position = note.position.copy(
                                level = note.position.level - levelOffset,
                                lft = note.position.lft - offset,
                                rgt = note.position.rgt - offset
                        ))
            }

            Gson().toJson(notes, writer)

            return notes.size
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
            return File(App.getAppContext().filesDir, "clipboard.json")
        }
    }
}
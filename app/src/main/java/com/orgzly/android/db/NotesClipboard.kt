package com.orgzly.android.db

import com.google.gson.Gson
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Note
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.MiscUtils
import java.io.File
import java.io.StringWriter

data class NotesClipboard(val notes: List<Note>) {

    val count: Int
        get() = notes.count()


    fun save() {
        try {
            val writer = StringWriter()
            Gson().toJson(notes, writer)
            val content = writer.toString()

            MiscUtils.writeStringToFile(content, dataFile())

            AppPreferences.notesClipboard(App.getAppContext(), "$count")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        fun create(dataRepository: DataRepository, ids: Set<Long>): NotesClipboard {
            var offset = 0L
            var subtreeRgt = 0L
            var levelOffset = 0

            val notes = dataRepository.getSubtrees(ids).map { note ->
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

            return NotesClipboard(notes)
        }

        fun load(): NotesClipboard? {
            val pref = AppPreferences.notesClipboard(App.getAppContext())

            if (pref != null) {
                try {
                    val data = MiscUtils.readStringFromFile(dataFile())

                    val notes = Gson().fromJson(data, Array<Note>::class.java).toMutableList()

                    return NotesClipboard(notes)
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
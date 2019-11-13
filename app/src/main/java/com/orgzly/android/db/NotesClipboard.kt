package com.orgzly.android.db

import com.google.gson.Gson
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteProperty
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.MiscUtils
import java.io.File
import java.io.StringWriter

data class NotesClipboard(val entries: List<Entry> = emptyList()) {

    data class Entry(
            val note: Note,
            val properties: List<NoteProperty>
    )

    val count: Int
        get() = entries.count()


    fun save() {
        try {
            val writer = StringWriter()
            Gson().toJson(entries, writer)
            val content = writer.toString()

            MiscUtils.writeStringToFile(content, dataFile())

            AppPreferences.notesClipboard(App.getAppContext(), "$count")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        fun count(): Int {
            return AppPreferences.notesClipboard(App.getAppContext())?.toInt() ?: 0
        }

        fun create(dataRepository: DataRepository, ids: Set<Long>): NotesClipboard {
            val alignedNotes = dataRepository.getSubtreesAligned(ids).map { note ->
                Entry(note, dataRepository.getNoteProperties(note.id))
            }

            return NotesClipboard(alignedNotes)
        }

        fun load(): NotesClipboard {
            if (count() > 0) {
                try {
                    val data = MiscUtils.readStringFromFile(dataFile())

                    val notes = Gson().fromJson(data, Array<Entry>::class.java).toMutableList()

                    return NotesClipboard(notes)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return NotesClipboard()
        }

        @JvmStatic
        fun clear() {
            AppPreferences.notesClipboard(App.getAppContext(), null)

            dataFile().delete()
        }

        private fun dataFile(): File {
            return File(App.getAppContext().filesDir, "clipboard.json")
        }
    }
}
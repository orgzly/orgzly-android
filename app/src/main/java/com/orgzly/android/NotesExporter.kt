package com.orgzly.android

import android.content.Context
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.provider.clients.NotesClient
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.parser.OrgParserSettings
import com.orgzly.org.parser.OrgParserWriter
import java.io.*
import java.nio.charset.Charset


class NotesExporter private constructor(
        val context: Context, val format: BookName.Format = BookName.Format.ORG) {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun getInstance(context: Context, format: BookName.Format = BookName.Format.ORG): NotesExporter {
            return NotesExporter(context, format)
        }
    }

    /**
     * Writes content of the book from database to a specified file.
     */
    @Throws(IOException::class)
    fun exportBook(book: Book, file: File) {
        val encoding = book.usedEncoding ?: Charset.defaultCharset().name()

        PrintWriter(file, encoding).use {
            exportBook(book, it)
        }
    }

    @Throws(IOException::class)
    fun exportBook(book: Book, writer: Writer) {
        val createdAtPropertyName = AppPreferences.createdAtProperty(context)
        val useCreatedAtProperty = AppPreferences.createdAt(context)

        val parserSettings = getOrgParserSettingsFromPreferences()
        val orgWriter = OrgParserWriter(parserSettings)

        // Write preface
        writer.write(orgWriter.whiteSpacedFilePreface(book.preface))

        // Write each note
        NotesClient.forEachBookNote(context, book.name) { note ->
            // Update note properties with created-at property, if created-at time exists
            if (useCreatedAtProperty && createdAtPropertyName != null && note.createdAt > 0) {
                val time = OrgDateTime(note.createdAt, false)
                note.head.addProperty(createdAtPropertyName, time.toString())
            }

            writer.write(orgWriter.whiteSpacedHead(
                    note.head,
                    note.position.level,
                    book.orgFileSettings.isIndented))
        }
    }

    private fun getOrgParserSettingsFromPreferences(): OrgParserSettings {
        val parserSettings = OrgParserSettings.getBasic()

        when (AppPreferences.separateNotesWithNewLine(context)) {
            context.getString(R.string.pref_value_separate_notes_with_new_line_always) ->
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.ALWAYS

            context.getString(R.string.pref_value_separate_notes_with_new_line_multi_line_notes_only) ->
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.MULTI_LINE_NOTES_ONLY

            context.getString(R.string.pref_value_separate_notes_with_new_line_never) ->
                parserSettings.separateNotesWithNewLine = OrgParserSettings.SeparateNotesWithNewLine.NEVER
        }

        parserSettings.separateHeaderAndContentWithNewLine =
                AppPreferences.separateHeaderAndContentWithNewLine(context)

        parserSettings.tagsColumn = AppPreferences.tagsColumn(context)

        parserSettings.orgIndentMode = AppPreferences.orgIndentMode(context)

        parserSettings.orgIndentIndentationPerLevel = AppPreferences.orgIndentIndentationPerLevel(context)

        return parserSettings
    }
}
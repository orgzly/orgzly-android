package com.orgzly.android

import android.content.Context
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.mappers.OrgMapper
import com.orgzly.android.db.entity.Book
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.org.parser.OrgParserSettings
import com.orgzly.org.parser.OrgParserWriter
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.Charset

class NotesExporter @JvmOverloads constructor(
        val context: Context,
        val dataRepository: DataRepository,
        val format: BookFormat = BookFormat.ORG) {

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
        val parserSettings = getOrgParserSettingsFromPreferences()
        val orgWriter = OrgParserWriter(parserSettings)

        // Write preface
        writer.write(orgWriter.whiteSpacedFilePreface(book.preface))

        // Write each note
        OrgMapper(dataRepository).forEachOrgHead(book.name) { head, level ->
            writer.write(orgWriter.whiteSpacedHead(head, level, book.isIndented == true))
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
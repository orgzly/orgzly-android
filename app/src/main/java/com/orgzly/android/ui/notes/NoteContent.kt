package com.orgzly.android.ui.notes

/**
 * Represents a subsection of note content: either text, or a table
 */
sealed class NoteContent {

    abstract val text: String

    data class TextNoteContent(override val text: String) : NoteContent() {
    }

    data class TableNoteContent(override val text: String) : NoteContent() {

        fun reformat() {
            // placeholder - but would fix all the spacing, missing cells, etc. Complicated
        }
    }


    companion object {

        private fun lineIsTable(raw: String) = raw.isNotEmpty() && raw[0] == '|'

        /**
         * Converts the provided raw string  (with embedded newlines) into a list of sections of
         * either text or tables. Each section is contiguous and can contain newlines.
         *
         * This is horrible, never try to write your own parser. Consider using a regex instead.
         */
        fun parse(raw: String): List<NoteContent> {
            val list: MutableList<NoteContent> = mutableListOf()

            var currentText = ""
            var currentTable = ""

            var previousIsTable: Boolean = this.lineIsTable(raw)

            val rawSplitByNewlines = raw.split("\n")

            val missingLastNewline = rawSplitByNewlines.last() != ""

            val linesForParsing =
                    if (missingLastNewline) {
                        rawSplitByNewlines
                    } else {
                        rawSplitByNewlines.dropLast(1)
                    }

            linesForParsing.forEach {
                val currentIsTable = lineIsTable(it)
                when {
                    currentIsTable && previousIsTable -> {
                        currentTable += it + "\n"
                    }
                    currentIsTable && !previousIsTable -> {
                        currentTable = it + "\n"
                        list.add(TextNoteContent(currentText))
                        currentText = ""
                    }
                    !currentIsTable && previousIsTable -> {
                        currentText = it + "\n"
                        list.add(TableNoteContent(currentTable))
                        currentTable = ""
                    }
                    !currentIsTable && !previousIsTable -> {
                        currentText += it + "\n"
                    }
                }
                previousIsTable = currentIsTable
            }

            if (linesForParsing.isNotEmpty()) {
                if (previousIsTable) {
                    list.add(TableNoteContent(if (missingLastNewline) {
                        currentTable.dropLast(1)
                    } else currentTable))
                } else {
                    list.add(TextNoteContent(if (missingLastNewline) {
                        currentText.dropLast(1)
                    } else currentText))
                }
            }

            return list
        }
    }
}
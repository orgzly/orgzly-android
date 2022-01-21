package com.orgzly.android.ui.notes

/**
 * Represents a subsection of note content: either text, or a table
 */
data class NoteContent(val text: String, val startOffset: Int, val endOffset: Int, val textType: TextType) {

    enum class TextType {
        TEXT, TABLE
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
                        val startOffset = getLastOffset(list)
                        list.add(NoteContent(currentText, startOffset, startOffset + currentText.length - 1, TextType.TEXT))
                        currentText = ""
                    }
                    !currentIsTable && previousIsTable -> {
                        currentText = it + "\n"
                        val startOffset = getLastOffset(list)
                        list.add(NoteContent(currentTable, startOffset, startOffset + currentTable.length - 1, TextType.TABLE))
                        currentTable = ""
                    }
                    !currentIsTable && !previousIsTable -> {
                        currentText += it + "\n"
                    }
                }
                previousIsTable = currentIsTable
            }

            if (linesForParsing.isNotEmpty()) {

                val endOffsetAdjustment = if (missingLastNewline) 2 else 1

                if (previousIsTable) {
                    list.add(NoteContent(if (missingLastNewline) {
                        currentTable.dropLast(1)
                    } else currentTable, getLastOffset(list), getLastOffset(list) + currentTable.length - endOffsetAdjustment, TextType.TABLE))
                } else {
                    list.add(NoteContent(if (missingLastNewline) {
                        currentText.dropLast(1)
                    } else currentText, getLastOffset(list), getLastOffset(list) + currentText.length - endOffsetAdjustment, TextType.TEXT))
                }
            }

            return list
        }

        private fun getLastOffset(list: MutableList<NoteContent>) = if (list.isEmpty()) 0 else list.last().endOffset + 1
    }
}
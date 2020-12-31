package com.orgzly.android.ui.notes

import com.orgzly.android.ui.notes.NoteContent.TableNoteContent
import com.orgzly.android.ui.notes.NoteContent.TextNoteContent
import org.hamcrest.Matchers.emptyCollectionOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.Random

// TODO - check CRLF vs LF vs whatever MacOS does
class NoteContentTest {

    @Test
    fun emptyString() {
        val parse = NoteContent.parse("")
        assertThat(parse, (emptyCollectionOf(NoteContent.javaClass)))
    }

    @Test
    fun emptyLinesShouldStayInSingleSection() {
        checkExpected("\n\n", listOf(TextNoteContent("\n\n")))
    }

    @Test
    fun pipeInText() {
        checkExpected("""foo
|

foo|bar""", listOf(
                TextNoteContent("foo\n"),
                TableNoteContent("|\n"),
                TextNoteContent("\nfoo|bar")
        ))
    }

    @Test
    fun singleTable() {
        checkExpected("""|a|b|
|c|d|
""", listOf(TableNoteContent("""|a|b|
|c|d|
""")))
    }

    @Test
    fun singleTableNoFinalNewline() {
        checkExpected("""|a|b|
|c|d|""", listOf(TableNoteContent("""|a|b|
|c|d|""")))
    }

    @Test
    fun singleLineTextTableText() {
        checkExpected("""foo
|
bar""", listOf(
                TextNoteContent("foo\n"),
                TableNoteContent("|\n"),
                TextNoteContent("bar")
        ))
    }


    @Test
    fun blankLineTextTableText() {
        checkExpected("""
|
bar
""", listOf(
                TextNoteContent("\n"),
                TableNoteContent("|\n"),
                TextNoteContent("bar\n")
        ))
    }

    @Test
    fun tableBlankLineTable() {
        checkExpected("""|zoo|

|zog|""", listOf(
                TableNoteContent("|zoo|\n"),
                TextNoteContent("\n"),
                TableNoteContent("|zog|")
        ))
    }

    @Test
    fun textTableBlankLineText() {
        checkExpected("""foo
|

chops""", listOf(
                TextNoteContent("foo\n"),
                TableNoteContent("|\n"),
                TextNoteContent("\nchops")
        ))
    }


    @Test
    fun textTableTextTableText() {
        checkExpected("""text1
|table2a|
|table2b|
text3a
text3b
text3c
|table4|
text5
""", listOf(
                TextNoteContent("text1\n"),
                TableNoteContent("|table2a|\n|table2b|\n"),
                TextNoteContent("text3a\ntext3b\ntext3c\n"),
                TableNoteContent("|table4|\n"),
                TextNoteContent("text5\n")
        ))
    }

    @Test
    fun randomStringsRoundTrip() {

        val stringAtoms: List<String> = listOf("\n", "a", "|")

        for (i in 0..1000) {
            val rawStringLength = Random().nextInt(100)
            val builder = StringBuilder()
            for (j in 0..rawStringLength) {
                builder.append(stringAtoms.random())
            }

            val raw = builder.toString()

            val actual: List<NoteContent> = NoteContent.parse(raw)

            val roundTripped: String = actual.fold("") { acc: String, current: NoteContent -> acc + current.text }

            assertEquals(raw, roundTripped)

        }

    }


    private fun checkExpected(input: String, expected: List<NoteContent>) {
        val actual: List<NoteContent> = NoteContent.parse(input)
        assertEquals(expected, actual)

        val roundTripped: String = actual.fold("") { acc: String, current: NoteContent -> acc + current.text }

        assertEquals(input, roundTripped)

    }
}

package com.orgzly.android.ui.notes

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
        checkExpected("\n\n", listOf(NoteContent("\n\n", 0, 1, NoteContent.TextType.TEXT)))
    }

    @Test
    fun pipeInText() {
        checkExpected("""foo
|

foo|bar""", listOf(
                NoteContent("foo\n", 0, 3, NoteContent.TextType.TEXT),
                NoteContent("|\n", 4, 5, NoteContent.TextType.TABLE),
                NoteContent("\nfoo|bar", 6, 13, NoteContent.TextType.TEXT)
        ))
    }

    @Test
    fun singleTable() {
        checkExpected("""|a|b|
|c|d|
""", listOf(NoteContent("""|a|b|
|c|d|
""", 0, 11, NoteContent.TextType.TABLE)))
    }

    @Test
    fun singleTableNoFinalNewline() {
        checkExpected("""|a|b|
|c|d|""", listOf(NoteContent("""|a|b|
|c|d|""", 0, 10, NoteContent.TextType.TABLE)))
    }

    @Test
    fun singleLineTextTableText() {
        checkExpected("""foo
|
bar""", listOf(
                NoteContent("foo\n", 0, 3, NoteContent.TextType.TEXT),
                NoteContent("|\n", 4, 5, NoteContent.TextType.TABLE),
                NoteContent("bar", 6, 8, NoteContent.TextType.TEXT)
        ))
    }


    @Test
    fun blankLineTextTableText() {
        checkExpected("""
|
bar
""", listOf(
                NoteContent("\n", 0, 0, NoteContent.TextType.TEXT),
                NoteContent("|\n", 1, 2, NoteContent.TextType.TABLE),
                NoteContent("bar\n", 3, 6, NoteContent.TextType.TEXT)
        ))
    }

    @Test
    fun tableBlankLineTable() {
        checkExpected("""|zoo|

|zog|""", listOf(
                NoteContent("|zoo|\n", 0, 5, NoteContent.TextType.TABLE),
                NoteContent("\n", 6, 6, NoteContent.TextType.TEXT),
                NoteContent("|zog|", 7, 11, NoteContent.TextType.TABLE)
        ))
    }

    @Test
    fun textTableBlankLineText() {
        checkExpected("""foo
|

chops""", listOf(
                NoteContent("foo\n", 0, 3, NoteContent.TextType.TEXT),
                NoteContent("|\n", 4, 5, NoteContent.TextType.TABLE),
                NoteContent("\nchops", 6, 11, NoteContent.TextType.TEXT)
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
                NoteContent("text1\n", 0, 5, NoteContent.TextType.TEXT),
                NoteContent("|table2a|\n|table2b|\n", 6, 25, NoteContent.TextType.TABLE),
                NoteContent("text3a\ntext3b\ntext3c\n", 26, 46, NoteContent.TextType.TEXT),
                NoteContent("|table4|\n", 47, 55, NoteContent.TextType.TABLE),
                NoteContent("text5\n", 56, 61, NoteContent.TextType.TEXT)
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

        actual.forEach {
            assertEquals(it.text, input.substring(it.startOffset, it.endOffset + 1))
        }
    }
}

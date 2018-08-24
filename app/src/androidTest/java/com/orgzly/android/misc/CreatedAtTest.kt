package com.orgzly.android.misc

import com.orgzly.R
import com.orgzly.android.BookFormat
import com.orgzly.android.LocalStorage
import com.orgzly.android.NotesExporter
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.org.datetime.OrgDateTime
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CreatedAtTest : OrgzlyTest() {
    @Test
    fun testImportUsesCreatedAtValue() {
        AppPreferences.createdAt(context, true)

        val createdProperty = context.getString(R.string.created_property_name)

        testUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":" + createdProperty + ": [2018-01-01 12:00]\n" +
                        ":END:\n")

        val note = dataRepository.getNote("Note [a-1]")

        assertEquals(
                OrgDateTime.parse("[2018-01-01 12:00]").calendar.timeInMillis,
                note?.createdAt)
    }


    /* Test that CREATED property is not added twice. */
    @Test
    fun testExportSetsOneCreatedAtProperty() {
        AppPreferences.createdAt(context, true)

        val book = testUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":END:\n")

        withTempFile { file ->
            NotesExporter(context, dataRepository).exportBook(book.book, file)

            dataRepository.loadBookFromFile("book-a", BookFormat.ORG, file)

            val note = dataRepository.getNote("Note [a-1]")

            assertEquals(1, dataRepository.getNoteProperties(note!!.id).size)
        }
    }


    @Test
    fun testBookOutOfSyncAfterDifferentCreatedAtPropertyName() {
        AppPreferences.createdAt(context, true)

        val createdProperty = context.getString(R.string.created_property_name)

        testUtils.setupRepo("mock://repo-a")
        testUtils.setupRook(
                "mock://repo-a",
                "mock://repo-a/book-a.org",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":" + createdProperty + ": [2018-01-01 12:00]\n" +
                        ":END:\n",
                "0abcdef",
                1400067156)

        testUtils.sync()

        assertFalse(dataRepository.getBookView("book-a")!!.isOutOfSync())

        AppPreferences.createdAtProperty(context, "CREATED_AT")
        dataRepository.syncCreatedAtTimeWithProperty()

        assertTrue(dataRepository.getBookView("book-a")!!.isOutOfSync())
    }

    @Test
    fun testBookMarkedSyncedAfterSettingCreatedAtTime() {
        testUtils.setupRepo("mock://repo-a")
        testUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":END:\n")
        testUtils.sync()

        AppPreferences.createdAt(context, true)

        dataRepository.syncCreatedAtTimeWithProperty()

        assertFalse(dataRepository.getBookView("book-a")!!.isOutOfSync())
    }

    @Test
    fun testParsingInvalidPropertyValue() {
        AppPreferences.createdAt(context, true)

        testUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: Invalid format\n" +
                        ":END:\n")
    }

    @Test
    fun testParsingInvalidPropertyValueWhenSyncing() {
        AppPreferences.createdAt(context, true)

        testUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":CREATED_AT: Invalid format\n" +
                        ":END:\n")

        AppPreferences.createdAtProperty(context, "CREATED_AT")

        dataRepository.syncCreatedAtTimeWithProperty()
    }

    private fun withTempFile(f: (file: File) -> Unit) {
        val file = LocalStorage(context).tempBookFile
        try {
            f(file)
        } finally {
            file.delete()
        }
    }
}

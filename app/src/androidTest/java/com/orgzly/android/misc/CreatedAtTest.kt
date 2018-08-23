package com.orgzly.android.misc

import android.support.test.rule.ActivityTestRule
import com.orgzly.R
import com.orgzly.android.BookName
import com.orgzly.android.LocalStorage
import com.orgzly.android.NotesExporter
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.MainActivity
import com.orgzly.org.datetime.OrgDateTime
import junit.framework.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class CreatedAtTest : OrgzlyTest() {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    @Test
    fun testImportUsesCreatedAtValue() {
        AppPreferences.createdAt(context, true)

        val createdProperty = context.getString(R.string.created_property_name)

        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":" + createdProperty + ": [2018-01-01 12:00]\n" +
                        ":END:\n")

        val note = shelf.getNote("Note [a-1]")

        assertEquals(
                OrgDateTime.parse("[2018-01-01 12:00]").calendar.timeInMillis,
                note.createdAt)
    }


    /* Test that CREATED property is not added twice. */
    @Test
    fun testExportSetsOneCreatedAtProperty() {
        AppPreferences.createdAt(context, true)

        val book = shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":END:\n")

        withTempFile { file ->
            NotesExporter.getInstance(context).exportBook(book, file)

            shelf.loadBookFromFile("book-a", BookName.Format.ORG, file)

            val note = shelf.getNote("Note [a-1]")

            assertEquals(1, shelf.getNoteProperties(note.id).size)
        }
    }


    @Test
    fun testBookMarkedNotSyncedAfterAddingNewProperty() {
        AppPreferences.createdAt(context, true)

        val createdProperty = context.getString(R.string.created_property_name)

        shelfTestUtils.setupRepo("mock://repo-a")
        shelfTestUtils.setupRook(
                "mock://repo-a",
                "mock://repo-a/book-a.org",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":" + createdProperty + ": [2018-01-01 12:00]\n" +
                        ":END:\n",
                "0abcdef",
                1400067156)

        shelf.sync()

        assertFalse(shelf.getBook("book-a").isModifiedAfterLastSync)

        AppPreferences.createdAtProperty(context, "CREATED_AT")
        shelf.syncCreatedAtTimeWithProperty()

        assertTrue(shelf.getBook("book-a").isModifiedAfterLastSync)
    }

    @Test
    fun testBookMarkedSyncedAfterSettingCreatedAtTime() {
        shelfTestUtils.setupRepo("mock://repo-a")
        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":END:\n")
        shelf.sync()

        AppPreferences.createdAt(context, true)

        shelf.syncCreatedAtTimeWithProperty()

        assertFalse(shelf.getBook("book-a").isModifiedAfterLastSync)
    }

    @Test
    fun testParsingInvalidPropertyValue() {
        AppPreferences.createdAt(context, true)

        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: Invalid format\n" +
                        ":END:\n")
    }

    @Test
    fun testParsingInvalidPropertyValueWhenSyncing() {
        AppPreferences.createdAt(context, true)

        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
                        ":CREATED_AT: Invalid format\n" +
                        ":END:\n")

        AppPreferences.createdAtProperty(context, "CREATED_AT")

        shelf.syncCreatedAtTimeWithProperty()
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

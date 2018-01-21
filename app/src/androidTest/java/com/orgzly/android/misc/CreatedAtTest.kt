package com.orgzly.android.misc

import android.support.test.rule.ActivityTestRule
import com.orgzly.android.BookName
import com.orgzly.android.LocalStorage
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.MainActivity
import com.orgzly.org.datetime.OrgDateTime
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class CreatedAtTest : OrgzlyTest() {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    @Test
    fun testImportUsesCreatedAtValue() {
        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CREATED: [2018-01-01 12:00]\n" +
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
            shelf.writeBookToFile(book, BookName.Format.ORG, file)
            shelf.loadBookFromFile("book-a", BookName.Format.ORG, file)

            val note = shelf.getNote("Note [a-1]")

            assertEquals(1, shelf.getNoteProperties(note.id).size)
        }
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

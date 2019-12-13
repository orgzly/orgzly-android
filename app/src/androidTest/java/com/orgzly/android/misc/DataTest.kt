package com.orgzly.android.misc

import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import org.junit.Assert.*

import org.junit.Test

import java.io.IOException

class DataTest : OrgzlyTest() {
    @Test
    fun testGetBookWithEmptyDatabase() {
        assertNull(dataRepository.getBookView(1))
    }


    @Test
    @Throws(Exception::class)
    fun testInsertBookWithExistingName() {
        dataRepository.createBook("book-01")

        try {
            dataRepository.createBook("book-01")
            fail("Second insert of the book should fail")

        } catch (e: IOException) {
            assertEquals(context.getString(R.string.book_name_already_exists, "book-01"), e.message)
        }

    }

    @Test
    fun selectAllTags() {
        testUtils.setupBook(
                "book-01",
                """
                    * Note 01-01 :tag1:tag2:
                    * Note 01-02 :tag1:tag3:
                """.trimIndent())

        assertEquals(listOf("tag1", "tag2", "tag3"), dataRepository.selectAllTags())
    }
}

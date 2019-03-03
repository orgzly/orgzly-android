package com.orgzly.android

import com.orgzly.android.usecase.NoteClipboardCut
import com.orgzly.android.usecase.UseCaseRunner
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesClipboardTest : OrgzlyTest() {
    @Test
    fun testCut() {
        val book = testUtils.setupBook(
                "Book A",
                """
                * Note A-01
                * Note A-02
                ** Note A-03
                ** Note A-04
                *** Note A-05
                """.trimIndent())

        val result = UseCaseRunner.run(NoteClipboardCut(
                book.book.id,
                setOf(
                        dataRepository.getNote("Note A-01")!!.id,
                        dataRepository.getNote("Note A-02")!!.id)))

        assertEquals(
                """
                * Note A-01
                * Note A-02
                ** Note A-03
                ** Note A-04
                *** Note A-05

                """.trimIndent(),
                (result.userData as NotesClipboard).toOrg())
    }
}
package com.orgzly.android.usecase

import com.orgzly.android.OrgzlyTest
import com.orgzly.android.repos.RepoType
import com.orgzly.org.datetime.OrgDateTime
import org.junit.Assert.*
import org.junit.Test

class NoteUpdateScheduledTimeTest : OrgzlyTest() {
    @Test
    fun book_markedAsModified() {
        testUtils.setupBook("book", "* Note")
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a")
        testUtils.sync()

        assertFalse(dataRepository.getBook("book")!!.isModified)
        assertNull(dataRepository.getBook("book")!!.mtime)

        UseCaseRunner.run(NoteUpdateScheduledTime(
                setOf(dataRepository.getLastNote("Note")!!.id),
                OrgDateTime(true)))

        assertTrue(dataRepository.getBook("book")!!.isModified)
        assertTrue(dataRepository.getBook("book")!!.mtime!! > 0)
    }
}
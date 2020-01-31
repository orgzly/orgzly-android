package com.orgzly.android.misc

import com.orgzly.android.BookFormat
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.android.usecase.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.IOException
import java.util.*

class StructureTest : OrgzlyTest() {
    @Test
    @Throws(IOException::class)
    fun testNewNote() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    Preface

                    * Note A-01
                    ** Note A-02
                """.trimIndent())

        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-03"),
                NotePlace(book.book.id)))

        Assert.assertEquals(
                """
                    Preface

                    * Note A-01
                    ** Note A-02
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(
                getRootNode(book.book.id).id,
                getNote("Note A-03").position.parentId)
    }

    @Test
    fun testBookSetupLevels() {
        val bookView = testUtils.setupBook(
                "Book A",
                """
                    Preface

                    * Note A-01
                    * Note A-02
                    ** Note A-03
                    ** Note A-04
                    *** Note A-05
                    **** Note A-06
                    ** Note A-07
                    * Note A-08
                    **** Note A-09
                    ** Note A-10
                """.trimIndent())

        Assert.assertEquals(0, getRootNode(bookView.book.id).position.level)
        Assert.assertEquals(1, getNote("Note A-01").position.level)
        Assert.assertEquals(1, getNote("Note A-02").position.level)
        Assert.assertEquals(2, getNote("Note A-03").position.level)
        Assert.assertEquals(2, getNote("Note A-04").position.level)
        Assert.assertEquals(3, getNote("Note A-05").position.level)
        Assert.assertEquals(4, getNote("Note A-06").position.level)
        Assert.assertEquals(2, getNote("Note A-07").position.level)
        Assert.assertEquals(1, getNote("Note A-08").position.level)
        Assert.assertEquals(4, getNote("Note A-09").position.level)
        Assert.assertEquals(2, getNote("Note A-10").position.level)
    }

    @Test
    fun testCut() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    Preface

                    * Note A-01
                    * Note A-02
                    ** Note A-03
                    ** Note A-04
                    *** Note A-05
                    **** Note A-06
                    ** Note A-07
                    * Note A-08
                    **** Note A-09
                    ** Note A-10
                """.trimIndent())

        val ids = setOf(
                getNote("Note A-01").id,
                getNote("Note A-03").id)

        UseCaseRunner.run(NoteCut(book.book.id, ids))

        Assert.assertEquals(8, dataRepository.getNoteCount(book.book.id))

        val notes = dataRepository.getNotes(book.book.name)

        Assert.assertEquals("Title for book should match", "Note A-02", notes[0].note.title)
        Assert.assertEquals("Level for book should match", 1, notes[0].note.position.level)
        Assert.assertEquals("Title for book should match", "Note A-04", notes[1].note.title)
        Assert.assertEquals("Level for book should match", 2, notes[1].note.position.level)
        Assert.assertEquals("Title for book should match", "Note A-05", notes[2].note.title)
        Assert.assertEquals("Level for book should match", 3, notes[2].note.position.level)
    }

    @Test
    @Throws(IOException::class)
    fun testPasteToDifferentBook() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                """.trimIndent())

        val (book) = testUtils.setupBook(
                "Book B",
                """
                     * Note B-01
                     ** Note B-02 
                     *** Note B-03
                 """.trimIndent())

        UseCaseRunner.run(NoteCut(book.id, setOf(getNote("Note B-02").id)))
        val n = getNote("Note A-03")
        UseCaseRunner.run(NotePaste(n.position.bookId, n.id, Place.UNDER))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    **** Note B-02
                    ***** Note B-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(
                """
                    * Note B-01

                """.trimIndent(),
                dataRepository.getBookContent("Book B", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testRefileToDifferentBook() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                """.trimIndent())

        val (bookB) = testUtils.setupBook(
                "Book B",
                """
                    * Note B-01
                    ** Note B-02
                    *** Note B-03
                """.trimIndent())

        UseCaseRunner.run(NoteRefile(
                setOf(getNote("Note A-02").id),
                NotePlace(bookB.id)))

        Assert.assertEquals(
                "* Note A-01\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(
                """
                    * Note B-01
                    ** Note B-02
                    *** Note B-03
                    * Note A-02
                    ** Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book B", BookFormat.ORG))

        Assert.assertTrue(getBook("Book B").isModified)
        Assert.assertTrue(getBook("Book A").isModified)
    }

    @Test
    fun testDescendantCountAfterCut() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n** Note A-02\n")
        Assert.assertEquals(1, getNote("Note A-01").position.descendantsCount)
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-02").id)))
        Assert.assertEquals(0, getNote("Note A-01").position.descendantsCount)
    }

    @Test
    fun testDescendantCountAfterDelete() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n** Note A-02\n")
        Assert.assertEquals(1, getNote("Note A-01").position.descendantsCount)
        UseCaseRunner.run(NoteDelete(
                book.book.id, setOf(getNote("Note A-02").id)))
        Assert.assertEquals(0, getNote("Note A-01").position.descendantsCount)
    }

    @Test
    @Throws(IOException::class)
    fun testPasteUnder() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                    *** Note A-05
                    *** Note A-06
                    * Note A-07
                """.trimIndent())

        // Cut A-02 and paste it under A-04
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-02").id)))
        val n = getNote("Note A-04")
        UseCaseRunner.run(NotePaste(n.position.bookId, n.id, Place.UNDER))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-04
                    *** Note A-05
                    *** Note A-06
                    *** Note A-02
                    **** Note A-03
                    * Note A-07

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA00 = getRootNode(book.book.id)
        val nA01 = getNote("Note A-01")
        val nA02 = getNote("Note A-02")
        val nA03 = getNote("Note A-03")
        val nA04 = getNote("Note A-04")
        val nA05 = getNote("Note A-05")
        val nA06 = getNote("Note A-06")
        val nA07 = getNote("Note A-07")

        Assert.assertEquals(1, nA01.position.level)
        Assert.assertEquals(2, nA04.position.level)
        Assert.assertEquals(3, nA05.position.level)
        Assert.assertEquals(3, nA06.position.level)
        Assert.assertEquals(3, nA02.position.level)
        Assert.assertEquals(4, nA03.position.level)
        Assert.assertEquals(1, nA07.position.level)
        Assert.assertTrue(nA01.position.lft < nA04.position.lft)
        Assert.assertTrue(nA04.position.lft < nA05.position.lft)
        Assert.assertTrue(nA05.position.lft < nA05.position.rgt)
        Assert.assertTrue(nA05.position.rgt < nA06.position.lft)
        Assert.assertTrue(nA06.position.lft < nA06.position.rgt)
        Assert.assertTrue(nA06.position.rgt < nA02.position.lft)
        Assert.assertTrue(nA02.position.lft < nA03.position.lft)
        Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
        Assert.assertTrue(nA03.position.rgt < nA02.position.rgt)
        Assert.assertTrue(nA02.position.rgt < nA04.position.rgt)
        Assert.assertTrue(nA04.position.rgt < nA01.position.rgt)
        Assert.assertTrue(nA01.position.rgt < nA07.position.lft)
        Assert.assertTrue(nA07.position.lft < nA07.position.rgt)
        Assert.assertEquals(nA00.id, nA01.position.parentId)
        Assert.assertEquals(nA01.id, nA04.position.parentId)
        Assert.assertEquals(nA04.id, nA05.position.parentId)
        Assert.assertEquals(nA04.id, nA06.position.parentId)
        Assert.assertEquals(nA04.id, nA02.position.parentId)
        Assert.assertEquals(nA02.id, nA03.position.parentId)
        Assert.assertEquals(nA00.id, nA07.position.parentId)
        Assert.assertEquals(5, nA01.position.descendantsCount)
        Assert.assertEquals(4, nA04.position.descendantsCount)
        Assert.assertEquals(0, nA05.position.descendantsCount)
        Assert.assertEquals(0, nA06.position.descendantsCount)
        Assert.assertEquals(1, nA02.position.descendantsCount)
        Assert.assertEquals(0, nA03.position.descendantsCount)
        Assert.assertEquals(0, nA07.position.descendantsCount)
    }

    @Test
    @Throws(IOException::class)
    fun testPasteUnderFolded() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    *** Note A-04
                    ** Note A-05
                    * Note A-06
                """.trimIndent())

        // Cut A-06 and paste it under folded A-02
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-06").id)))
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-02").id))
        val n = getNote("Note A-02")
        UseCaseRunner.run(NotePaste(n.position.bookId, n.id, Place.UNDER))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    *** Note A-04
                    *** Note A-06
                    ** Note A-05

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA00 = getRootNode(book.book.id)
        val nA01 = getNote("Note A-01")
        val nA02 = getNote("Note A-02")
        val nA03 = getNote("Note A-03")
        val nA04 = getNote("Note A-04")
        val nA05 = getNote("Note A-05")
        val nA06 = getNote("Note A-06")

        Assert.assertEquals(nA00.id, nA01.position.parentId)
        Assert.assertEquals(nA01.id, nA02.position.parentId)
        Assert.assertEquals(nA02.id, nA03.position.parentId)
        Assert.assertEquals(nA02.id, nA04.position.parentId)
        Assert.assertEquals(nA02.id, nA06.position.parentId)
        Assert.assertEquals(nA01.id, nA05.position.parentId)
        Assert.assertEquals(1, nA01.position.level)
        Assert.assertEquals(2, nA02.position.level)
        Assert.assertEquals(3, nA03.position.level)
        Assert.assertEquals(3, nA04.position.level)
        Assert.assertEquals(3, nA06.position.level)
        Assert.assertEquals(2, nA05.position.level)
        Assert.assertTrue(nA01.position.lft < nA02.position.lft)
        Assert.assertTrue(nA02.position.lft < nA03.position.lft)
        Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
        Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
        Assert.assertTrue(nA03.position.rgt < nA04.position.lft)
        Assert.assertTrue(nA04.position.lft < nA04.position.rgt)
        Assert.assertTrue(nA04.position.rgt < nA06.position.lft)
        Assert.assertTrue(nA06.position.lft < nA06.position.rgt)
        Assert.assertTrue(nA06.position.rgt < nA02.position.rgt)
        Assert.assertTrue(nA02.position.rgt < nA05.position.lft)
        Assert.assertTrue(nA05.position.lft < nA05.position.rgt)
        Assert.assertTrue(nA05.position.rgt < nA01.position.rgt)
        Assert.assertEquals(5, nA01.position.descendantsCount)
        Assert.assertEquals(3, nA02.position.descendantsCount)
        Assert.assertEquals(0, nA03.position.descendantsCount)
        Assert.assertEquals(0, nA04.position.descendantsCount)
        Assert.assertEquals(0, nA06.position.descendantsCount)
        Assert.assertEquals(0, nA05.position.descendantsCount)
        Assert.assertEquals(0, nA01.position.foldedUnderId)
        Assert.assertEquals(0, nA02.position.foldedUnderId)
        Assert.assertEquals(0, nA03.position.foldedUnderId)
        Assert.assertEquals(0, nA04.position.foldedUnderId)
        Assert.assertEquals(0, nA06.position.foldedUnderId)
        Assert.assertEquals(0, nA05.position.foldedUnderId)
    }

    @Test
    @Throws(IOException::class)
    fun testDemoteMultipleUnderFolded() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                    * Note A-04
                """.trimIndent())

        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-01").id))
        UseCaseRunner.run(NoteDemote(
                HashSet(listOf(
                        getNote("Note A-03").id,
                        getNote("Note A-04").id))))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    ** Note A-03
                    ** Note A-04

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testCutNoteUnderFoldedThenPaste() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                 """.trimIndent())

        // Cut hidden A-03 and paste it under A-01
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-02").id))
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-03").id)))
        getNote("Note A-01").let { note ->
            UseCaseRunner.run(NotePaste(note.position.bookId, note.id, Place.UNDER))
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    ** Note A-03

                    """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(0, getNote("Note A-01").position.foldedUnderId)
        Assert.assertEquals(0, getNote("Note A-02").position.foldedUnderId)
        Assert.assertEquals(0, getNote("Note A-03").position.foldedUnderId)
    }

    @Test
    @Throws(IOException::class)
    fun testPromote() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                """.trimIndent())

        val note = getNote("Note A-02")

        val (_, _, _, userData) = UseCaseRunner.run(NotePromote(setOf(note.id)))

        Assert.assertEquals(1, userData as Int)

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-02
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position
        val nA03 = getNote("Note A-03").position

        Assert.assertEquals(0, nA01.descendantsCount)
        Assert.assertEquals(0, nA02.descendantsCount)
        Assert.assertEquals(0, nA03.descendantsCount)
        Assert.assertEquals(1, nA01.level)
        Assert.assertEquals(1, nA02.level)
        Assert.assertEquals(1, nA03.level)
        Assert.assertTrue(nA01.lft < nA01.rgt)
        Assert.assertTrue(nA01.rgt < nA02.lft)
        Assert.assertTrue(nA02.lft < nA02.rgt)
        Assert.assertTrue(nA02.rgt < nA03.lft)
        Assert.assertTrue(nA03.lft < nA03.rgt)
    }

    @Test
    fun testPromoteFirstLevelNote() {
        testUtils.setupBook("Book A", "* Note A-01")
        val note = getNote("Note A-01")
        val (_, _, _, userData) = UseCaseRunner.run(NotePromote(setOf(note.id)))
        Assert.assertEquals(0, userData as Int)
    }

    @Test
    @Throws(IOException::class)
    fun testPromote2() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                    * Note A-05
                """.trimIndent())


        // Promote A-03 twice
        getNote("Note A-03").let { note ->
            UseCaseRunner.run(NotePromote(setOf(note.id))).let {
                Assert.assertEquals(1, it.userData as Int)
            }

            UseCaseRunner.run(NotePromote(setOf(note.id))).let {
                Assert.assertEquals(1, it.userData as Int)
            }
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    ** Note A-04
                    * Note A-03
                    * Note A-05

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position
        val nA03 = getNote("Note A-03").position
        val nA04 = getNote("Note A-04").position
        val nA05 = getNote("Note A-05").position

        Assert.assertEquals(2, nA01.descendantsCount)
        Assert.assertEquals(0, nA02.descendantsCount)
        Assert.assertEquals(0, nA04.descendantsCount)
        Assert.assertEquals(0, nA03.descendantsCount)
        Assert.assertEquals(0, nA05.descendantsCount)
        Assert.assertEquals(1, nA01.level)
        Assert.assertEquals(2, nA02.level)
        Assert.assertEquals(2, nA04.level)
        Assert.assertEquals(1, nA03.level)
        Assert.assertEquals(1, nA05.level)
        Assert.assertTrue(nA01.lft < nA02.lft)
        Assert.assertTrue(nA02.lft < nA02.rgt)
        Assert.assertTrue(nA02.rgt < nA04.lft)
        Assert.assertTrue(nA04.lft < nA04.rgt)
        Assert.assertTrue(nA04.rgt < nA01.rgt)
        Assert.assertTrue(nA01.rgt < nA03.lft)
        Assert.assertTrue(nA03.lft < nA03.rgt)
        Assert.assertTrue(nA03.rgt < nA05.lft)
        Assert.assertTrue(nA05.lft < nA05.rgt)
    }

    @Test
    @Throws(IOException::class)
    fun testPromoteFolded() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                    * Note A-05
                """.trimIndent())

        // Fold and promote A-02
        getNote("Note A-02").let { note ->
            UseCaseRunner.run(NoteToggleFolding(note.id))
            val (_, _, _, userData) = UseCaseRunner.run(NotePromote(setOf(note.id)))
            Assert.assertEquals(2, userData as Int)
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-04
                    * Note A-02
                    ** Note A-03
                    * Note A-05

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position
        val nA03 = getNote("Note A-03").position
        val nA04 = getNote("Note A-04").position
        val nA05 = getNote("Note A-05").position

        Assert.assertEquals(1, nA01.descendantsCount)
        Assert.assertEquals(0, nA04.descendantsCount)
        Assert.assertEquals(1, nA02.descendantsCount)
        Assert.assertEquals(0, nA03.descendantsCount)
        Assert.assertEquals(0, nA05.descendantsCount)
        Assert.assertEquals(1, nA01.level)
        Assert.assertEquals(2, nA04.level)
        Assert.assertEquals(1, nA02.level)
        Assert.assertEquals(2, nA03.level)
        Assert.assertEquals(1, nA05.level)
        Assert.assertEquals(0, nA01.foldedUnderId)
        Assert.assertEquals(0, nA04.foldedUnderId)
        Assert.assertEquals(0, nA02.foldedUnderId)
        Assert.assertEquals(getNote("Note A-02").id, nA03.foldedUnderId)
        Assert.assertEquals(0, nA05.foldedUnderId)
        Assert.assertFalse(nA01.isFolded)
        Assert.assertFalse(nA04.isFolded)
        Assert.assertTrue(nA02.isFolded)
        Assert.assertFalse(nA03.isFolded)
        Assert.assertFalse(nA05.isFolded)
        Assert.assertTrue(nA01.lft < nA04.lft)
        Assert.assertTrue(nA04.lft < nA04.rgt)
        Assert.assertTrue(nA04.rgt < nA02.rgt)
        Assert.assertTrue(nA01.rgt < nA02.lft)
        Assert.assertTrue(nA02.lft < nA03.lft)
        Assert.assertTrue(nA03.lft < nA03.rgt)
        Assert.assertTrue(nA03.rgt < nA02.rgt)
        Assert.assertTrue(nA02.rgt < nA05.lft)
        Assert.assertTrue(nA05.lft < nA05.rgt)
    }

    @Test
    @Throws(IOException::class)
    fun testPromoteFirst2LevelOrphan() {
        testUtils.setupBook("Book A", "** Note A-01\n* Note A-02\n")

        // Promote first note
        UseCaseRunner.run(NotePromote(setOf(getNote("Note A-01").id)))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position

        Assert.assertEquals(1, nA01.level)
        Assert.assertEquals(1, nA02.level)
        Assert.assertTrue("${nA01.lft} < ${nA01.rgt}", nA01.lft < nA01.rgt)
        Assert.assertTrue("${nA01.rgt} < ${nA02.lft}", nA01.rgt < nA02.lft)
        Assert.assertTrue("${nA02.lft} < ${nA02.rgt}", nA02.lft < nA02.rgt)

        Assert.assertEquals(
                "* Note A-01\n* Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testPromoteNoteWithOrphan() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    **** Note A-03
                    **** Note A-04
                """.trimIndent())

        UseCaseRunner.run(
                NotePromote(setOf(getNote("Note A-02").id)))

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-02
                    *** Note A-03
                    *** Note A-04

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA00 = getRootNode(book.book.id)
        val nA01 = getNote("Note A-01")
        val nA02 = getNote("Note A-02")
        val nA03 = getNote("Note A-03")
        val nA04 = getNote("Note A-04")

        Assert.assertEquals(1, nA01.position.level)
        Assert.assertEquals(1, nA02.position.level)
        Assert.assertEquals(3, nA03.position.level)
        Assert.assertEquals(3, nA04.position.level)
        Assert.assertEquals(nA00.id, nA01.position.parentId)
        Assert.assertEquals(nA00.id, nA02.position.parentId)
        Assert.assertEquals(nA02.id, nA03.position.parentId)
        Assert.assertEquals(nA02.id, nA04.position.parentId)
        Assert.assertTrue(nA01.position.lft < nA01.position.rgt)
        Assert.assertTrue(nA01.position.rgt < nA02.position.lft)
        Assert.assertTrue(nA02.position.lft < nA03.position.lft)
        Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
        Assert.assertTrue(nA03.position.rgt < nA04.position.lft)
        Assert.assertTrue(nA04.position.lft < nA04.position.rgt)
        Assert.assertTrue(nA04.position.rgt < nA02.position.rgt)
    }

    @Test
    @Throws(IOException::class)
    fun testPromote4LevelOrphan() {
        testUtils.setupBook("Book A", "* Note A-01\n**** Note A-02\n")

        // Promote first note
        UseCaseRunner.run(
                NotePromote(setOf(getNote("Note A-02").id)))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position

        Assert.assertEquals(1, nA01.level)
        Assert.assertEquals(2, nA02.level)

        Assert.assertTrue("${nA01.lft} < ${nA02.lft}", nA01.lft < nA02.lft)
        Assert.assertTrue("${nA02.lft} < ${nA02.rgt}", nA02.lft < nA02.rgt)
        Assert.assertTrue("${nA02.rgt} < ${nA01.rgt}", nA02.rgt < nA01.rgt)

        Assert.assertEquals(
                "* Note A-01\n** Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testDelete() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                """.trimIndent())

        UseCaseRunner.run(NoteDelete(
                book.book.id, setOf(getNote("Note A-02").id)))

        Assert.assertEquals("* Note A-01\n", dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testDemote() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01 :tag1:
                    ** Note A-02
                    * Note A-03
                    ** Note A-04
                    """.trimIndent())

        UseCaseRunner.run(NoteDemote(setOf(getNote("Note A-03").id))).let {
            Assert.assertEquals(2, it.userData as Int)
        }

        Assert.assertEquals(
                """
                    * Note A-01 :tag1:
                    ** Note A-02
                    ** Note A-03
                    *** Note A-04

                    """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01")
        val nA02 = getNote("Note A-02")
        val nA03 = getNote("Note A-03")

        Assert.assertEquals(3, nA01.position.descendantsCount)
        Assert.assertEquals(0, nA02.position.descendantsCount)
        Assert.assertEquals(1, nA03.position.descendantsCount)
        Assert.assertEquals(1, nA01.position.level)
        Assert.assertEquals(2, nA02.position.level)
        Assert.assertEquals(2, nA03.position.level)
        Assert.assertTrue(nA01.position.lft < nA02.position.lft)
        Assert.assertTrue(nA02.position.lft < nA02.position.rgt)
        Assert.assertTrue(nA02.position.rgt < nA03.position.lft)
        Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
        Assert.assertTrue(nA03.position.rgt < nA01.position.rgt)

        Assert.assertEquals("tag1", getNoteView("Note A-03").inheritedTags)
        Assert.assertEquals("tag1", getNoteView("Note A-04").inheritedTags)
    }

    @Test
    @Throws(IOException::class)
    fun testNewBelowFoldable() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n** Note A-02\n")

        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-03"),
                NotePlace(book.book.id, getNote("Note A-01").id, Place.BELOW)))

        Assert.assertEquals(
                "* Note A-01\n** Note A-02\n* Note A-03\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01").position
        val nA02 = getNote("Note A-02").position
        val nA03 = getNote("Note A-03").position

        Assert.assertTrue(nA01.lft < nA02.lft)
        Assert.assertTrue(nA02.lft < nA02.rgt)
        Assert.assertTrue(nA02.rgt < nA01.rgt)
        Assert.assertTrue(nA01.rgt < nA03.lft)
        Assert.assertTrue(nA03.lft < nA03.rgt)
        Assert.assertEquals(0, nA03.descendantsCount)
        Assert.assertEquals(getRootNode(book.book.id).id, nA03.parentId)
    }

    @Test
    @Throws(IOException::class)
    fun testPasteFoldedSubtree() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                """.trimIndent())

        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-01").id))

        // Is folded
        Assert.assertTrue(getNote("Note A-01").position.isFolded)
        Assert.assertEquals(
                getNote("Note A-01").id,
                getNote("Note A-02").position.foldedUnderId)

        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-01").id)))

        getNote("Note A-03").let { note ->
            UseCaseRunner.run(NotePaste(note.position.bookId, note.id, Place.ABOVE))
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        // Remains folded
        Assert.assertTrue(getNote("Note A-01").position.isFolded)
        Assert.assertEquals(
                getNote("Note A-01").id,
                getNote("Note A-02").position.foldedUnderId)
    }

    @Test
    @Throws(IOException::class)
    fun testPasteUnderHidden() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                """.trimIndent())

        UseCaseRunner.run(NoteToggleFolding(
                getNote("Note A-01").id))
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-03").id)))
        val n = getNote("Note A-02")
        UseCaseRunner.run(NotePaste(n.position.bookId, n.id, Place.UNDER))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertFalse(getNote("Note A-01").position.isFolded)
        Assert.assertEquals(0, getNote("Note A-02").position.foldedUnderId)
        Assert.assertEquals(0, getNote("Note A-03").position.foldedUnderId)
    }

    @Test
    @Throws(IOException::class)
    fun testDemoteUnderHidden() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                """.trimIndent())

        UseCaseRunner.run(NoteToggleFolding(
                getNote("Note A-01").id))
        UseCaseRunner.run(NoteDemote(setOf(getNote("Note A-03").id)))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    ** Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertFalse(getNote("Note A-01").position.isFolded)
        Assert.assertEquals(0, getNote("Note A-02").position.foldedUnderId)
        Assert.assertEquals(0, getNote("Note A-03").position.foldedUnderId)
    }

    @Test
    @Throws(IOException::class)
    fun testDemoteNoChanges() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    * Note A-04
                    ** Note A-05
                    *** Note A-06
                """.trimIndent())

        // Demote A-05
        val (_, _, _, userData) = UseCaseRunner.run(NoteDemote(
                setOf(getNote("Note A-05").id)))

        Assert.assertEquals(0, userData as Int)

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    * Note A-04
                    ** Note A-05
                    *** Note A-06

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testNewNoteUnder() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                """.trimIndent())

        run {
            val nA01 = getNote("Note A-01").position
            val nA02 = getNote("Note A-02").position
            val nA03 = getNote("Note A-03").position
            val nA04 = getNote("Note A-04").position

            Assert.assertTrue(nA01.lft < nA02.lft)
            Assert.assertTrue(nA02.lft < nA03.lft)
            Assert.assertTrue(nA03.lft < nA03.rgt)
            Assert.assertTrue(nA03.rgt < nA02.rgt)
            Assert.assertTrue(nA02.rgt < nA04.lft)
            Assert.assertTrue(nA04.lft < nA04.rgt)
            Assert.assertTrue(nA04.rgt < nA01.rgt)
            Assert.assertEquals(3, nA01.descendantsCount)
            Assert.assertEquals(1, nA02.descendantsCount)
            Assert.assertEquals(0, nA03.descendantsCount)
            Assert.assertEquals(0, nA04.descendantsCount)
        }

        // Create new note under Note A-02
        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-05"),
                NotePlace(book.book.id, getNote("Note A-02").id, Place.UNDER)))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    *** Note A-05
                    ** Note A-04

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        run {
            val nA01 = getNote("Note A-01").position
            val nA02 = getNote("Note A-02").position
            val nA03 = getNote("Note A-03").position
            val nA05 = getNote("Note A-05").position
            val nA04 = getNote("Note A-04").position

            Assert.assertTrue(nA01.lft < nA02.lft)
            Assert.assertTrue(nA02.lft < nA03.lft)
            Assert.assertTrue(nA03.lft < nA03.rgt)
            Assert.assertTrue(nA03.rgt < nA05.lft)
            Assert.assertTrue(nA05.lft < nA05.rgt)
            Assert.assertTrue(nA05.rgt < nA02.rgt)
            Assert.assertTrue(nA02.rgt < nA04.lft)
            Assert.assertTrue(nA04.lft < nA04.rgt)
            Assert.assertTrue(nA04.rgt < nA01.rgt)
            Assert.assertEquals(4, nA01.descendantsCount)
            Assert.assertEquals(2, nA02.descendantsCount)
            Assert.assertEquals(0, nA03.descendantsCount)
            Assert.assertEquals(0, nA05.descendantsCount)
            Assert.assertEquals(0, nA04.descendantsCount)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testNewNoteAbove() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                """.trimIndent())

        // Create new note above Note A-02
        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-05"),
                NotePlace(book.book.id, getNote("Note A-02").id, Place.ABOVE)))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-05
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        val nA01 = getNote("Note A-01").position
        val nA05 = getNote("Note A-05").position
        val nA02 = getNote("Note A-02").position
        val nA03 = getNote("Note A-03").position
        val nA04 = getNote("Note A-04").position

        Assert.assertTrue(nA01.lft < nA05.lft)
        Assert.assertTrue(nA05.lft < nA05.rgt)
        Assert.assertTrue(nA05.rgt < nA02.lft)
        Assert.assertTrue(nA02.lft < nA03.lft)
        Assert.assertTrue(nA03.lft < nA03.rgt)
        Assert.assertTrue(nA03.rgt < nA02.rgt)
        Assert.assertTrue(nA02.rgt < nA04.lft)
        Assert.assertTrue(nA04.lft < nA04.rgt)
        Assert.assertTrue(nA04.rgt < nA01.rgt)
    }

    @Test
    fun testCyclingFreshlyImportedNotebook() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                    * Note A-05
                """.trimIndent())

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        Assert.assertTrue(getNote("Note A-01").position.isFolded)
        Assert.assertTrue(getNote("Note A-02").position.isFolded)
        Assert.assertTrue(getNote("Note A-03").position.isFolded)
        Assert.assertTrue(getNote("Note A-04").position.isFolded)
        Assert.assertTrue(getNote("Note A-05").position.isFolded)

        // Unfold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        Assert.assertFalse(getNote("Note A-01").position.isFolded)
        Assert.assertFalse(getNote("Note A-02").position.isFolded)
        Assert.assertFalse(getNote("Note A-03").position.isFolded)
        Assert.assertFalse(getNote("Note A-04").position.isFolded)
        Assert.assertFalse(getNote("Note A-05").position.isFolded)
    }

    @Test
    fun testCyclingFoldedState() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                    * Note A-05
                    ** Note A-06
                """.trimIndent())

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        // Unfold A-01
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-01").id))

        Assert.assertEquals(0, getNote("Note A-02").position.foldedUnderId)
        Assert.assertEquals(0, getNote("Note A-04").position.foldedUnderId)

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        // Unfold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        // Fold A-01
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-01").id))

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        // Unfold Note 1
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-01").id))

        Assert.assertFalse(getNote("Note A-01").position.isFolded)
        Assert.assertTrue(getNote("Note A-02").position.isFolded)
    }

    @Test
    @Throws(IOException::class)
    fun testCutChildCutParentThenPaste() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n** Note A-02\n* Note A-03\n")

        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-02").id)))
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-01").id)))
        getNote("Note A-03").let { n ->
            UseCaseRunner.run(NotePaste(n.position.bookId, n.id, Place.UNDER))
        }

        Assert.assertEquals(
                "* Note A-03\n** Note A-01\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testParentIds() {
        val (book) = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    ** Note A-04
                """.trimIndent())

        val rootNode = getRootNode(book.id)

        Assert.assertNotNull(rootNode)
        Assert.assertEquals(0, rootNode.position.parentId)
        Assert.assertEquals(rootNode.id, getNote("Note A-01").position.parentId)
        Assert.assertEquals(
                getNote("Note A-01").id,
                getNote("Note A-02").position.parentId)
        Assert.assertEquals(
                getNote("Note A-02").id,
                getNote("Note A-03").position.parentId)
        Assert.assertEquals(
                getNote("Note A-01").id,
                getNote("Note A-04").position.parentId)
    }

    @Test
    @Throws(IOException::class)
    fun testParentIdForCreatedNote() {
        val book = testUtils.setupBook("Book A", "* Note A-01")

        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-02"),
                NotePlace(book.book.id, getNote("Note A-01").id, Place.UNDER)))

        Assert.assertEquals(1, getNote("Note A-01").position.descendantsCount)
        Assert.assertEquals(
                getNote("Note A-01").id,
                getNote("Note A-02").position.parentId)
    }

    @Test
    fun testFoldingAllWhenContentOnlyIsFolded() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    Preface
                    * Note A-01
                    ** Note A-02
                    * Note A-03
                    Content
                """.trimIndent())

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        // Unfold A-03's content
        UseCaseRunner.run(NoteToggleFolding(getNote("Note A-03").id))

        // Fold all
        UseCaseRunner.run(BookCycleVisibility(book.book))

        Assert.assertTrue(getNote("Note A-01").position.isFolded)
        Assert.assertTrue(getNote("Note A-02").position.isFolded)
        Assert.assertTrue(getNote("Note A-03").position.isFolded)
    }

    @Test
    @Throws(IOException::class)
    fun testInheritedTagsAfterCutAndPaste() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * A-01 :a:
                    ** A-02 :b:
                    *** A-03 :c:
                    * A-04 :d:
                """.trimIndent())

        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("A-02").id)))
        UseCaseRunner.run(NotePaste(
                book.book.id, getNote("A-04").id, Place.UNDER))

        Assert.assertEquals(
                """
                    * A-01 :a:
                    * A-04 :d:
                    ** A-02 :b:
                    *** A-03 :c:

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(1, getNoteView("A-02").getInheritedTagsList().size)
        Assert.assertEquals(2, getNoteView("A-03").getInheritedTagsList().size)
    }

    /**
     * Test that root node's rgt is larger then notes' rgt.
     */
    @Test
    @Throws(IOException::class)
    fun testCutAndPaste() {
        val (book) = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    ** Note A-03
                """.trimIndent())

        UseCaseRunner.run(NoteCut(
                book.id,
                HashSet(listOf(
                        getNote("Note A-01").id,
                        getNote("Note A-03").id))))

        UseCaseRunner.run(NotePaste(
                book.id,
                getNote("Note A-02").id,
                Place.BELOW))

        Assert.assertEquals(
                """
                    * Note A-02
                    * Note A-01
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        // Compare to root note
        val bookId = book.id
        val rootRgt = getRootNode(bookId).position.rgt

        Assert.assertTrue(rootRgt > getNote("Note A-01").position.rgt)
        Assert.assertTrue(rootRgt > getNote("Note A-02").position.rgt)
        Assert.assertTrue(rootRgt > getNote("Note A-03").position.rgt)
    }

    /**
     * After moving one note under another, test lft and rgt od third newly created note.
     */
    @Test
    @Throws(IOException::class)
    fun testNewNoteAfterMovingUnder() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n* Note A-02")

        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-01").id)))
        UseCaseRunner.run(NotePaste(
                book.book.id, getNote("Note A-02").id, Place.UNDER))
        UseCaseRunner.run(NoteCreate(
                NotePayload("Note A-03"), NotePlace(book.book.id)))

        Assert.assertTrue(
                getNote("Note A-02").position.rgt
                        < getNote("Note A-03").position.lft)
    }

    @Test
    @Throws(IOException::class)
    fun testMoveNoteDown() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * A-01
                    ** A-02
                    ** A-03
                    * A-04
                    ** A-05
                    ** A-06
                    * A-07
                """.trimIndent())

        // Move A-01 down
        UseCaseRunner.run(NoteMove(
                book.book.id, setOf(getNote("A-01").id), 1))

        Assert.assertEquals(
                """
                    * A-04
                    ** A-05
                    ** A-06
                    * A-01
                    ** A-02
                    ** A-03
                    * A-07

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testRefile() {
        testUtils.setupBook(
                "Book A",
                """
                    * TODO A-01
                    SCHEDULED: <2018-04-24 Tue>

                    content

                    ** A-02
                    ** A-03

                    * TODO A-04
                    SCHEDULED: <2018-04-23 Mon>

                    ** A-05
                    ** A-06

                    * TODO A-07

                """.trimIndent())

        val (bookB) = testUtils.setupBook(
                "Book B",
                "* TODO B-01\n")

        UseCaseRunner.run(NoteRefile(
                setOf(getNote("A-04").id), NotePlace(bookB.id)))

        Assert.assertEquals(
                """
                    * TODO A-01
                    SCHEDULED: <2018-04-24 Tue>

                    content

                    ** A-02
                    ** A-03
                    * TODO A-07

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertEquals(
                """
                    * TODO B-01
                    * TODO A-04
                    SCHEDULED: <2018-04-23 Mon>

                    ** A-05
                    ** A-06

                """.trimIndent(),
                dataRepository.getBookContent("Book B", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun testPasteMultipleTimesBelow() {
        val book = testUtils.setupBook("Book A", "* Note A-01\n* Note A-02\n")

        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-02").id)))

        Assert.assertEquals(
                "* Note A-01\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        getNote("Note A-01").let { note ->
            UseCaseRunner.run(NotePaste(note.position.bookId, note.id, Place.BELOW))
        }

        Assert.assertEquals(
                "* Note A-01\n* Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertTrue(getNote("Note A-01").position.rgt
                < getNote("Note A-02").position.lft)

        getNote("Note A-02").let { note ->
            UseCaseRunner.run(NotePaste(note.position.bookId, note.id, Place.BELOW))
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-02
                    * Note A-02

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        getNote("Note A-02").let { note ->
            UseCaseRunner.run(NotePaste(note.position.bookId, note.id, Place.BELOW))
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-02
                    * Note A-02
                    * Note A-02

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        Assert.assertTrue(getNote("Note A-02").position.rgt < getRootNode(book.book.id).position.rgt)
    }

    @Test
    @Throws(IOException::class)
    fun moveMultiple() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    * Note A-03
                    ** Note A-04
                    *** Note A-05
                """.trimIndent())

        UseCaseRunner.run(NoteMove(
                book.book.id,
                HashSet(listOf(
                        getNote("Note A-02").id,
                        getNote("Note A-04").id)),
                -1))

        Assert.assertEquals(
                """
                    * Note A-02
                    * Note A-04
                    ** Note A-05
                    * Note A-01
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Ignore("Not supported")
    @Test
    @Throws(IOException::class)
    fun moveMultipleDownWithChildrenSelected() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    ** Note A-03
                    * Note A-04
                """.trimIndent())

        UseCaseRunner.run(NoteMove(
                book.book.id,
                HashSet(listOf(
                        getNote("Note A-02").id,
                        getNote("Note A-03").id)),
                1))

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-04
                    * Note A-02
                    ** Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun demoteMultiple() {
        testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    * Note A-03
                    ** Note A-04
                    *** Note A-05
                """.trimIndent())

        UseCaseRunner.run(NoteDemote(
                HashSet(listOf(
                        getNote("Note A-02").id,
                        getNote("Note A-04").id))))

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    ** Note A-04
                    *** Note A-05
                    * Note A-03

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))
    }

    @Test
    @Throws(IOException::class)
    fun keptNotePropertiesAndNoteEvents() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    :PROPERTIES:
                    :K1: V1
                    :K2: V2
                    :END:
                    <2000-01-01>
                    * Note A-03
                """.trimIndent())

        Assert.assertEquals(1, dataRepository.getNoteEvents(
                getNote("Note A-02").id).size)
        Assert.assertEquals(2, dataRepository.getNoteProperties(
                getNote("Note A-02").id).size)

        UseCaseRunner.run(NoteDemote(setOf(getNote("Note A-02").id)))
        UseCaseRunner.run(NotePromote(setOf(getNote("Note A-02").id)))
        UseCaseRunner.run(NoteMove(
                book.book.id, setOf(getNote("Note A-02").id),
                1))
        UseCaseRunner.run(NoteMove(
                book.book.id, setOf(getNote("Note A-02").id),
                -1))
        UseCaseRunner.run(NoteCut(
                book.book.id, setOf(getNote("Note A-02").id)))
        UseCaseRunner.run(NotePaste(
                book.book.id,
                getNote("Note A-01").id,
                Place.ABOVE))
        UseCaseRunner.run(NoteRefile(setOf(getNote("Note A-02").id),
                NotePlace(book.book.id)))
        UseCaseRunner.run(NoteCopy(
                book.book.id, setOf(getNote("Note A-02").id)))
        UseCaseRunner.run(NotePaste(
                book.book.id,
                getNote("Note A-02").id,
                Place.BELOW))

        Assert.assertEquals(
                """
                    * Note A-01
                    * Note A-03
                    * Note A-02
                    :PROPERTIES:
                    :K1:       V1
                    :K2:       V2
                    :END:

                    <2000-01-01>

                    * Note A-02
                    :PROPERTIES:
                    :K1:       V1
                    :K2:       V2
                    :END:

                    <2000-01-01>


                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        getNote("Note A-02").id.let { noteId ->
            Assert.assertEquals(1, dataRepository.getNoteEvents(noteId).size)
            Assert.assertEquals(2, dataRepository.getNoteProperties(noteId).size)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testSubtreesAligned() {
        val book = testUtils.setupBook(
                "Book A",
                """
                    * Note A-01
                    * Note A-02
                    ** Note A-03
                    ** Note A-04

                """.trimIndent())

        run {
            val nA01 = getNote("Note A-01")
            val nA02 = getNote("Note A-02")
            val nA03 = getNote("Note A-03")

            UseCaseRunner.run(NoteMove(
                    book.book.id, setOf(nA03.id), 1))
            UseCaseRunner.run(NoteMove(
                    book.book.id, setOf(nA03.id), -1))
            UseCaseRunner.run(NoteMove(
                    book.book.id, setOf(nA03.id), 1))
            UseCaseRunner.run(NoteMove(
                    book.book.id, setOf(nA03.id), -1))
            UseCaseRunner.run(NoteRefile(setOf(nA02.id),
                    NotePlace(book.book.id, nA01.id, Place.UNDER)))
        }

        Assert.assertEquals(
                """
                    * Note A-01
                    ** Note A-02
                    *** Note A-03
                    *** Note A-04

                """.trimIndent(),
                dataRepository.getBookContent("Book A", BookFormat.ORG))

        run {
            val nA01 = getNote("Note A-01")
            val nA02 = getNote("Note A-02")
            val nA03 = getNote("Note A-03")
            val nA04 = getNote("Note A-04")

            Assert.assertTrue(nA01.position.lft < nA02.position.lft)
            Assert.assertTrue(nA02.position.lft < nA03.position.lft)
            Assert.assertTrue(nA03.position.lft < nA03.position.rgt)
            Assert.assertTrue(nA03.position.rgt < nA04.position.lft)
            Assert.assertTrue(nA04.position.lft < nA04.position.rgt)
            Assert.assertTrue(nA04.position.rgt < nA02.position.rgt)
            Assert.assertTrue(nA02.position.rgt < nA01.position.rgt)
        }
    }

    private fun getNote(title: String): Note {
        return dataRepository.getLastNote(title)!!
    }

    private fun getRootNode(bookId: Long): Note {
        return dataRepository.getRootNode(bookId)!!
    }

    private fun getNoteView(title: String): NoteView {
        return dataRepository.getLastNoteView(title)!!
    }

    private fun getBook(title: String): Book {
        return dataRepository.getBook(title)!!
    }
}

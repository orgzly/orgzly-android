package com.orgzly.android.espresso

import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.util.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Test


class InternalLinksTest : OrgzlyTest() {
    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        testUtils.setupBook(
                "book-a",
                """
                    :PROPERTIES:
                    :ID: 019ae998-1439-4a28-988a-291ee8428245
                    :END:

                    * Note [a-1]
                    [[id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F]]

                    * Note [a-2]
                    [[#Different case custom id]]

                    * Note [a-3]
                    [[#Link to note in a different book]]

                    * Note [a-4]
                    file:book-b.org

                    * Note [a-5]
                    file:./book-b.org

                    * Note [a-6]
                    [[id:note-with-this-id-does-not-exist]]

                    * Note [a-7]
                    [[id:dd791937-3fb6-4018-8d5d-b278e0e52c80][Link to book-b by id]]
                """.trimIndent()
        )

        testUtils.setupBook(
                "book-b",
                """
                    :PROPERTIES:
                    dd791937-3fb6-4018-8d5d-b278e0e52c80
                    :END:

                    * Note [b-1]
                    :PROPERTIES:
                    :CUSTOM_ID: DIFFERENT case CUSTOM id
                    :END:

                    * Note [b-2]
                    :PROPERTIES:
                    :ID: BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F
                    :END:

                    * Note [b-3]
                    :PROPERTIES:
                    :CUSTOM_ID: Link to note in a different book
                    :END:
                """.trimIndent()
        )

        ActivityScenario.launch(MainActivity::class.java)

        onBook(0).perform(click())
    }

    @Test
    fun testDifferentCaseUuidInternalLink() {
        onNoteInBook(1, R.id.item_head_content_view)
                .perform(clickClickableSpan("id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F"))
        onView(withId(R.id.title_view)).check(matches(withText("Note [b-2]")))
    }

    @Test
    fun testDifferentCaseCustomIdInternalLink() {
        onNoteInBook(2, R.id.item_head_content_view)
                .perform(clickClickableSpan("#Different case custom id"))
        onView(withId(R.id.title_view)).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testCustomIdLink() {
        onNoteInBook(3, R.id.item_head_content_view)
                .perform(clickClickableSpan("#Link to note in a different book"))
        onView(withId(R.id.title_view)).check(matches(withText("Note [b-3]")))
    }

    @Test
    fun testBookLink() {
        onNoteInBook(4, R.id.item_head_content_view)
                .perform(clickClickableSpan("file:book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testBookRelativeLink() {
        onNoteInBook(5, R.id.item_head_content_view)
                .perform(clickClickableSpan("file:./book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testNonExistentId() {
        onNoteInBook(6, R.id.item_head_content_view)
            .perform(clickClickableSpan("id:note-with-this-id-does-not-exist"))
        onSnackbar()
            .check(matches(withText("Note with “ID” property set to “note-with-this-id-does-not-exist” not found")))
    }

    @Test
    @Ignore("Parsing PROPERTIES drawer from book preface is not supported yet")
    fun testLinkToBookById() {
        onNoteInBook(7, R.id.item_head_content_view)
            .perform(clickClickableSpan("Link to book-b by id"))

//        onSnackbar()
//            .check(matches(withText("Note with “ID” property set to “dd791937-3fb6-4018-8d5d-b278e0e52c80” not found")))

        // In book
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))

        // In book-b
        onView(allOf(instanceOf(TextView::class.java), withParent(withId(R.id.top_toolbar))))
            .check(matches(withText("book-b")))
    }
}

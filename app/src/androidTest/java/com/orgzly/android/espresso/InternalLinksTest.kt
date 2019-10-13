package com.orgzly.android.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InternalLinksTest : OrgzlyTest() {
    @get:Rule
    val activityRule = EspressoActivityTestRule(MainActivity::class.java)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        testUtils.setupBook(
                "book-a",
                """
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
                """.trimIndent()
        )

        testUtils.setupBook(
                "book-b",
                """
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

        activityRule.launchActivity(null)

        onBook(0).perform(click())
    }

    @Test
    fun testDifferentCaseUuidInternalLink() {
        onNoteInBook(1, R.id.item_head_content)
                .perform(clickClickableSpan("id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-2]")))
    }

    @Test
    fun testDifferentCaseCustomIdInternalLink() {
        onNoteInBook(2, R.id.item_head_content)
                .perform(clickClickableSpan("#Different case custom id"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testCustomIdLink() {
        onNoteInBook(3, R.id.item_head_content)
                .perform(clickClickableSpan("#Link to note in a different book"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-3]")))
    }

    @Test
    fun testBookLink() {
        onNoteInBook(4, R.id.item_head_content)
                .perform(clickClickableSpan("file:book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testBookRelativeLink() {
        onNoteInBook(5, R.id.item_head_content)
                .perform(clickClickableSpan("file:./book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note [b-1]")))
    }
}

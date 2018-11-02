package com.orgzly.android.espresso

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.clickClickableSpan
import com.orgzly.android.espresso.EspressoUtils.onListItem
import com.orgzly.android.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InternalLinksTest : OrgzlyTest() {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        shelfTestUtils.setupBook(
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

        shelfTestUtils.setupBook(
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

        onListItem(0).perform(click())
    }

    @Test
    fun testDifferentCaseUuidInternalLink() {
        onListItem(0).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-2]")))
    }

    @Test
    fun testDifferentCaseCustomIdInternalLink() {
        onListItem(1).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("#Different case custom id"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testCustomIdLink() {
        onListItem(2).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("#Link to note in a different book"))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-3]")))
    }

    @Test
    fun testBookLink() {
        onListItem(3).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("file:book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note [b-1]")))
    }

    @Test
    fun testBookRelativeLink() {
        onListItem(4).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("file:./book-b.org"))
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note [b-1]")))
    }
}

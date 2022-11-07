package com.orgzly.android.espresso

import androidx.annotation.StringRes
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.util.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test

class BookPrefaceTest : OrgzlyTest() {
    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        testUtils.setupBook(
                "book-name",
                """
                    Line 1
                    Line 2
                    Line 3
                    Line 4
                    Line 5

                    * Note #1.
                    * Note #2.
                    ** TODO Note #3.
                """.trimIndent())

        ActivityScenario.launch(MainActivity::class.java)

        onBook(0).perform(click())
    }

    @Test
    fun testOpensBookDescription() {
        onPreface().perform(click())
        onView(withId(R.id.fragment_book_preface_container)).check(matches(isDisplayed()))
    }

    @Test
    fun testUpdatingBookPreface() {
        onPreface().perform(click())
        onView(withId(R.id.fragment_book_preface_content)).perform(click())
        onView(withId(R.id.fragment_book_preface_content_edit)).perform(*replaceTextCloseKeyboard("New content"))
        onView(withId(R.id.done)).perform(click()) // Preface done
        onPreface().perform(click())
        onView(withId(R.id.fragment_book_preface_content_view)).check(matches(withText("New content")))
    }

    @Test
    fun testDeleteBookPreface() {
        // Preface is displayed
        onPreface().check(matches(isDisplayed()))

        // Open preface and delete it
        onPreface().perform(click())
        onActionItemClick(R.id.delete, R.string.delete)

        // Preface is not displayed anymore
        onPreface().check(matches(not(isDisplayed())))
    }

    @Test
    fun testPrefaceFullDisplayed() {
        setPrefaceSetting(R.string.preface_in_book_full)
        onPreface(R.id.item_preface_text_view).check(matches(withText("Line 1\nLine 2\nLine 3\nLine 4\nLine 5")))
        // onView(withText("Line 1\nLine 2\nLine 3\nLine 4\nLine 5")).check(matches(isDisplayed()))
    }

    @Test
    fun testPrefaceHiddenNotDisplayed() {
        setPrefaceSetting(R.string.preface_in_book_hide)
        onPreface().check(matches(not(isDisplayed())))
    }

    private fun setPrefaceSetting(@StringRes id: Int) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings)

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks)
        clickSetting("pref_key_preface_in_book", R.string.preface_in_book)

        onView(withText(id)).perform(click())

        pressBack()
        pressBack()
    }
}

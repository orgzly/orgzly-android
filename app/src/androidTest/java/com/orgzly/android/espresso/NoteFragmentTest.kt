package com.orgzly.android.espresso

import android.widget.DatePicker
import android.widget.TimePicker
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions.setDate
import androidx.test.espresso.contrib.PickerActions.setTime
import androidx.test.espresso.matcher.ViewMatchers.*
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class NoteFragmentTest : OrgzlyTest() {
    @get:Rule
    var activityRule = EspressoActivityTestRule(MainActivity::class.java)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        testUtils.setupBook(
                "book-name",
                """
                    Sample book used for tests

                    * Note #1.

                    * Note #2.
                    SCHEDULED: <2014-05-22 Thu> DEADLINE: <2014-05-22 Thu>

                    ** TODO Note #3.

                    ** Note #4.
                    SCHEDULED: <2015-01-11 Sun .+1d/2d>

                    *** DONE Note #5.
                    CLOSED: [2014-01-01 Wed 20:07]

                    **** Note #6.

                    ** Note #7.

                    * ANTIVIVISECTIONISTS Note #8.

                    **** Note #9.

                    ** Note #10.
                    :PROPERTIES:
                    :CREATED:  [2019-10-04 Fri 10:23]
                    :END:

                """.trimIndent())

        activityRule.launchActivity(null)

        onBook(0).perform(click())
    }

    @Test
    fun testDeleteNote() {
        onNoteInBook(1).perform(click())

        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()))

        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(R.string.delete)).perform(click())
        onView(withText(R.string.delete)).perform(click())

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))

        onSnackbar().check(matches(withText(
                context.resources.getQuantityString(R.plurals.notes_deleted, 1, 1))))
    }

    @Test
    fun testUpdateNoteTitle() {
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note #1.")))

        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_title))
                .perform(*replaceTextCloseKeyboard("Note title changed"))
        onView(withId(R.id.done)).perform(click())

        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note title changed")))
    }

    @Test
    fun testSettingScheduleTime() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        onView(withText(R.string.set)).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(withText(startsWith(defaultDialogUserDate()))))
    }

    @Test
    fun testAbortingOfSettingScheduledTime() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        pressBack()
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
    }

    @Test
    fun testRemovingScheduledTime() {
        onNoteInBook(2).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(""))))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        onView(withText(R.string.clear)).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
    }

    @Test
    fun testRemovingScheduledTimeAndOpeningTimestampDialogAgain() {
        onNoteInBook(2).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(""))))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        onView(withText(R.string.clear)).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
    }

    @Test
    fun testSettingDeadlineTime() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_deadline_button)).perform(click())
        onView(withText(R.string.set)).perform(click())
        onView(withId(R.id.fragment_note_deadline_button))
                .check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())))
    }

    @Test
    fun testAbortingOfSettingDeadlineTime() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_deadline_button)).perform(click())
        pressBack()
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))
    }

    @Test
    fun testRemovingDeadlineTime() {
        onNoteInBook(2).perform(click())
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(not(withText(""))))
        onView(withId(R.id.fragment_note_deadline_button)).perform(click())
        onView(withText(R.string.clear)).perform(click())
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))
    }

    @Test
    fun testStateToDoneShouldAddClosedTime() {
        onNoteInBook(2).perform(click())

        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())))
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("DONE")).perform(click())
        onView(withId(R.id.fragment_note_closed_edit_text))
                .check(matches(allOf(withText(startsWith(currentUserDate())), isDisplayed())))
    }

    @Test
    fun testStateToDoneShouldOverwriteLastRepeat() {
        onNoteInBook(4).perform(click())

        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("DONE")).perform(click())

        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("DONE")).perform(click())

        // This will fail if there are two or more LAST_REPEAT properties
        onView(allOf(withId(R.id.name), withText("LAST_REPEAT"))).check(matches(isDisplayed()))
    }

    @Test
    fun testStateToDoneForNoteShouldShiftTime() {
        onNoteInBook(4).perform(click())

        onView(withId(R.id.fragment_note_state_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(allOf(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")), isDisplayed())))
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())))

        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("DONE")).perform(click())

        onView(withId(R.id.fragment_note_state_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(not(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")))))
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testChangingStateSettingsFromNoteFragment() {
        onNoteInBook(1).perform(click())
        settingsSetTodoKeywords("")
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onListView().check(matches(listViewItemCount(1))) // Only DONE
        pressBack()
        settingsSetTodoKeywords("TODO")
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onListView().check(matches(listViewItemCount(2)))
    }

    @Test
    fun testTitleCanNotBeEmptyForNewNote() {
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.done)).perform(click())
        onSnackbar().check(matches(withText(R.string.title_can_not_be_empty)))
    }

    @Test
    fun testTitleCanNotBeEmptyForExistingNote() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_title)).perform(*replaceTextCloseKeyboard(""))
        onView(withId(R.id.done)).perform(click())
        onSnackbar().check(matches(withText(R.string.title_can_not_be_empty)))
    }

    @Test
    fun testSavingNoteWithRepeater() {
        onNoteInBook(4).perform(click())
        onView(withId(R.id.done)).perform(click())
    }

    @Test
    fun testClosedTimeInNoteFragmentIsSameAsInList() {
        onNoteInBook(5).perform(click())
        onView(withId(R.id.fragment_note_closed_edit_text))
                .check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())))
    }

    @Test
    fun testSettingStateRemainsSetAfterRotation() {
        toPortrait(activityRule)
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("TODO")).perform(click())
        onView(withText("TODO")).check(matches(isDisplayed()))
        toLandscape(activityRule)
        onView(withText("TODO")).check(matches(isDisplayed()))
    }

    @Test
    fun testSettingPriorityRemainsSetAfterRotation() {
        toPortrait(activityRule)
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_priority_button)).perform(click())
        onView(withText("B")).perform(click())
        onView(withId(R.id.fragment_note_priority_button)).check(matches(withText("B")))
        toLandscape(activityRule)
        onView(withId(R.id.fragment_note_priority_button)).check(matches(withText("B")))
    }

    @Test
    fun testSettingScheduledTimeRemainsSetAfterRotation() {
        toPortrait(activityRule)
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        onView(withText(R.string.set)).perform(click())
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(withText(startsWith(defaultDialogUserDate()))))
        toLandscape(activityRule)
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(withText(startsWith(defaultDialogUserDate()))))
    }

    @Test
    fun testSetScheduledTimeAfterRotation() {
        onNoteInBook(1).perform(click())
        toPortrait(activityRule)
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click())
        toLandscape(activityRule)
        onView(withText(R.string.set)).perform(closeSoftKeyboardWithDelay(), click())
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(withText(startsWith(defaultDialogUserDate()))))
    }

    @Test
    fun testRemovingDoneStateRemovesClosedTime() {
        onNoteInBook(5).perform(click())
        onView(withId(R.id.fragment_note_closed_edit_text))
                .check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())))
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText(R.string.clear)).perform(click())
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testSettingPmTimeDisplays24HourTime() {
        onNoteInBook(1).perform(click())

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))
        onView(withId(R.id.fragment_note_deadline_button)).perform(click())

        /* Set date. */
        onView(withId(R.id.date_picker_button)).perform(click())
        onView(withClassName(equalTo(DatePicker::class.java.name))).perform(setDate(2014, 4, 1))
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click())

        /* Set time. */
        onView(withId(R.id.time_picker_button)).perform(scrollTo(), click())
        onView(withClassName(equalTo(TimePicker::class.java.name))).perform(setTime(15, 15))
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click())

        onView(withText(R.string.set)).perform(click())

        onView(withId(R.id.fragment_note_deadline_button))
                .check(matches(withText(userDateTime("<2014-04-01 Tue 15:15>"))))
    }

    @Test
    fun testDateTimePickerKeepsValuesAfterRotation() {
        onNoteInBook(1).perform(click())

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")))

        toPortrait(activityRule)

        onView(withId(R.id.fragment_note_deadline_button)).perform(click())

        /* Set date. */
        onView(withId(R.id.date_picker_button)).perform(click())
        onView(withClassName(equalTo(DatePicker::class.java.name))).perform(setDate(2014, 4, 1))
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click())

        /* Set time. */
        onView(withId(R.id.time_picker_button)).perform(scrollTo(), click())
        onView(withClassName(equalTo(TimePicker::class.java.name))).perform(setTime(9, 15))
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click())

        /* Set repeater. */
        onView(withId(R.id.repeater_used_checkbox)).perform(scrollTo(), click())
        onView(withId(R.id.repeater_picker_button)).perform(scrollTo(), click())
        onView(withId(R.id.value_picker)).perform(setNumber(3))
        onView(withText(R.string.ok)).perform(click())

        /* Rotate screen. */
        toLandscape(activityRule)

        /* Set time. */
        onView(withText(R.string.set)).perform(click())

        onView(withId(R.id.fragment_note_deadline_button))
                .check(matches(withText(userDateTime("<2014-04-01 Tue 09:15 .+3w>"))))
    }

    @Test
    fun testChangingPrioritySettingsFromNoteFragment() {
        /* Open note which has no priority set. */
        onNoteInBook(1).perform(click())

        /* Change lowest priority to A. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings)
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks)
        clickSetting("pref_key_min_priority", R.string.lowest_priority)
        onData(hasToString(containsString("A"))).perform(click())
        pressBack()
        pressBack()

        onView(withId(R.id.fragment_note_priority_button)).perform(click())
        onListView().check(matches(listViewItemCount(1)))
        pressBack()

        /* Change lowest priority to C. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings)
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks)
        clickSetting("pref_key_min_priority", R.string.lowest_priority)
        onData(hasToString(containsString("C"))).perform(click())
        pressBack()
        pressBack()

        onView(withId(R.id.fragment_note_priority_button)).perform(click())
        onListView().check(matches(listViewItemCount(3)))
    }

    @Test
    fun testPropertiesAfterRotatingDevice() {
        onNoteInBook(1).perform(click())

        onView(withId(R.id.fragment_note_container)).perform(swipeUp()) // For small screens

        onView(withId(R.id.name))
                .perform(replaceText("prop-name-1"))
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-1"))))
                .perform(*replaceTextCloseKeyboard("prop-value-1"))

        onView(allOf(withId(R.id.name), not(withText("prop-name-1"))))
                .perform(replaceText("prop-name-2"))
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-2"))))
                .perform(*replaceTextCloseKeyboard("prop-value-2"))

        toLandscape(activityRule)
        toPortrait(activityRule)

        onView(withId(R.id.fragment_note_container)).perform(swipeUp()) // For small screens

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.name), withText("prop-name-2"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.value), withText("prop-value-2"))).check(matches(isDisplayed()))
    }

    @Test
    fun testSavingProperties() {
        onNoteInBook(1).perform(click())

        onView(withId(R.id.name))
                .perform(replaceText("prop-name-1"))
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-1"))))
                .perform(*replaceTextCloseKeyboard("prop-value-1"))

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()))

        onView(withId(R.id.done)).perform(click())

        onNoteInBook(1).perform(click())

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()))
    }

    @Test
    fun testContentLineCountUpdatedOnNoteUpdate() {
        onNoteInBook(1).perform(click())
        onView(withId(R.id.body_edit)).perform(scrollTo()) // For smaller screens
        onView(withId(R.id.body_edit)).perform(*replaceTextCloseKeyboard("a\nb\nc"))
        onView(withId(R.id.done)).perform(click())
        onNoteInBook(1, R.id.item_head_fold_button_text).perform(click())
        onNoteInBook(1, R.id.item_head_title).check(matches(withText(endsWith("3"))))
    }

    @Test
    fun testBreadcrumbsFollowToBook() {
        onNoteInBook(3).perform(click())

        // onView(withId(R.id.fragment_note_breadcrumbs_text)).perform(clickClickableSpan("book-name"));
        // SystemClock.sleep(5000);

        onView(withId(R.id.fragment_note_breadcrumbs_text)).perform(click())

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
    }

    @Test
    fun testBreadcrumbsFollowToNote() {
        onNoteInBook(3).perform(click())
        onView(withId(R.id.fragment_note_breadcrumbs_text)).perform(clickClickableSpan("Note #2."))
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note #2.")))
    }

    @Test
    fun testBreadcrumbsPromptWhenCreatingNewNote() {
        onNoteInBook(1).perform(longClick())
        onView(withId(R.id.bottom_action_bar_new)).perform(click())
        onView(withText(R.string.new_under)).perform(click())
        onView(withId(R.id.fragment_note_title)).perform(*replaceTextCloseKeyboard("1.1"))
        onView(withId(R.id.fragment_note_breadcrumbs_text)).perform(clickClickableSpan("Note #1."))

        // Dialog is displayed
        onView(withText(R.string.discard_or_save_changes)).check(matches(isDisplayed()))

        onView(withText(R.string.cancel)).perform(click())

        // Title remains the same
        onView(withId(R.id.fragment_note_title)).check(matches(withText("1.1")))
    }

    // https://github.com/orgzly/orgzly-android/issues/605
    @Test
    fun testMetadataShowSelectedOnNoteLoad() {
        onNoteInBook(10).perform(click())
        onView(withText("CREATED")).check(matches(isDisplayed()))
        openActionBarOverflowOrOptionsMenu(context)
        onView(withText(R.string.metadata)).perform(click())
        onView(withText(R.string.show_selected)).perform(click())
        onView(withText("CREATED")).check(matches(isDisplayed()))
        pressBack()
        onNoteInBook(10).perform(click())
        onView(withText("CREATED")).check(matches(isDisplayed()))
    }

    @Test
    fun testDoNotPromptAfterLeavingNewNoteUnmodified() {
        onView(withId(R.id.fab)).perform(click())
        pressBack() // Close keyboard
        pressBack() // Leave note

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()))
    }
}

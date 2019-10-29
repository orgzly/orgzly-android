package com.orgzly.android.espresso

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.org.datetime.OrgDateTime
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test

class NoteEventsTest : OrgzlyTest() {
    @get:Rule
    val activityRule = EspressoActivityTestRule(MainActivity::class.java)

    private val now: String
            get() = OrgDateTime(true).toString()

    private val today: String
        get() = OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis())
                .setIsActive(true)
                .build()
                .toString()

    private val tomorrow: String
            get() = OrgDateTime.Builder()
                    .setDateTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24)
                    .setIsActive(true)
                    .build()
                    .toString()

    private val inFewDays: String
        get() = OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 3)
                .setIsActive(true)
                .build()
                .toString()

    private val yesterday: String
        get() = OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24)
                .setIsActive(true)
                .build()
                .toString()

    private val fewDaysAgo: String
        get() = OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 3)
                .setIsActive(true)
                .build()
                .toString()

    @Test
    fun search_OneInTitle() {
        testUtils.setupBook("book-a", "* Note $now")
        activityRule.launchActivity(null)
        searchForText("e.ge.today")
        onNotesInSearch().check(matches(recyclerViewItemCount(1)))
    }

    @Test
    fun search_OneInContent() {
        testUtils.setupBook("book-a", "* Note\n$now")
        activityRule.launchActivity(null)
        searchForText("e.ge.today")
        onNotesInSearch().check(matches(recyclerViewItemCount(1)))
    }

    @Test
    fun search_TwoSameInContent() {
        testUtils.setupBook("book-a", "* Note\n$now $now")
        activityRule.launchActivity(null)
        searchForText("e.ge.today")
        onNotesInSearch().check(matches(recyclerViewItemCount(1)))
    }

    @Test
    fun agenda_OneInTitle() {
        testUtils.setupBook("book-a", "* Note $now")
        activityRule.launchActivity(null)
        searchForText("ad.1")
        onNotesInAgenda().check(matches(recyclerViewItemCount(2)))
    }

    @Test
    fun agenda_TwoInTitle() {
        testUtils.setupBook("book-a", "* Note $now $tomorrow")
        activityRule.launchActivity(null)
        searchForText("ad.2")
        onNotesInAgenda().check(matches(recyclerViewItemCount(4)))
    }

    @Test
    fun agenda_OneInContent() {
        testUtils.setupBook("book-a", "* Note\n$now")
        activityRule.launchActivity(null)
        searchForText("ad.1")
        onNotesInAgenda().check(matches(recyclerViewItemCount(2)))
    }

    @Test
    fun agenda_TwoInContent() {
        testUtils.setupBook("book-a", "* Note\n$now $tomorrow")
        activityRule.launchActivity(null)
        searchForText("ad.2")
        onNotesInAgenda().check(matches(recyclerViewItemCount(4)))
    }

    private fun time(offset: Long = 0, hasTime: Boolean = false): OrgDateTime {
        return OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis() + offset)
                .setHasTime(hasTime)
                .setIsActive(true)
                .build()
    }

    @Test
    fun agenda_MultipleWithTimes() {
        testUtils.setupBook("book-a", """
            * Note
            SCHEDULED: ${time(1000 * 60 * 60 * 24 * 2)}
            DEADLINE: ${time()}

            Now: ${time(hasTime = true)}
            In one hour: ${time(1000 * 60 * 60, hasTime = true)}
            Tomorrow: ${time(1000 * 60 * 60 * 24, hasTime = true)}"
        """.trimIndent())

        activityRule.launchActivity(null)

        searchForText("ad.5")

        SystemClock.sleep(10000)

        onNotesInAgenda().check(matches(recyclerViewItemCount(10)))

        // Today: deadline
        onItemInAgenda(1, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())))
        onItemInAgenda(1, R.id.item_head_deadline_text).check(matches(isDisplayed()))
        onItemInAgenda(1, R.id.item_head_event_text).check(matches(not(isDisplayed())))

        // Today: event
        onItemInAgenda(2, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())))
        onItemInAgenda(2, R.id.item_head_deadline_text).check(matches(not(isDisplayed())))
        onItemInAgenda(2, R.id.item_head_event_text).check(matches(isDisplayed()))

        // Today: event
        onItemInAgenda(3, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())))
        onItemInAgenda(3, R.id.item_head_deadline_text).check(matches(not(isDisplayed())))
        onItemInAgenda(3, R.id.item_head_event_text).check(matches(isDisplayed()))

        // Tomorrow: event
        onItemInAgenda(5, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())))
        onItemInAgenda(5, R.id.item_head_deadline_text).check(matches(not(isDisplayed())))
        onItemInAgenda(5, R.id.item_head_event_text).check(matches(isDisplayed()))

        // In two days: scheduled
        onItemInAgenda(7, R.id.item_head_scheduled_text).check(matches(isDisplayed()))
        onItemInAgenda(7, R.id.item_head_deadline_text).check(matches(not(isDisplayed())))
        onItemInAgenda(7, R.id.item_head_event_text).check(matches(not(isDisplayed())))
    }

    @Test
    fun search_MultipleWithTimes() {
        testUtils.setupBook("book-a", """
            * Note
            SCHEDULED: ${time(1000 * 60 * 60 * 24 * 2)}
            DEADLINE: ${time()}

            Now: ${time(hasTime = true)}
            In one hour: ${time(1000 * 60 * 60, hasTime = true)}
            Tomorrow: ${time(1000 * 60 * 60 * 24, hasTime = true)}"
        """.trimIndent())

        activityRule.launchActivity(null)
        searchForText("b.book-a")

        onNotesInSearch().check(matches(recyclerViewItemCount(1)))

        onNoteInSearch(0, R.id.item_head_scheduled_text).check(matches(isDisplayed()))
        onNoteInSearch(0, R.id.item_head_deadline_text).check(matches(isDisplayed()))
        onNoteInSearch(0, R.id.item_head_event_text).check(matches(isDisplayed()))
    }

    @Test
    fun search_TodayAndInFewDays() {
        testUtils.setupBook(
                "book-a",
                "* Today $today\n* In few days $inFewDays\n* Today & In few days $today $inFewDays")
        activityRule.launchActivity(null)
        searchForText("e.gt.1d")
        onNotesInSearch().check(matches(recyclerViewItemCount(2)))
    }

    @Test
    fun agenda_PastEvent() {
        testUtils.setupBook("book-a", "* Few days ago\n$fewDaysAgo")
        activityRule.launchActivity(null)
        searchForText("ad.2")
        onNotesInAgenda().check(matches(recyclerViewItemCount(2)))
    }

    @Test
    fun agendaSearch_TwoWithScheduledTime() {
        testUtils.setupBook("book-a", "* $yesterday $fewDaysAgo\nSCHEDULED: $tomorrow")
        activityRule.launchActivity(null)
        searchForText("e.lt.now ad.3")
        onNotesInAgenda().check(matches(recyclerViewItemCount(4)))
    }

    @Test
    fun search_MultiplePerNote_Today() {
        testUtils.setupBook(
                "Book A",
                """
                * Note A-01
                  $today $tomorrow
                """.trimIndent())
        activityRule.launchActivity(null)
        searchForText("e.today")
        onNotesInSearch().check(matches(recyclerViewItemCount(1)))
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText(startsWith("Note A-01"))))
    }

    @Test
    fun search_MultiplePerNote_OrderBy() {
        testUtils.setupBook(
                "Book A",
                """
                * Note A-01
                  <2000-01-10> <2000-01-15> <2000-01-20>
                * Note A-02
                  <2000-01-12>
                """.trimIndent())
        activityRule.launchActivity(null)
        searchForText("e.lt.now o.e")
        onNotesInSearch().check(matches(recyclerViewItemCount(2)))
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText(startsWith("Note A-01"))))
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText(startsWith("Note A-02"))))
    }

    @Test
    fun search_MultiplePerNote_OrderByDesc() {
        testUtils.setupBook(
                "Book A",
                """
                * Note A-01
                  <2000-01-10> <2000-01-15> <2000-01-20>
                * Note A-02
                  <2000-01-12>
                """.trimIndent())
        activityRule.launchActivity(null)
        searchForText("e.lt.now .o.e")
        onNotesInSearch().check(matches(recyclerViewItemCount(2)))
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText(startsWith("Note A-01"))))
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText(startsWith("Note A-02"))))
    }

    @Test
    fun shiftFromList() {
        testUtils.setupBook("Book A", "* Note A-01 <2000-01-10 +1d>")
        activityRule.launchActivity(null)
        onBook(0).perform(click())
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note A-01 <2000-01-10 +1d>")))
        onNoteInBook(1).perform(longClick())
        onView(withId(R.id.bottom_action_bar_done)).perform(click())
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note A-01 <2000-01-11 Tue +1d>")))
    }

    @Test
    fun shiftFromNote() {
        testUtils.setupBook("Book A", "* Note A-01 <2000-01-10 +1d>")
        activityRule.launchActivity(null)
        onBook(0).perform(click())
        onNoteInBook(1).perform(click())
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note A-01 <2000-01-10 +1d>")))
        onView(withId(R.id.fragment_note_state_button)).perform(click())
        onView(withText("DONE")).perform(click())
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note A-01 <2000-01-11 Tue +1d>")))
    }
}

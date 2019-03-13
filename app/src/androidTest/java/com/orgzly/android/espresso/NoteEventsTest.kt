package com.orgzly.android.espresso

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.orgzly.R
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.espresso.EspressoUtils.*
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.org.datetime.OrgDateTime
import org.hamcrest.Matchers.startsWith
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class NoteEventsTest : OrgzlyTest() {
    @get:Rule
    var activityRule = EspressoActivityTestRule(MainActivity::class.java, true, false)

    private val now: String
            get() = OrgDateTime(true).toString()

    private val today: String
        get() = OrgDateTime.Builder()
                .setDay(System.currentTimeMillis())
                .setIsActive(true)
                .build()
                .toString()

    private val tomorrow: String
            get() = OrgDateTime.Builder()
                    .setDay(System.currentTimeMillis() + 86400000L)
                    .setIsActive(true)
                    .build()
                    .toString()

    private val inFewDays: String
        get() = OrgDateTime.Builder()
                .setDay(System.currentTimeMillis() + 86400000L * 3)
                .setIsActive(true)
                .build()
                .toString()

    private val yesterday: String
        get() = OrgDateTime.Builder()
                .setDay(System.currentTimeMillis() - 86400000L)
                .setIsActive(true)
                .build()
                .toString()

    private val fewDaysAgo: String
        get() = OrgDateTime.Builder()
                .setDay(System.currentTimeMillis() - 86400000L * 3)
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

    @Ignore("Timestamp used when ordering is not defined due to grouping by note ID only")
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

    @Ignore("Timestamp used when ordering is not defined due to grouping by note ID only")
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
}

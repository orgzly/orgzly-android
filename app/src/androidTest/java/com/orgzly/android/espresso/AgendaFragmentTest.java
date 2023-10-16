package com.orgzly.android.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.util.EspressoUtils.onItemInAgenda;
import static com.orgzly.android.espresso.util.EspressoUtils.onNotesInAgenda;
import static com.orgzly.android.espresso.util.EspressoUtils.recyclerViewItemCount;
import static com.orgzly.android.espresso.util.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import android.content.pm.ActivityInfo;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.PickerActions;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

public class AgendaFragmentTest extends OrgzlyTest {
    private ActivityScenario<MainActivity> defaultSetUp() {
        testUtils.setupBook("book-one",
                "First book used for testing\n" +

                        "* Note A.\n" +

                        "* TODO Note B\n" +
                        "SCHEDULED: <2014-01-01>\n" +

                        "*** TODO Note C\n" +
                        "SCHEDULED: <2014-01-02 ++1d>\n");

        testUtils.setupBook("book-two",
                "Sample book used for tests\n" +

                        "*** DONE Note 1\n" +
                        "CLOSED: [2014-01-03 Tue 13:34]\n" +

                        "**** Note 2\n" +
                        "SCHEDULED: <2014-01-04 Sat>--<2044-01-10 Fri>\n");

        return ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void testAgendaSavedSearch() {
        defaultSetUp();
        searchForText(".it.done ad.7");
        /*
         * 1 Overdue
         * 1 Note B
         * 1 Note C
         * 1 Note 2
         * 7 Day
         * 7 Note C
         * 7 Note 2
         */
        onView(withId(R.id.fragment_query_agenda_recycler_view)).check(matches(recyclerViewItemCount(25)));
    }

    @Test
    public void testWithNoBook() {
        ActivityScenario.launch(MainActivity.class);
        searchForText(".it.done (s.7d or d.7d) ad.7");
        onNotesInAgenda().check(matches(recyclerViewItemCount(7)));
        searchForText(".it.done (s.7d or d.7d) ad.3");
        onNotesInAgenda().check(matches(recyclerViewItemCount(3)));
    }

    @Test
    public void testDayAgenda() {
        defaultSetUp();
        searchForText(".it.done (s.7d or d.7d) ad.1");
        onNotesInAgenda().check(matches(recyclerViewItemCount(7)));
        onItemInAgenda(0, R.id.item_agenda_divider_text).check(matches(allOf(withText(R.string.overdue), isDisplayed())));
        onItemInAgenda(1, R.id.item_head_title_view).check(matches(allOf(withText(endsWith("Note B")), isDisplayed())));
        onItemInAgenda(2, R.id.item_head_title_view).check(matches(allOf(withText(endsWith("Note C")), isDisplayed())));
        onItemInAgenda(3, R.id.item_head_title_view).check(matches(allOf(withText(endsWith("Note 2")), isDisplayed())));
        // Day 1
        onItemInAgenda(5, R.id.item_head_title_view).check(matches(allOf(withText(endsWith("Note C")), isDisplayed())));
        onItemInAgenda(6, R.id.item_head_title_view).check(matches(allOf(withText(endsWith("Note 2")), isDisplayed())));
    }

    @Test
    public void testAgendaRangeEvent() {
        DateTime start = DateTime.now().withTimeAtStartOfDay();
        DateTime end = DateTime.now().withTimeAtStartOfDay().plusDays(4);
        testUtils.setupBook("book", "Book for testing\n" +
                            "* Event A.\n" +
                            "<" + start.toString() + ">--<" + end.toString() + ">\n");

        ActivityScenario.launch(MainActivity.class);
        searchForText("ad.5");
        onNotesInAgenda().check(matches(recyclerViewItemCount(10)));
    }

    @Test
    public void testOneTimeTaskMarkedDone() {
        defaultSetUp();
        searchForText(".it.done ad.7");
        /*
         * 1 Overdue
         * 1 Note B   <- Mark as done
         * 1 Note C
         * 1 Note 2
         * 7 Day
         * 7 Note C
         * 7 Note 2
         */
        onItemInAgenda(1).perform(longClick());
        onView(withId(R.id.toggle_state)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(24)));
    }

    @Test
    public void testRepeaterTaskMarkedDone() {
        defaultSetUp();
        searchForText(".it.done ad.7");
        onItemInAgenda(2).perform(longClick());
        onView(withId(R.id.toggle_state)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(23)));
    }

    @Test
    public void testRangeTaskMarkedDone() {
        defaultSetUp();
        searchForText(".it.done ad.7");
        onItemInAgenda(3).perform(longClick());
        onView(withId(R.id.toggle_state)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(17)));
    }

    @Test
    public void testMoveTaskWithRepeaterToTomorrow() {
        DateTime tomorrow = DateTime.now().withTimeAtStartOfDay().plusDays(1);

        defaultSetUp();
        searchForText(".it.done ad.7");
        onItemInAgenda(2).perform(longClick());
        onView(withId(R.id.schedule)).perform(click());
        onView(withId(R.id.date_picker_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        tomorrow.getYear(),
                        tomorrow.getMonthOfYear(),
                        tomorrow.getDayOfMonth()));
        onView(withText(android.R.string.ok)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(23)));
    }

    @Test
    public void testPersistedSpinnerSelection() {
        ActivityScenario<MainActivity> scenario = defaultSetUp();

        searchForText(".it.done ad.7");
        onNotesInAgenda().check(matches(recyclerViewItemCount(25)));

        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));

        onNotesInAgenda().check(matches(recyclerViewItemCount(25)));
    }

    @Test
    public void testDeselectRemovedNoteInAgenda() {
        testUtils.setupBook(
                "notebook",
                "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>\n" +
                "* TODO Note B\nSCHEDULED: <2018-01-01 .+1d>\n");

        ActivityScenario.launch(MainActivity.class);

        searchForText("i.todo ad.3");

        onNotesInAgenda().check(matches(recyclerViewItemCount(12)));

        onItemInAgenda(1).perform(longClick());

        // Check title for number of selected notes
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.top_toolbar))))
                .check(matches(withText("1")));

        // Remove state
        onView(withId(R.id.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onNotesInAgenda().check(matches(recyclerViewItemCount(8)));

        // Check subtitle for search query
        onView(allOf(instanceOf(TextView.class), not(withText(R.string.agenda)), withParent(withId(R.id.top_toolbar))))
                .check(matches(withText("i.todo ad.3")));
    }

    @Ignore("Not implemented yet")
    @Test
    public void testPreselectedStateOfSelectedNote() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        ActivityScenario.launch(MainActivity.class);

        searchForText("ad.3");

        onItemInAgenda(1).perform(longClick());
        onView(withId(R.id.state)).perform(click());

        onView(withText("TODO")).check(matches(isChecked()));
    }

    @Test
    public void testSwipeDivider() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        ActivityScenario.launch(MainActivity.class);
        searchForText("ad.3");
        onItemInAgenda(0).perform(swipeLeft());
        onItemInAgenda(2).perform(swipeLeft());
    }

    /* Tests correct mapping of agenda ID to note's DB ID. */
    @Test
    public void testOpenCorrectNote() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        ActivityScenario.launch(MainActivity.class);

        searchForText("ad.3");

        onItemInAgenda(1).perform(click());

        onView(withId(R.id.scroll_view)).check(matches(isDisplayed()));
        onView(withId(R.id.title_view)).check(matches(withText("Note A")));
    }

    @Test
    public void testChangeStateWithReverseNoteClick() {
        testUtils.setupBook("book-1","* DONE Note A");
        testUtils.setupBook("book-2","* TODO Note B\nSCHEDULED: <2014-01-01>\n* TODO Note C\nSCHEDULED: <2014-01-02>\n");
        AppPreferences.isReverseNoteClickAction(context, false);
        ActivityScenario.launch(MainActivity.class);

        searchForText(".it.done ad.7");
        onItemInAgenda(1).perform(longClick());
        onView(withId(R.id.state)).perform(click());
        onView(withText("NEXT")).perform(click());
    }

    @Test
    public void testInactiveScheduled() {
        testUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: [2020-07-01]\nDEADLINE: <2020-07-01>");
        ActivityScenario.launch(MainActivity.class);
        searchForText("ad.1");
        // Overdue, note (deadline), today
        onNotesInAgenda().check(matches(recyclerViewItemCount(3)));
    }

    @Test
    public void testInactiveDeadline() {
        testUtils.setupBook("notebook-1", "* Note A\nDEADLINE: [2020-07-01]\nSCHEDULED: <2020-07-01>");
        ActivityScenario.launch(MainActivity.class);
        searchForText("ad.1");
        // Overdue, note (scheduled), today
        onNotesInAgenda().check(matches(recyclerViewItemCount(3)));
    }

    @Test
    public void testInactiveScheduledAndDeadline() {
        testUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: [2020-07-01]\nDEADLINE: [2020-07-01]");
        ActivityScenario.launch(MainActivity.class);
        searchForText("ad.1");
        // Today
        onNotesInAgenda().check(matches(recyclerViewItemCount(1)));
    }
}

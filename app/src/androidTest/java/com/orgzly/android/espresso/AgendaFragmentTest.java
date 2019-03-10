package com.orgzly.android.espresso;

import android.content.pm.ActivityInfo;
import android.widget.DatePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import androidx.test.espresso.contrib.PickerActions;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onItemInAgenda;
import static com.orgzly.android.espresso.EspressoUtils.onNotesInAgenda;
import static com.orgzly.android.espresso.EspressoUtils.recyclerViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

//@Ignore
public class AgendaFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class, true, false);

    private void defaultSetUp() {
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

        activityRule.launchActivity(null);
    }

    private void emptySetup() {
        activityRule.launchActivity(null);
    }

    private void openAgenda() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Agenda")).perform(click());
    }

    @Test
    public void testWithNoBook() {
        emptySetup();
        searchForText(".it.done (s.7d or d.7d) ad.7");
        onNotesInAgenda().check(matches(recyclerViewItemCount(7)));
        searchForText(".it.done (s.7d or d.7d) ad.3");
        onNotesInAgenda().check(matches(recyclerViewItemCount(3)));
    }

    @Test
    public void testDayAgenda() {
        defaultSetUp();
        searchForText(".it.done (s.7d or d.7d) ad.1");
        onNotesInAgenda().check(matches(recyclerViewItemCount(4)));
        onItemInAgenda(1, R.id.item_head_title).check(matches(allOf(withText(endsWith("Note B")), isDisplayed())));
        onItemInAgenda(2, R.id.item_head_title).check(matches(allOf(withText(endsWith("Note C")), isDisplayed())));
        onItemInAgenda(3, R.id.item_head_title).check(matches(allOf(withText(endsWith("Note 2")), isDisplayed())));
    }

    @Test
    public void testWeekAgenda() {
        defaultSetUp();
        openAgenda();
        // 7 date headers + 1 x Note B + 7 x Note C + 7 x Note 2
        onView(withId(R.id.fragment_query_agenda_recycler_view)).check(matches(recyclerViewItemCount(22)));
    }

    @Test
    public void testOneTimeTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        EspressoUtils.onItemInAgenda(1).perform(click());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(21)));
    }

    @Test
    public void testRepeaterTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        EspressoUtils.onItemInAgenda(2).perform(click());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(21)));
    }

    @Test
    public void testRangeTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        EspressoUtils.onItemInAgenda(3).perform(click());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(15)));
    }

    @Test
    public void testMoveTaskWithRepeaterToTomorrow() {
        DateTime tomorrow = DateTime.now().withTimeAtStartOfDay().plusDays(1);

        defaultSetUp();
        openAgenda();
        EspressoUtils.onItemInAgenda(2).perform(click());
        onView(withId(R.id.bottom_action_bar_schedule)).perform(click());
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        tomorrow.getYear(),
                        tomorrow.getMonthOfYear(),
                        tomorrow.getDayOfMonth()));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withText(R.string.set)).perform(click());
        onNotesInAgenda().check(matches(recyclerViewItemCount(21)));
    }

    @Test
    public void testPersistedSpinnerSelection() {
        defaultSetUp();
        openAgenda();
        onNotesInAgenda().check(matches(recyclerViewItemCount(22)));
        activityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onNotesInAgenda().check(matches(recyclerViewItemCount(22)));
    }

    @Test
    public void testDeSelectRemovedNoteInAgenda() {
        testUtils.setupBook(
                "notebook",
                "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>\n"
                + "* TODO Note B\nSCHEDULED: <2018-01-01 .+1d>\n");

        activityRule.launchActivity(null);

        searchForText("i.todo ad.3");

        EspressoUtils.onItemInAgenda(1).perform(click());

        onNotesInAgenda().check(matches(recyclerViewItemCount(9)));
        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        // Remove state from selected note
        onView(withId(R.id.bottom_action_bar_state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onNotesInAgenda().check(matches(recyclerViewItemCount(6)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }

    @Ignore // TODO: Implement
    @Test
    public void testPreselectedStateOfSelectedNote() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        activityRule.launchActivity(null);

        searchForText("ad.3");

        EspressoUtils.onItemInAgenda(1).perform(click());

        onView(withId(R.id.bottom_action_bar_state)).perform(click());

        onView(withText("TODO")).check(matches(isChecked()));
    }

    @Test
    public void testSwipeDivider() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        activityRule.launchActivity(null);
        searchForText("ad.3");
        onItemInAgenda(0).perform(swipeLeft());
    }

    @Test
    public void testOpenBookFromAgendaBySwiping() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        activityRule.launchActivity(null);
        searchForText("ad.3");
        onItemInAgenda(1).perform(swipeLeft());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    /* Tests correct mapping of agenda ID to note's DB ID. */
    @Test
    public void testOpenCorrectNote() {
        testUtils.setupBook("notebook", "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>");
        activityRule.launchActivity(null);

        searchForText("ad.3");

        onItemInAgenda(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note A")));
    }

    @Test
    public void testChangeStateWithReverseNoteClick() {
        testUtils.setupBook("book-1","* DONE Note A");
        testUtils.setupBook("book-2","* TODO Note B\nSCHEDULED: <2014-01-01>\n* TODO Note C\nSCHEDULED: <2014-01-02>\n");
        AppPreferences.isReverseNoteClickAction(context, false);
        activityRule.launchActivity(null);

        openAgenda();
        onItemInAgenda(1).perform(longClick());
        onView(withId(R.id.bottom_action_bar_state)).perform(click());
        onView(withText("NEXT")).perform(click());
    }
}

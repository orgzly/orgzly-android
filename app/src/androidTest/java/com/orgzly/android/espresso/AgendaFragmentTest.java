package com.orgzly.android.espresso;

import android.content.pm.ActivityInfo;
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.MainActivity;

import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onList;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

public class AgendaFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private void defaultSetUp() {
        shelfTestUtils.setupBook("book-one",
                "First book used for testing\n" +

                        "* Note A.\n" +

                        "* TODO Note B\n" +
                        "SCHEDULED: <2014-01-01>\n" +

                        "*** TODO Note C\n" +
                        "SCHEDULED: <2014-01-02 ++1d>\n");

        shelfTestUtils.setupBook("book-two",
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
        onList().check(matches(listViewItemCount(7)));
        searchForText(".it.done (s.7d or d.7d) ad.3");
        onList().check(matches(listViewItemCount(3)));
    }

    @Test
    public void testDayAgenda() {
        defaultSetUp();
        searchForText(".it.done (s.7d or d.7d) ad.1");
        onList().check(matches(listViewItemCount(4)));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(endsWith("Note B")), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(endsWith("Note C")), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(endsWith("Note 2")), isDisplayed())));
    }

    @Test
    public void testWeekAgenda() {
        defaultSetUp();
        openAgenda();
        // 7 date headers, 1 Note B, 7 x Note C, 7 x Note 2
        onList().check(matches(listViewItemCount(22)));
    }

    @Test
    public void testOneTimeTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        onListItem(1).perform(swipeRight());
        onListItem(1).onChildView(withId(R.id.item_menu_done_state_btn)).perform(click());
        onList().check(matches(listViewItemCount(21)));
    }

    @Test
    public void testRepeaterTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        onListItem(2).perform(swipeRight());
        onListItem(2).onChildView(withId(R.id.item_menu_done_state_btn)).perform(click());
        onList().check(matches(listViewItemCount(21)));
    }

    @Test
    public void testRangeTaskMarkedDone() {
        defaultSetUp();
        openAgenda();
        onListItem(3).perform(swipeRight());
        onListItem(3).onChildView(withId(R.id.item_menu_done_state_btn)).perform(click());
        onList().check(matches(listViewItemCount(15)));
    }

    @Test
    public void testShiftRepeaterTaskToTomorrow() {
        DateTime tomorrow = DateTime.now().withTimeAtStartOfDay().plusDays(1);

        defaultSetUp();
        openAgenda();
        onListItem(2).perform(swipeRight());
        onListItem(2).onChildView(withId(R.id.item_menu_schedule_btn)).perform(click());
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        tomorrow.getYear(),
                        tomorrow.getMonthOfYear(),
                        tomorrow.getDayOfMonth()));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withText(R.string.set)).perform(click());
        onList().check(matches(listViewItemCount(21)));
    }

    @Test
    public void testPersistedSpinnerSelection() {
        defaultSetUp();
        openAgenda();
        onList().check(matches(listViewItemCount(22)));
        activityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onList().check(matches(listViewItemCount(22)));
    }

    @Test
    public void testClickAndChangeStateWithReverseNoteClick() {
        shelfTestUtils.setupBook("book-1","* DONE Note A");
        shelfTestUtils.setupBook("book-2","* TODO Note B\nSCHEDULED: <2014-01-01>\n* TODO Note C\nSCHEDULED: <2014-01-02>\n");
        AppPreferences.isReverseNoteClickAction(context, true);
        activityRule.launchActivity(null);

        openAgenda();
        onListItem(2).perform(click());
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText(R.string.state)).perform(click());
        onView(withText("NEXT")).perform(click());
    }

    @Test
    public void testOpenNoteWithReverseNoteClick() {
        shelfTestUtils.setupBook("book-1","* DONE Note A");
        shelfTestUtils.setupBook("book-2","* TODO Note B\nSCHEDULED: <2014-01-01>\n* TODO Note C\nSCHEDULED: <2014-01-02>\n");
        AppPreferences.isReverseNoteClickAction(context, true);
        activityRule.launchActivity(null);

        openAgenda();
        onListItem(2).perform(longClick());
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note C")));
    }

    @Test
    public void testDeSelectRemovedNoteInAgenda() {
        shelfTestUtils.setupBook(
                "notebook",
                "* TODO Note A\nSCHEDULED: <2018-01-01 +1d>\n"
                + "* TODO Note B\nSCHEDULED: <2018-01-01 .+1d>\n");

        activityRule.launchActivity(null);

        searchForText("i.todo ad.3");

        onListItem(1).perform(longClick());

        onList().check(matches(listViewItemCount(9)));
        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        // Remove state from selected note
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText(R.string.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onList().check(matches(listViewItemCount(6)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }
}

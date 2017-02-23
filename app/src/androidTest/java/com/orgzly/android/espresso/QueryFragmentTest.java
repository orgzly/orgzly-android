package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.PickerActions.setDate;
import static android.support.test.espresso.contrib.PickerActions.setTime;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 */
@SuppressWarnings("unchecked")
public class QueryFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private void defaultSetUp() {
        shelfTestUtils.setupBook("book-one",
                "First book used for testing\n" +
                "* Note A.\n" +
                "** [#A] Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "*** TODO Note E.\n" +
                "*** Same title in different notebooks.\n" +
                "*** Another note.\n" +
                "");

        shelfTestUtils.setupBook("book-two",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* DONE Note #8.\n" +
                "CLOSED: [2014-06-03 Tue 3:34]\n" +
                "**** Note #9.\n" +
                "SCHEDULED: <2014-05-26 Mon>\n" +
                "** Note #10.\n" +
                "** Same title in different notebooks.\n" +
                "** Note #11.\n" +
                "** Note #12.\n" +
                "** Note #13.\n" +
                "DEADLINE: <2014-05-26 Mon>\n" +
                "** Note #14.\n" +
                "** [#A] Note #15.\n" +
                "** [#A] Note #16.\n" +
                "** [#B] Note #17.\n" +
                "** [#C] Note #18.\n" +
                "** Note #19.\n" +
                "** Note #20.\n" +
                "** Note #21.\n" +
                "** Note #22.\n" +
                "** Note #23.\n" +
                "** Note #24.\n" +
                "** Note #25.\n" +
                "** Note #26.\n" +
                "** Note #27.\n" +
                "** Note #28.\n" +
                "");

        activityRule.launchActivity(null);
    }

    @Test
    public void testSearchFromBookOneResult() {
        defaultSetUp();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("b.book-one another note");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(1)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Another note."), isDisplayed())));
    }

    @Test
    public void testSearchFromBookMultipleResults() {
        defaultSetUp();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(7)));
    }

    @Test
    public void testSearchTwice() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("different");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("another");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(1)));
    }

    @Test
    public void testSearchExpressionTodo() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("i.todo");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(3)));
    }

    @Test
    public void testSearchExpressionsToday() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("s.today");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
    }

    @Test
    public void testSearchExpressionsPriority() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("p.a");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));

        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(3)));

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note B."), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #15."), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #16."), isDisplayed())));
    }


    @Test
    public void testSearchInBook() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(7)));
    }

    /**
     * Starting with 3 displayed to-do notes, removing state from one, expecting 2 to-do notes left.
     */
    @Test
    public void testEditChangeState() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("To Do")).perform(click());
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(3)));
        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("NOTE")).perform(click());
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
    }

    /**
     * Clicks on the last note and expects it opened.
     */
    @Test
    public void testClickingNote() {
        defaultSetUp();

        onView(allOf(withText("book-two"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("b.book-two Note");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(29)));
        onListItem(27).perform(click());
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
        onView(withText("Note #28.")).check(matches(isDisplayed()));
    }

    @Test
    public void testSchedulingNote() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));

        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText("Schedule")).perform(click());

        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText("OK"), withText("Set"), withText("Done"))).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText("OK"), withText("Set"), withText("Done"))).perform(click());
        onView(withText("Set")).perform(click());

        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
        onView(withText("2014-04-01 Tue 09:15")).check(matches(isDisplayed()));
    }

    @Test
    public void testSearchExpressionsDefaultPriority() {
        shelfTestUtils.setupBook("book-one",
                "* Note A.\n" +
                "** [#A] Note B.\n" +
                "* TODO [#B] Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** [#C] Note D.\n" +
                "*** TODO Note E.");
        shelfTestUtils.setupBook("book-two", "* Note #1.\n");
        activityRule.launchActivity(null);

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("p.b");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));

        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(4)));
    }

    @Test
    public void testMultipleNotState() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A.\n" +
                "** [#A] Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "");
        shelfTestUtils.setupBook("notebook-2",
                "* Note #1.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText(".i.todo .i.done");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(5)));
    }

    /**
     * Added after a bug when using insertWithOnConflict for timestamps,
     * due to https://code.google.com/p/android/issues/detail?id=13045
     */
    @Test
    public void testNotesWithSameScheduledTimeString() throws IOException {
        shelfTestUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: <2014-01-01>");
        shelfTestUtils.setupBook("notebook-2", "* Note B\nSCHEDULED: <2014-01-01>");
        activityRule.launchActivity(null);

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("s.today");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
    }
    @Test
    public void testNotesWithSameDeadlineTimeString() throws IOException {
        shelfTestUtils.setupBook("notebook-1", "* Note A\nDEADLINE: <2014-01-01>");
        shelfTestUtils.setupBook("notebook-2", "* Note B\nDEADLINE: <2014-01-01>");
        activityRule.launchActivity(null);

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("d.today");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(2)));
    }

    @Test
    public void testInheritedTagSearchWhenMultipleAncestorsMatch() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A :tagtag:\n" +
                "** Note B :tag:\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("t.tag");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withId(android.R.id.list), isDisplayed())).check(matches(listViewItemCount(4)));
    }

    @Test
    public void testSearchOrderScheduled() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A\n" +
                "SCHEDULED: <2014-02-01>\n" +
                "** Note B\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("note o.scheduled");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note B")));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note A")));
    }

    @Test
    public void testNotebookNameInListAfterRename() {
        defaultSetUp();

        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        searchForText("note");
        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_book_name_text)).check(matches(withText("book-one")));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Notebooks")).perform(click());

        onListItem(0).perform(longClick());
        onView(withText("Rename")).perform(click());
        onView(withId(R.id.name)).perform(replaceText("renamed book-one"), closeSoftKeyboardWithDelay());
        onView(withText("Rename")).perform(click());

        /* The other book is now first. Rename it too to keep the order of notes the same. */
        onListItem(0).perform(longClick());
        onView(withText("Rename")).perform(click());
        onView(withId(R.id.name)).perform(replaceText("renamed book-two"), closeSoftKeyboardWithDelay());
        onView(withText("Rename")).perform(click());

        pressBack();

        onView(withId(R.id.fragment_query_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_book_name_text)).check(matches(withText("renamed book-one")));
    }
}

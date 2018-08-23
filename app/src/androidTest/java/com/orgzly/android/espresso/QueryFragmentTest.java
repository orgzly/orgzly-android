package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
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
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.PickerActions.setDate;
import static android.support.test.espresso.contrib.PickerActions.setTime;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onList;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

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
        searchForText("b.book-one another note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(1)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Another note."), isDisplayed())));
    }

    @Test
    public void testSearchFromBookMultipleResults() {
        defaultSetUp();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(7)));
    }

    @Test
    public void testSearchTwice() {
        defaultSetUp();

        searchForText("different");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        searchForText("another");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(1)));
    }

    @Test
    public void testSearchExpressionTodo() {
        defaultSetUp();

        searchForText("i.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(3)));
    }

    @Test
    public void testSearchExpressionsToday() {
        defaultSetUp();

        searchForText("s.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
    }

    @Test
    public void testSearchExpressionsPriority() {
        defaultSetUp();

        searchForText("p.a");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onList().check(matches(listViewItemCount(3)));

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note B."), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #15."), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #16."), isDisplayed())));
    }

    @Test
    public void testNotPriority() {
        defaultSetUp();

        searchForText(".p.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onList().check(matches(listViewItemCount(4)));

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note B."), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #15."), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#A  Note #16."), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("#C  Note #18."), isDisplayed())));
    }


    @Test
    public void testSearchInBook() {
        defaultSetUp();

        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(7)));
    }

    /**
     * Starting with 3 displayed to-do notes, removing state from one, expecting 2 to-do notes left.
     */
    @Test
    public void testEditChangeState() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("To Do")).perform(click());
        onList().check(matches(listViewItemCount(3)));
        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText(R.string.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onList().check(matches(listViewItemCount(2)));
    }

    /**
     * Clicks on the last note and expects it opened.
     */
    @Test
    public void testClickingNote() {
        defaultSetUp();

        onView(allOf(withText("book-two"), isDisplayed())).perform(click());
        searchForText("b.book-two Note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(29)));
        onListItem(27).perform(click());
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
        onView(withText("Note #28.")).check(matches(isDisplayed()));
    }

    @Test
    public void testSchedulingNote() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());
        onList().check(matches(listViewItemCount(2)));

        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText(R.string.schedule)).perform(click());

        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withText(R.string.set)).perform(click());

        onList().check(matches(listViewItemCount(2)));
        onView(withText(userDateTime("<2014-04-01 Tue 09:15>"))).check(matches(isDisplayed()));
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

        searchForText("p.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onList().check(matches(listViewItemCount(4)));
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

        searchForText(".i.todo .i.done");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(5)));
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

        searchForText("s.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
    }
    @Test
    public void testNotesWithSameDeadlineTimeString() throws IOException {
        shelfTestUtils.setupBook("notebook-1", "* Note A\nDEADLINE: <2014-01-01>");
        shelfTestUtils.setupBook("notebook-2", "* Note B\nDEADLINE: <2014-01-01>");
        activityRule.launchActivity(null);

        searchForText("d.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
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
        searchForText("t.tag");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(4)));
    }

    @Test
    public void testInheritedAndOwnTag() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "** Note B :tag2:\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("t.tag1 t.tag2");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(3)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(startsWith("Note B")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(startsWith("Note C")), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(startsWith("Note D")), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterMovingNote() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "** Note B :tag2:\n" +
                "*** Note C :tag3:\n" +
                "*** Note D :tag3:\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Move Note C down. */
        onListItem(2).perform(longClick());
        onView(withId(R.id.book_cab_move)).perform(click());
        onView(withId(R.id.notes_action_move_down)).perform(click());
        pressBack();

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note D  tag3 • tag2 tag1"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note C  tag3 • tag2 tag1"), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterDemotingSubtree() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "* Note B :tag2:\n" +
                "** Note C :tag3:\n" +
                "** Note D :tag3:\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Demote Note B. */
        onListItem(1).perform(longClick());
        onView(withId(R.id.book_cab_move)).perform(click());
        onView(withId(R.id.notes_action_move_right)).perform(click());
        pressBack();

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note C  tag3 • tag1 tag2"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note D  tag3 • tag1 tag2"), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterCutAndPasting() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "* Note B :tag2:\n" +
                "** Note C :tag3:\n" +
                "** Note D :tag3:\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Cut Note B. */
        onListItem(1).perform(longClick());
        onView(withId(R.id.book_cab_cut)).perform(click());

        /* Paste under Note A. */
        onListItem(0).perform(longClick());
        onView(withId(R.id.book_cab_paste)).perform(click());
        onView(withText(R.string.heads_action_menu_item_paste_under)).perform(click());

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note C  tag3 • tag1 tag2"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note D  tag3 • tag1 tag2"), isDisplayed())));
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
        searchForText("note o.scheduled");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note B")));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note A")));
    }

    @Test
    public void testOrderScheduledWithAndWithoutTimePart() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note B\n" +
                "SCHEDULED: <2014-01-02>\n" +
                "*** Note C\n" +
                "SCHEDULED: <2014-01-02 10:00>\n" +
                "*** DONE Note D\n" +
                "SCHEDULED: <2014-01-03>\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("s.today .i.done o.s");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note A")));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note C")));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note B")));
    }

    @Test
    public void testOrderDeadlineWithAndWithoutTimePartDesc() {
        shelfTestUtils.setupBook("notebook-1",
                "* Note A\n" +
                "DEADLINE: <2014-01-01>\n" +
                "** Note B\n" +
                "DEADLINE: <2014-01-02>\n" +
                "*** Note C\n" +
                "DEADLINE: <2014-01-02 10:00>\n" +
                "*** DONE Note D\n" +
                "DEADLINE: <2014-01-03>\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("d.today .i.done .o.d");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note B")));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note C")));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note A")));
    }

    @Test
    public void testNotebookNameInListAfterRename() {
        defaultSetUp();

        searchForText("note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_book_name_text)).check(matches(withText("book-one")));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());

        onListItem(0).perform(longClick());
        onView(withText(R.string.books_context_menu_item_rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceText("renamed book-one"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.rename)).perform(click());

        /* The other book is now first. Rename it too to keep the order of notes the same. */
        onListItem(0).perform(longClick());
        onView(withText(R.string.books_context_menu_item_rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceText("renamed book-two"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.rename)).perform(click());

        pressBack();

        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_book_name_text)).check(matches(withText("renamed book-one")));
    }

    @Test
    public void testSearchForNonExistentTagShouldReturnAllNotes() {
        shelfTestUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".t.c");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(4)));
    }

    @Test
    public void testNotTagShouldReturnSomeNotes() {
        shelfTestUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".t.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note A  a"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note D"), isDisplayed())));
    }

    @Test
    public void testSearchForTagOrTag() {
        shelfTestUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("tn.a or tn.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(startsWith("Note A")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(startsWith("Note B")), isDisplayed())));
    }

    @Test
    public void testSortByPriority() {
        shelfTestUtils.setupBook("notebook",
                "* [#B] Note A :a:\n" +
                "** [#A] Note B :b:\n" +
                "*** [#C] Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("o.p");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
    }

    @Test
    public void testSortByPriorityDesc() {
        shelfTestUtils.setupBook("notebook",
                "* [#B] Note A :a:\n" +
                "** [#A] Note B :b:\n" +
                "*** [#C] Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".o.p");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
    }

    @Test
    public void testSearchNoteStateType() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        shelfTestUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".it.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
    }

    @Test
    public void testSearchStateType() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        shelfTestUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("it.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(2)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
    }

    @Test
    public void testSearchNoState() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        shelfTestUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("it.none");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(1)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
    }

    @Test
    public void testSearchWithState() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        shelfTestUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".it.none");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(3)));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
    }

    @Test
    public void testContentOfFoldedNoteDisplayed() {
        AppPreferences.isNotesContentDisplayedInSearch(context, true);
        shelfTestUtils.setupBook("notebook",
                "* Note A\n" +
                "** Note B\n" +
                "Content for Note B\n" +
                "* Note C\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_fold_button)).perform(click());
        searchForText("note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onList().check(matches(listViewItemCount(3)));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_content)).check(matches(allOf(withText(containsString("Content for Note B")), isDisplayed())));
    }

    @Test
    public void testDeSelectRemovedNoteInSearch() {
        shelfTestUtils.setupBook("notebook", "* TODO Note A\n* TODO Note B");
        activityRule.launchActivity(null);

        searchForText("i.todo");

        onListItem(0).perform(longClick());

        onList().check(matches(listViewItemCount(2)));
        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        // Remove state from selected note
        onView(withId(R.id.query_cab_edit)).perform(click());
        onView(withText(R.string.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onList().check(matches(listViewItemCount(1)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }
}

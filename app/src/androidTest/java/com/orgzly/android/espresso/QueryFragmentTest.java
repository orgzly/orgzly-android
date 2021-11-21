package com.orgzly.android.espresso;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.org.datetime.OrgDateTime;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.PickerActions.setDate;
import static androidx.test.espresso.contrib.PickerActions.setTime;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onBook;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInSearch;
import static com.orgzly.android.espresso.EspressoUtils.onNotesInSearch;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.recyclerViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

public class QueryFragmentTest extends OrgzlyTest {
    private void defaultSetUp() {
        testUtils.setupBook("book-one",
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

        testUtils.setupBook("book-two",
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

        ActivityScenario.launch(MainActivity.class);
    }

    @Test
    public void testSearchFromBookOneResult() {
        defaultSetUp();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        searchForText("b.book-one another note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText("Another note."), isDisplayed())));
    }

    @Test
    public void testSearchFromBookMultipleResults() {
        defaultSetUp();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(7)));
    }

    @Test
    public void testSearchTwice() {
        defaultSetUp();

        searchForText("different");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        searchForText("another");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
    }

    @Test
    public void testSearchExpressionTodo() {
        defaultSetUp();

        searchForText("i.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(3)));
    }

    @Test
    public void testSearchExpressionsToday() {
        defaultSetUp();

        searchForText("s.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
    }

    @Test
    public void testSearchExpressionsPriority() {
        defaultSetUp();

        searchForText("p.a");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onNotesInSearch().check(matches(recyclerViewItemCount(3)));

        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText("#A  Note B."), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText("#A  Note #15."), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText("#A  Note #16."), isDisplayed())));
    }

    @Test
    public void testNotPriority() {
        defaultSetUp();

        searchForText(".p.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onNotesInSearch().check(matches(recyclerViewItemCount(4)));

        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText("#A  Note B."), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText("#A  Note #15."), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText("#A  Note #16."), isDisplayed())));
        onNoteInSearch(3, R.id.item_head_title).check(matches(allOf(withText("#C  Note #18."), isDisplayed())));
    }


    @Test
    public void testSearchInBook() {
        defaultSetUp();

        searchForText("b.book-one note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(7)));
    }

    /**
     * Starting with 3 displayed to-do notes, removing state from one, expecting 2 to-do notes left.
     */
    @Test
    public void testEditChangeState() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("To Do")).perform(click());
        onNotesInSearch().check(matches(recyclerViewItemCount(3)));
        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.bottom_action_bar_state)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
    }

    @Test
    public void testToggleState() {
        testUtils.setupBook("book-one", "* Note");
        ActivityScenario.launch(MainActivity.class);

        searchForText("Note");
        onNoteInSearch(0).perform(longClick());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText(startsWith("DONE"))));
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
        onNotesInSearch().check(matches(recyclerViewItemCount(29)));
        onNoteInSearch(27).perform(click());
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withText("Note #28."), isDisplayed())).check(matches(isDisplayed()));
    }

    @Test
    public void testSchedulingNote() {
        defaultSetUp();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));

        onView(allOf(withText(endsWith("Note C.")), isDisplayed())).perform(longClick());
        onView(withId(R.id.bottom_action_bar_schedule)).perform(click());
        onView(withId(R.id.date_picker_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withId(R.id.time_picker_button)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withText(R.string.set)).perform(click());

        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onView(withText(userDateTime("<2014-04-01 Tue 09:15>"))).check(matches(isDisplayed()));
    }

    @Test
    public void testSearchExpressionsDefaultPriority() {
        testUtils.setupBook("book-one",
                "* Note A.\n" +
                "** [#A] Note B.\n" +
                "* TODO [#B] Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** [#C] Note D.\n" +
                "*** TODO Note E.");
        testUtils.setupBook("book-two", "* Note #1.\n");
        ActivityScenario.launch(MainActivity.class);

        searchForText("p.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));

        onNotesInSearch().check(matches(recyclerViewItemCount(4)));
    }

    @Test
    public void testMultipleNotState() {
        testUtils.setupBook("notebook-1",
                "* Note A.\n" +
                "** [#A] Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "");
        testUtils.setupBook("notebook-2",
                "* Note #1.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        searchForText(".i.todo .i.done");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(5)));
    }

    /**
     * Added after a bug when using insertWithOnConflict for timestamps,
     * due to https://code.google.com/p/android/issues/detail?id=13045
     */
    @Test
    public void testNotesWithSameScheduledTimeString() throws IOException {
        testUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: <2014-01-01>");
        testUtils.setupBook("notebook-2", "* Note B\nSCHEDULED: <2014-01-01>");
        ActivityScenario.launch(MainActivity.class);

        searchForText("s.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
    }

    @Test
    public void testNotesWithSameDeadlineTimeString() throws IOException {
        testUtils.setupBook("notebook-1", "* Note A\nDEADLINE: <2014-01-01>");
        testUtils.setupBook("notebook-2", "* Note B\nDEADLINE: <2014-01-01>");
        ActivityScenario.launch(MainActivity.class);

        searchForText("d.today");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
    }

    @Test
    public void testClosedTimeSearch() {
        testUtils.setupBook("notebook-1", "* Note A\nCLOSED: [2014-01-01]");
        ActivityScenario.launch(MainActivity.class);

        searchForText("c.ge.-2d");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onView(withText(R.string.no_notes_found_after_search)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(0)));
    }

    @Test
    public void testInheritedTagSearchWhenMultipleAncestorsMatch() {
        testUtils.setupBook("notebook-1",
                "* Note A :tagtag:\n" +
                "** Note B :tag:\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("t.tag");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(4)));
    }

    @Test
    public void testInheritedAndOwnTag() {
        testUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "** Note B :tag2:\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("t.tag1 t.tag2");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(3)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(startsWith("Note B")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(startsWith("Note C")), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText(startsWith("Note D")), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterMovingNote() {
        testUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "** Note B :tag2:\n" +
                "*** Note C :tag3:\n" +
                "*** Note D :tag3:\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Move Note C down. */
        onNoteInBook(3).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.move)).perform(click());
        onView(withId(R.id.notes_action_move_down)).perform(click());
        pressBack();

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note D  tag3 • tag2 tag1"), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title)
                .check(matches(allOf(withText("Note C  tag3 • tag2 tag1"), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterDemotingSubtree() {
        testUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "* Note B :tag2:\n" + // Demote
                "** Note C :tag3:\n" +
                "** Note D :tag3:\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Demote Note B. */
        onNoteInBook(2).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.move)).perform(click());
        onView(withId(R.id.notes_action_move_right)).perform(click());
        pressBack();

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note C  tag3 • tag1 tag2"), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title)
                .check(matches(allOf(withText("Note D  tag3 • tag1 tag2"), isDisplayed())));
    }

    @Test
    public void testInheritedTagsAfterCutAndPasting() {
        testUtils.setupBook("notebook-1",
                "* Note A :tag1:\n" +
                "* Note B :tag2:\n" +
                "** Note C :tag3:\n" +
                "** Note D :tag3:\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());

        /* Cut Note B. */
        onNoteInBook(2).perform(longClick());
        onView(withId(R.id.book_cab_cut)).perform(click());

        /* Paste under Note A. */
        onNoteInBook(1).perform(longClick());
        onView(withId(R.id.book_cab_paste)).perform(click());
        onView(withText(R.string.heads_action_menu_item_paste_under)).perform(click());

        searchForText("t.tag3");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note C  tag3 • tag1 tag2"), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title)
                .check(matches(allOf(withText("Note D  tag3 • tag1 tag2"), isDisplayed())));
    }

    @Test
    public void testSearchOrderScheduled() {
        testUtils.setupBook("notebook-1",
                "* Note A\n" +
                "SCHEDULED: <2014-02-01>\n" +
                "** Note B\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "*** Note C\n" +
                "*** Note D\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("note o.scheduled");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note B")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note A")));
    }

    @Test
    public void testOrderScheduledWithAndWithoutTimePart() {
        testUtils.setupBook("notebook-1",
                "* Note A\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note B\n" +
                "SCHEDULED: <2014-01-02>\n" +
                "*** Note C\n" +
                "SCHEDULED: <2014-01-02 10:00>\n" +
                "*** DONE Note D\n" +
                "SCHEDULED: <2014-01-03>\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("s.today .i.done o.s");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note A")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note C")));
        onNoteInSearch(2, R.id.item_head_title).check(matches(withText("Note B")));
    }

    @Test
    public void testOrderDeadlineWithAndWithoutTimePartDesc() {
        testUtils.setupBook("notebook-1",
                "* Note A\n" +
                "DEADLINE: <2014-01-01>\n" +
                "** Note B\n" +
                "DEADLINE: <2014-01-02>\n" +
                "*** Note C\n" +
                "DEADLINE: <2014-01-02 10:00>\n" +
                "*** DONE Note D\n" +
                "DEADLINE: <2014-01-03>\n" +
                "");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook-1"), isDisplayed())).perform(click());
        searchForText("d.today .i.done .o.d");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note B")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note C")));
        onNoteInSearch(2, R.id.item_head_title).check(matches(withText("Note A")));
    }

    @Test
    public void testOrderOfBooksAfterRenaming() {
        defaultSetUp();

        searchForText("note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_book_name_text).check(matches(withText("book-one")));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());

        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceTextCloseKeyboard("renamed book-one"));
        onView(withText(R.string.rename)).perform(click());

        /* The other book is now first. Rename it too to keep the order of notes the same. */
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceTextCloseKeyboard("renamed book-two"));
        onView(withText(R.string.rename)).perform(click());

        pressBack();

        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_book_name_text).check(matches(withText("renamed book-one")));
    }

    @Test
    public void testSearchForNonExistentTagShouldReturnAllNotes() {
        testUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".t.c");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(4)));
    }

    @Test
    public void testNotTagShouldReturnSomeNotes() {
        testUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".t.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText("Note A  a"), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText("Note D"), isDisplayed())));
    }

    @Test
    public void testSearchForTagOrTag() {
        testUtils.setupBook("notebook",
                "* Note A :a:\n" +
                "** Note B :b:\n" +
                "*** Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("tn.a or tn.b");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(startsWith("Note A")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(startsWith("Note B")), isDisplayed())));
    }

    @Test
    public void testSortByPriority() {
        testUtils.setupBook("notebook",
                "* [#B] Note A :a:\n" +
                "** [#A] Note B :b:\n" +
                "*** [#C] Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("o.p");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
        onNoteInSearch(3, R.id.item_head_title).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
    }

    @Test
    public void testSortByPriorityDesc() {
        testUtils.setupBook("notebook",
                "* [#B] Note A :a:\n" +
                "** [#A] Note B :b:\n" +
                "*** [#C] Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".o.p");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onNoteInSearch(3, R.id.item_head_title).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
    }

    @Test
    public void testSearchNoteStateType() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        testUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".it.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
    }

    @Test
    public void testSearchStateType() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        testUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("it.todo");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
    }

    @Test
    public void testSearchNoState() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        testUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText("it.none");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note D")), isDisplayed())));
    }

    @Test
    public void testSearchWithState() {
        AppPreferences.states(context, "TODO NEXT | DONE");
        testUtils.setupBook("notebook",
                "* TODO Note A :a:\n" +
                "** NEXT Note B :b:\n" +
                "* DONE Note C\n" +
                "* Note D\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        searchForText(".it.none");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(3)));
        onNoteInSearch(0, R.id.item_head_title).check(matches(allOf(withText(containsString("Note A")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onNoteInSearch(2, R.id.item_head_title).check(matches(allOf(withText(containsString("Note C")), isDisplayed())));
    }

    @Test
    public void testContentOfFoldedNoteDisplayed() {
        AppPreferences.isNotesContentDisplayedInSearch(context, true);
        testUtils.setupBook("notebook",
                "* Note A\n" +
                "** Note B\n" +
                "Content for Note B\n" +
                "* Note C\n");
        ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());
        onNoteInBook(1, R.id.item_head_fold_button).perform(click());
        searchForText("note");
        onView(withId(R.id.fragment_query_search_view_flipper)).check(matches(isDisplayed()));
        onNotesInSearch().check(matches(recyclerViewItemCount(3)));
        onNoteInSearch(1, R.id.item_head_title).check(matches(allOf(withText(containsString("Note B")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_content).check(matches(allOf(withText(containsString("Content for Note B")), isDisplayed())));
    }

    @Test
    public void testDeSelectRemovedNoteInSearch() {
        testUtils.setupBook("notebook", "* TODO Note A\n* TODO Note B");
        ActivityScenario.launch(MainActivity.class);

        searchForText("i.todo");

        onNoteInSearch(0).perform(longClick());

        onNotesInSearch().check(matches(recyclerViewItemCount(2)));
        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        // Remove state from selected note
        onView(withId(R.id.bottom_action_bar_state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }

    @Test
    public void testNoNotesFoundMessageIsDisplayedInSearch() {
        ActivityScenario.launch(MainActivity.class);
        searchForText("Note");
        onView(withText(R.string.no_notes_found_after_search)).check(matches(isDisplayed()));
    }

    @Ignore("Not implemented yet")
    @Test
    public void testPreselectedStateOfSelectedNote() {
        testUtils.setupBook("notebook", "* TODO Note A\n* TODO Note B");
        ActivityScenario.launch(MainActivity.class);

        searchForText("i.todo");

        onNoteInSearch(1).perform(longClick());

        onView(withId(R.id.bottom_action_bar_state)).perform(click());

        onView(withText("TODO")).check(matches(isChecked()));
    }

    @Test
    public void testSearchAndClickOnNoteWithTwoDifferentEvents() {
        testUtils.setupBook("notebook", "* Note\n<2000-01-01>\n<2000-01-02>");
        ActivityScenario.launch(MainActivity.class);
        searchForText("e.lt.now");
        onNoteInSearch(0).perform(click());
    }

    @Test
    public void testInactiveScheduled() {
        testUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: [2020-07-01]");
        ActivityScenario.launch(MainActivity.class);
        searchForText("s.le.today");
        onNotesInSearch().check(matches(recyclerViewItemCount(0)));
    }

    @Test
    public void testInactiveDeadline() {
        testUtils.setupBook("notebook-1", "* Note A\nDEADLINE: [2020-07-01]");
        ActivityScenario.launch(MainActivity.class);
        searchForText("d.le.today");
        onNotesInSearch().check(matches(recyclerViewItemCount(0)));
    }

    @Test
    public void testScheduledTimestamp() {
        String inOneHour = new OrgDateTime.Builder()
                .setDateTime(System.currentTimeMillis() + 1000 * 60 * 60)
                .setHasTime(true)
                .setIsActive(true)
                .build()
                .toString();

        testUtils.setupBook("notebook-1", "* Note A\nSCHEDULED: " + inOneHour);

        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        // Remove time usage
        onView(allOf(withText(endsWith("Note A")), isDisplayed())).perform(longClick());
        onView(withId(R.id.bottom_action_bar_schedule)).perform(click());
        onView(withId(R.id.time_used_checkbox)).perform(click());
        onView(withText(R.string.set)).perform(click());
        pressBack();

        searchForText("s.now");

        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
    }

    @Test
    public void testNotScheduled() {
        testUtils.setupBook("notebook-1", "* Note A");
        ActivityScenario.launch(MainActivity.class);
        searchForText("s.no");
        onNotesInSearch().check(matches(recyclerViewItemCount(1)));
    }
}

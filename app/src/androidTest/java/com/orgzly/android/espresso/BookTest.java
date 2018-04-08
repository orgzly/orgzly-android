package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.PickerActions.setDate;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class BookTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        /* Create book with enough notes to get a scrollable list on every device. */
        shelfTestUtils.setupBook("book-name",
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
                "** Note #11.\n" +
                "** Note #12.\n" +
                "** Note #13.\n" +
                "** Note #14.\n" +
                ":PROPERTIES:\n" +
                ":CREATED: [2017-01-01]\n" +
                ":END:\n" +
                "** Note #15.\n" +
                "** Note #16.\n" +
                "** Note #17.\n" +
                "** Note #18.\n" +
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
                "** Note #29.\n" +
                "** Note #30.\n" +
                "** Note #31.\n" +
                "** Note #32.\n" +
                "** Note #33.\n" +
                "** Note #34.\n" +
                "** Note #35.\n" +
                "** Note #36.\n" +
                "** Note #37.\n" +
                "** Note #38.\n" +
                "** Note #39.\n" +
                "** Note #40.\n" +
                "");

        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
    }

    @Test
    public void testNoteExists() {
        onListItem(7).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #7."))));
    }

    @Test
    public void testBookHasNewNoteIconDisplayed() {
        onView(withId(R.id.fab)).check(matches(isDisplayed()));
    }

    @Test
    public void testOpensBookDescription() {
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_book_preface_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testOpensNoteFromBook() {
        onListItem(2).perform(click());
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testScheduledNoteTimeStaysTheSameAfterSetting() {
        onListItem(9).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(withText(userDateTime("<2014-05-26 Mon>")), isDisplayed())));

        onListItem(9).perform(longClick());

        openContextualToolbarOverflowMenu();
        onView(withText(R.string.schedule)).perform(click());
        onView(withText(R.string.set)).perform(click());

        onListItem(9).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(withText(userDateTime("<2014-05-26 Mon>")), isDisplayed())));
    }

    @Test
    public void testRemovingScheduledTimeFromMultipleNotes() {
        onListItem(8).onChildView(withId(R.id.item_head_scheduled)).check(matches(not(isDisplayed())));
        onListItem(9).onChildView(withId(R.id.item_head_scheduled)).check(matches(isDisplayed()));

        onListItem(8).perform(longClick());
        onListItem(9).perform(click());

        openContextualToolbarOverflowMenu();
        onView(withText(R.string.schedule)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onListItem(8).onChildView(withId(R.id.item_head_scheduled)).check(matches(not(isDisplayed())));
        onListItem(9).onChildView(withId(R.id.item_head_scheduled)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testRemovingDoneState() {
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("DONE"))));
        onListItem(8).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("DONE"))));

        onListItem(5).perform(longClick());
        onListItem(8).perform(click());

        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("TODO")).perform(click());

        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("TODO"))));
        onListItem(8).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("TODO"))));
    }

    @Test
    public void testUpdatingBookPreface() {
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_book_preface_content)).perform(replaceText("New content"));
        onView(withId(R.id.done)).perform(click());
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_book_preface_content)).check(matches(withText("New content")));
    }

    @Test
    public void testScrollPositionKeptOnRotation() {
        toLandscape(activityRule);
        onListItem(40).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note #40."))); // Scroll
        toPortrait(activityRule);
        onView(withText("Note #40.")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateNewNoteUsingFabWhenBookIsEmpty() {
        // Create new empty notebook
        pressBack();
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("book-created-from-scratch"));
        onView(withText(R.string.create)).perform(click());

        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testFoldUnfoldAllButtonWhenBookIsEmpty() {
        // Create new empty notebook
        pressBack();
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("book-created-from-scratch"));
        onView(withText(R.string.create)).perform(click());

        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).perform(click());

        onView(withId(R.id.books_options_menu_item_cycle_visibility)).check(doesNotExist());
    }

    @Test
    public void testBackFromSettingsShouldReturnToPreviousFragment() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCutThenOpenNoteAtThePosition() {
        onListItem(2).perform(longClick());

        onView(withId(R.id.book_cab_cut)).perform(click());

        /* Open note at the same position as the cut one. */
        onListItem(2).perform(click());
    }

    @Test
    public void testCabForMovingNotesDisplayed() {
        onListItem(1).perform(longClick());
        onView(withId(R.id.book_cab_move)).perform(click());
        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrderOfMovedNote() {
        onListItem(3).perform(longClick());

        onView(withId(R.id.book_cab_move)).perform(click());
        onView(withId(R.id.notes_action_move_down)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #2."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #4."))));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #5."))));
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #6."))));
        onListItem(6).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #3."))));
    }

    @Test
    public void testActionModeMovingStaysOpenAfterRotation() {
        toPortrait(activityRule);

        onView(withId(R.id.notes_action_move_down)).check(doesNotExist());

        onListItem(2).perform(longClick());

        onView(withId(R.id.book_cab_move)).perform(click());

        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));

        toLandscape(activityRule);

        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));
    }

    @Test
    public void testPromoting() {
        onListItem(2).perform(longClick());

        onView(withId(R.id.book_cab_move)).perform(click());
        onView(withId(R.id.notes_action_move_left)).perform(click());
    }

    @Test
    public void testPasteAbove() {
        onListItem(2).perform(longClick());

        onView(withId(R.id.book_cab_cut)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #9."))));

        onListItem(1).perform(longClick());
        onView(withId(R.id.book_cab_paste)).perform(click());
        onView(withText(R.string.heads_action_menu_item_paste_above)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #2."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #3."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #4."))));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #5."))));
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #6."))));
        onListItem(6).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #7."))));
        onListItem(7).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(8).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
        onListItem(9).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #9."))));
    }

    @Test
    public void testPasteUnder() {
        onListItem(2).perform(longClick());

        onView(withId(R.id.book_cab_cut)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #9."))));

        onListItem(2).perform(longClick());
        onView(withId(R.id.book_cab_paste)).perform(click());
        onView(withText(R.string.heads_action_menu_item_paste_under)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #9."))));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #10."))));
        onListItem(35).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #2."))));
        onListItem(36).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #3."))));
    }

    @Test
    public void testFoldNotes() {
        onListItem(2).onChildView(withId(R.id.item_head_fold_button)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #2."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
    }

    @Test
    public void testCreateNewNoteUnderFolded() {
        /* Fold. */
        onListItem(2).onChildView(withId(R.id.item_head_fold_button)).perform(click());

        /* Create new note under folded. */
        onListItem(2).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.new_note)).perform(click());
        onView(withText(R.string.heads_action_menu_item_add_under)).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText("Created"));
        onView(withId(R.id.done)).perform(click());

        /* New note should not be visible. */
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #1."))));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #2."))));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText(endsWith("Note #8."))));
    }

    @Test
    public void testNewNoteHasCreatedAtPropertyAfterSaving() {
        AppPreferences.createdAt(context, true);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText("Title"));
        onView(withId(R.id.done)).perform(click());
        onListItem(41).perform(click());
        onView(allOf(withId(R.id.name), withText(R.string.created_property_name))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.name), withText(""))).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnToNonExistentNoteByPressingBack() {
        onListItem(1).perform(click());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());
        onView(allOf(withText("book-name"), withId(R.id.item_book_title))).perform(longClick());
        onData(hasToString(containsString(context.getString(R.string.delete)))).perform(click());
        onView(withText(R.string.delete)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
        onView(withText(R.string.note_does_not_exist_anymore)).check(matches(isDisplayed()));
        onView(withId(R.id.done)).check(doesNotExist());
        onView(withId(R.id.close)).check(doesNotExist());
        pressBack(); // Leave the note
    }

    @Test
    public void testScrollPositionKeptInBackStack() {
        onListItem(40).check(matches(isDisplayed())); // Scroll to note
        onView(withText("Note #40.")).check(matches(isDisplayed())); // Check it's displayed
        onView(withText("Note #40.")).perform(click());
        pressBack();
        onView(withText("Note #40.")).check(matches(isDisplayed())); // Check it's displayed
    }

    @Test
    public void testSetDeadlineTimeForNewNote() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).perform(closeSoftKeyboardWithDelay(), scrollTo(), click());
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText(userDateTime("<2014-04-01 Tue>"))));
    }

    @Test
    public void testDeleteBookPreface() {
        // Preface exists
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_book_preface_container)).check(matches(isDisplayed()));

        // Enter and delete it
        openContextualActionModeOverflowMenu();
        onView(withText(R.string.delete)).perform(click());

        // First list item is now a note
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note #1.")));
    }
}

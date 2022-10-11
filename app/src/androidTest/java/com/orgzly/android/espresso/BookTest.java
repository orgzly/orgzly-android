package com.orgzly.android.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.PickerActions.setDate;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.util.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.util.EspressoUtils.contextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.util.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.util.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.util.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.util.EspressoUtils.scroll;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import android.content.pm.ActivityInfo;
import android.widget.DatePicker;

import androidx.test.core.app.ActivityScenario;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BookTest extends OrgzlyTest {
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        /* Create book with enough notes to get a scrollable list on every device. */
        testUtils.setupBook("book-name",
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

        // Empty book for tests that need to scroll down to a newly created note
        testUtils.setupBook("Book B", "");

        scenario = ActivityScenario.launch(MainActivity.class);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
    }

    @Test
    public void testNoteExists() {
        onNoteInBook(7, R.id.item_head_title_view).check(matches(withText("Note #7.")));
    }

    @Test
    public void testBookHasNewNoteIconDisplayed() {
        onView(withId(R.id.fab)).check(matches(isDisplayed()));
    }

    @Test
    public void testOpensNoteFromBook() {
        onNoteInBook(2).perform(click());
        onView(withId(R.id.view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testScheduledNoteTimeStaysTheSameAfterSetting() {
        onNoteInBook(9, R.id.item_head_scheduled_text).check(matches(allOf(withText(userDateTime("<2014-05-26 Mon>")), isDisplayed())));

        onNoteInBook(9).perform(longClick());
        onView(withId(R.id.schedule)).perform(click());
        onView(withText(R.string.set)).perform(click());

        onNoteInBook(9, R.id.item_head_scheduled_text).check(matches(allOf(withText(userDateTime("<2014-05-26 Mon>")), isDisplayed())));
    }

    @Test
    public void testRemovingScheduledTimeFromMultipleNotes() {
        onNoteInBook(8, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
        onNoteInBook(9, R.id.item_head_scheduled_text).check(matches(isDisplayed()));

        onNoteInBook(8).perform(longClick());
        onNoteInBook(9).perform(click());

        onView(withId(R.id.schedule)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onNoteInBook(8, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
        onNoteInBook(9, R.id.item_head_scheduled_text).check(matches(not(isDisplayed())));
    }

    @Test
    public void testRemovingDoneState() {
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));
        onNoteInBook(8, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));

        onNoteInBook(5).perform(longClick());
        onNoteInBook(8).perform(click());

        onView(withId(R.id.state)).perform(click());
        onView(withText("TODO")).perform(click());

        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
        onNoteInBook(8, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
    }

    @Test
    public void testClearStateTitleUnchanged() {
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText("Note #1.")));

        onNoteInBook(1).perform(longClick());
        onView(withId(R.id.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText("Note #1.")));
    }

    @Test
    public void testScrollPositionKeptOnRotation() {
        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));

        onNoteInBook(40).check(matches(isDisplayed())); // Scroll to note

        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));

        onView(withText("Note #40.")).check(matches(isDisplayed()));
    }

    @Test
    public void testScrollPositionKeptInBackStack() {
        onNoteInBook(40).check(matches(isDisplayed())); // Scroll to note
        onView(withText("Note #40.")).check(matches(isDisplayed()));
        onView(withText("Note #40.")).perform(click());
        pressBack(); // Leave note
        onView(withText("Note #40.")).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateNewNoteUsingFabWhenBookIsEmpty() {
        // Create new empty notebook
        pressBack();
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("book-created-from-scratch"));
        onView(withText(R.string.create)).perform(click());

        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.scroll_view)).check(matches(isDisplayed()));
    }

    @Test
    public void testFoldUnfoldAllButtonWhenBookIsEmpty() {
        // Create new empty notebook
        pressBack();
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("book-created-from-scratch"));
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
        onNoteInBook(2).perform(longClick());

        onActionItemClick(R.id.cut, R.string.cut);

        /* Open note at the same position as the cut one. */
        onNoteInBook(2).perform(click());
    }

    @Test
    public void testCabForMovingNotesDisplayed() {
        onNoteInBook(1).perform(longClick());
        onActionItemClick(R.id.move, R.string.move);
        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrderOfMovedNote() {
        onNoteInBook(3).perform(longClick());

        onActionItemClick(R.id.move, R.string.move);
        onView(withId(R.id.notes_action_move_down)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #4."))));
        onNoteInBook(4, R.id.item_head_title_view).check(matches(withText(endsWith("Note #5."))));
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(endsWith("Note #6."))));
        onNoteInBook(6, R.id.item_head_title_view).check(matches(withText(endsWith("Note #3."))));
    }

    @Test
    public void testActionModeMovingStaysOpenAfterRotation() {
        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));

        onView(withId(R.id.notes_action_move_down)).check(doesNotExist());

        onNoteInBook(2).perform(longClick());

        onActionItemClick(R.id.move, R.string.move);
        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));

        scenario.onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));

        onView(withId(R.id.notes_action_move_down)).check(matches(isDisplayed()));
    }

    @Test
    public void testPromoting() {
        onNoteInBook(2).perform(longClick());

        onActionItemClick(R.id.move, R.string.move);
        onView(withId(R.id.notes_action_move_left)).perform(click());
    }

    @Test
    public void testPasteAbove() {
        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.cut, R.string.cut);

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #9."))));

        onNoteInBook(1).perform(longClick());
        onActionItemClick(R.id.paste, R.string.paste);
        onView(withText(R.string.heads_action_menu_item_paste_above)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #3."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #4."))));
        onNoteInBook(4, R.id.item_head_title_view).check(matches(withText(endsWith("Note #5."))));
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(endsWith("Note #6."))));
        onNoteInBook(6, R.id.item_head_title_view).check(matches(withText(endsWith("Note #7."))));
        onNoteInBook(7, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(8, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
        onNoteInBook(9, R.id.item_head_title_view).check(matches(withText(endsWith("Note #9."))));
    }

    @Test
    public void testPasteUnder() {
        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.cut, R.string.cut);

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #9."))));

        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.paste, R.string.paste);
        onView(withText(R.string.heads_action_menu_item_paste_under)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #9."))));
        onNoteInBook(4, R.id.item_head_title_view).check(matches(withText(endsWith("Note #10."))));
        onNoteInBook(35, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
        onNoteInBook(36, R.id.item_head_title_view).check(matches(withText(endsWith("Note #3."))));
    }

    @Test
    public void testCopyBelow() {
        onNoteInBook(1).perform(longClick());
        onActionItemClick(R.id.copy, R.string.copy);

        onNoteInBook(1).perform(longClick());
        onActionItemClick(R.id.paste, R.string.paste);
        onView(withText(R.string.heads_action_menu_item_paste_below)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
    }

    @Test
    public void testFoldNotes() {
        onNoteInBook(2, R.id.item_head_fold_button).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
    }

    @Test
    public void testCreateNewNoteUnderFolded() {
        /* Fold. */
        onNoteInBook(2, R.id.item_head_fold_button).perform(click());

        /* Create new note under folded. */
        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.new_note, R.string.new_note);
        onView(withText(R.string.new_under)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("Created"));
        onView(withId(R.id.done)).perform(click()); // Note done

        /* New note should not be visible. */
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(endsWith("Note #1."))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(endsWith("Note #2."))));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText(endsWith("Note #8."))));
    }

    @Test
    public void testNewNoteHasCreatedAtPropertyAfterSaving() {
        AppPreferences.createdAt(context, true);

        // Open an empty book
        pressBack();
        onView(allOf(withText("Book B"), isDisplayed())).perform(click());

        // Create a new note
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("Title"));
        onView(withId(R.id.done)).perform(click()); // Note done

        // Enter note
        onNoteInBook(1).perform(click());
        onView(withId(R.id.scroll_view)).perform(swipeUp()); // For small screens

        // Check properties
        onView(allOf(withId(R.id.name), withText(R.string.created_property_name))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.name), withText(""))).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnToNonExistentNoteByPressingBack() {
        // Enter note
        onNoteInBook(1).perform(click());

        // Open notebooks list
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());

        // Delete notebook
        onView(allOf(withText("book-name"), withId(R.id.item_book_title))).perform(longClick());
        contextualToolbarOverflowMenu().perform(click());
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());

        // Return to note
        pressBack();
        onView(withId(R.id.view_flipper)).check(matches(isDisplayed()));
        onView(withText(R.string.note_does_not_exist_anymore)).check(matches(isDisplayed()));
        onView(withId(R.id.done)).check(doesNotExist());
        onView(withId(R.id.delete)).check(doesNotExist());

        // Rotate
        scenario.onActivity(activity -> {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        });

        pressBack(); // Leave note
    }

    @Test
    public void testSetDeadlineTimeForNewNote() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.deadline_button)).perform(closeSoftKeyboardWithDelay(), scroll(), click());
        onView(withId(R.id.date_picker_button)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(withText(android.R.string.ok)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.deadline_button)).check(matches(withText(userDateTime("<2014-04-01 Tue>"))));
    }

    @Test
    public void testToggleState() {
        onNoteInBook(1).perform(longClick());
        onView(withId(R.id.toggle_state)).perform(click());
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));

        onView(withId(R.id.toggle_state)).perform(click());
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
    }

    @Test
    public void testToggleStateForAllDone() {
        onNoteInBook(5).perform(longClick());
        onNoteInBook(8).perform(click());
        onView(withId(R.id.toggle_state)).perform(click());

        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
        onNoteInBook(8, R.id.item_head_title_view).check(matches(withText(startsWith("TODO"))));
    }

    @Test
    public void testToggleStateForAllTodo() {
        onNoteInBook(1).perform(longClick());
        onNoteInBook(2).perform(click());
        onView(withId(R.id.toggle_state)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));
    }

    @Test
    public void testToggleStateForMixed() {
        onNoteInBook(1).perform(longClick());
        onNoteInBook(5).perform(click());
        onView(withId(R.id.toggle_state)).perform(click());

        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(startsWith("DONE"))));
    }

    @Ignore("Not implemented yet")
    @Test
    public void testPreselectedStateOfSelectedNote() {
        onNoteInBook(3).perform(longClick());

        onView(withId(R.id.state)).perform(click());

        onView(withText("TODO")).check(matches(isChecked()));
    }
}

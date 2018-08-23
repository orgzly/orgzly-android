package com.orgzly.android.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.R;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.repos.ReposActivity;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static android.support.test.espresso.Espresso.onData;
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
import static com.orgzly.android.espresso.EspressoUtils.clickClickableSpan;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.isHighlighted;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetDoneKeywords;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("unchecked")
@RunWith(AndroidJUnit4.class)
public class MiscTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Test
    public void testLftRgt() {
        shelfTestUtils.setupBook("booky", "Preface\n* Note 1\n** Note 2\n* Note 3\n");

        activityRule.launchActivity(null);

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();
        NotePosition n3 = shelf.getNote("Note 3").getPosition();

        assertTrue(n1.getLft() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n3.getLft());
        assertTrue(n3.getLft() < n3.getRgt());
    }

    @Test
    public void testClearDatabaseWithFragmentsInBackStack() {
        shelfTestUtils.setupBook(
                "book-one",
                "First book used for testing\n" +
                "* Note A.\n" +
                "** Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "*** TODO Note E.\n" +
                ""
        );

        shelfTestUtils.setupBook(
                "book-two",
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
                ""
        );
        activityRule.launchActivity(null);

        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(withText("Note B.")).perform(click());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("book-two"), isDisplayed())).perform(click());
        onView(withText("Note #2.")).perform(click());
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_app")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_clear_database")).perform(click());
        onView(withText(R.string.ok)).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_books_no_notebooks)).check(matches(isDisplayed()));
    }

    @Test
    public void testClickOnListViewItemOutOfView() {
        shelfTestUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n* 4\n* 5\n* 6\n* 7\n* 8\n* 9\n* 10\n* 11\n* 12\n* 13\n* 14\n* 15\n");
        activityRule.launchActivity(null);
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onListItem(15).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("15"), isDisplayed())));
    }

    @Test
    public void testChangingNoteStatesToDone() {
        shelfTestUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note #1."), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note #2."), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note #3."), isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note #4."), isDisplayed())));
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("DONE  Note #5."), isDisplayed())));

        onListItem(1).perform(longClick());
        onListItem(2).perform(click());
        onListItem(3).perform(click());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("DONE")).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("DONE  Note #1."), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(1).check(matches(isHighlighted()));

        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("DONE  Note #2."), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(2).check(matches(isHighlighted()));

        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("DONE  Note #3."), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(3).check(matches(isHighlighted()));

        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note #4."), isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(4).check(matches(not(isHighlighted())));

        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("DONE  Note #5."), isDisplayed())));
        onListItem(5).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(5).check(matches(not(isHighlighted())));
    }

    @Test
    public void testSchedulingMultipleNotes() {
        shelfTestUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
        onListItem(5).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));

        onListItem(1).perform(longClick());
        onListItem(2).perform(click());
        onListItem(3).perform(click());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.schedule)).perform(click());
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());
        onView(withText(R.string.set)).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
        onListItem(2).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
        onListItem(3).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText(userDateTime("<2014-04-01 Tue 09:15>")))));
        onListItem(4).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
        onListItem(5).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testTrimmingTitleInNoteFragment() {
        shelfTestUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n");
        activityRule.launchActivity(null);
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(withId(R.id.fab)).perform(click());

        /* Change state to NOTE to avoid having 1 or more spaces before title after keyword in book fragment. */
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText(R.string.clear)).perform(click());

        onView(withId(R.id.fragment_note_title))
                .perform(replaceText("    Title with empty spaces all around   "), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(4).check(matches(isDisplayed())); // Scroll to.
        onView(withText("Title with empty spaces all around")).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Title with empty spaces all around")));
    }

    @Test
    public void testNewBookDialogShouldSurviveScreenRotation() {
        activityRule.launchActivity(null);
        toLandscape(activityRule);
        onView(withId(R.id.fab)).perform(click()); // Failing here
        onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));
        toPortrait(activityRule);
        onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));
        onView(withId(R.id.dialog_input)).perform(replaceText("notebook"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.create)).perform(click());

        /* FIXME: This can fail from time to time on some devices. */
        onView(allOf(withText("notebook"), isDisplayed())).perform(click());

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    /**
     * There was a race condition. Old title is displayed if drawer
     * is closed after book has been loaded.
     */
    @Test
    public void testBookTitleMustBeDisplayedWhenOpeningBookFromDrawer() {
        shelfTestUtils.setupBook("book-one", "Sample book used for tests\n* 1\n* 2\n* 3\n");
        activityRule.launchActivity(null);
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.toolbar)))).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.toolbar)))).check(matches(isDisplayed()));
    }

    @Test
    public void testTimestampDialogTimeButtonValueWhenToggling() {
        shelfTestUtils.setupBook("book-name", "Sample book used for tests\n" +
                                              "* TODO Note #1.\n" +
                                              "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                                              "* Note #2.\n" +
                                              "");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
        onListItem(1).perform(click());

        Calendar cal = new GregorianCalendar(2015, 0, 18, 4, 5);
        String s = DateFormat.getTimeFormat(InstrumentationRegistry.getTargetContext()).format(cal.getTime());

        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).check(matches(withText(containsString(s))));
        onView(withId(R.id.dialog_timestamp_time_check)).perform(scrollTo(), click());
        onView(withId(R.id.dialog_timestamp_time_picker)).check(matches(withText(containsString(s))));
        onView(withId(R.id.dialog_timestamp_time_check)).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).check(matches(withText(containsString(s))));
    }

    @Test
    public void testTimestampComplicated() {
        shelfTestUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "SCHEDULED: <2015-01-18 04:05 .+6d>\n" +
                "* Note #2.\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText(userDateTime("<2015-01-18 Sun 04:05 .+6d>")))));
    }

    @Test
    public void testScheduledWithRepeaterToDoneFromBook() {
        shelfTestUtils.setupBook("book-name", "Sample book used for tests\n" +
                                              "* TODO Note #1.\n" +
                                              "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                                              "* Note #2.\n" +
                                              "");
        activityRule.launchActivity(null);

        settingsSetDoneKeywords("DONE OLD");

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(1).perform(longClick());

        /* TO DO -> DONE */
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("DONE")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("TODO"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

        /* DONE -> NOTE */
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("Note"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

        /* NOTE -> DONE */
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("DONE")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("Note"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText(userDateTime("<2015-01-30 Fri 04:05 +6d>"))));

        /* NOTE -> OLD */
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("OLD")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("Note"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText(userDateTime("<2015-02-05 Thu 04:05 +6d>"))));
    }

    @Test
    public void testScheduledWithRepeaterToDoneFromNoteFragment() {
        shelfTestUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* TODO Note #1.\n" +
                "SCHEDULED: <2015-01-18 04:05 +6d>\n" +
                "* Note #2.\n" +
                "");
        activityRule.launchActivity(null);

        settingsSetDoneKeywords("DONE OLD");

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(1).perform(click());

        /* TO DO -> DONE */
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("DONE")).perform(click());

        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

        /* DONE -> NOTE */
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(userDateTime("<2015-01-24 Sat 04:05 +6d>"))));

        /* NOTE -> DONE */
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("DONE")).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(userDateTime("<2015-01-30 Fri 04:05 +6d>"))));

        /* NOTE -> OLD */
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("OLD")).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(userDateTime("<2015-02-05 Thu 04:05 +6d>"))));
    }

    @Test
    public void testSettingStateToTodo() {
        shelfTestUtils.setupBook("booky", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 1"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note 2"), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note 3"), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note 4"), isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 5"), isDisplayed())));

        onListItem(0).perform(longClick());
        onListItem(1).perform(click());
        onListItem(2).perform(click());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.state)).perform(click());
        onView(withText("TODO")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 1"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 2"), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 3"), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note 4"), isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 5"), isDisplayed())));
    }

    /**
     * Visits every fragment used in the main activity and calls {@link #fragmentTest} on it.
     */
    @Test
    public void testMainActivityFragments() {
        shelfTestUtils.setupBook("book-one", "Preface\n\n* Note");
        shelfTestUtils.setupRepo("file:/");
        shelfTestUtils.setupRepo("dropbox:/orgzly");
        activityRule.launchActivity(null);

        // Books
        fragmentTest(activityRule, true, withId(R.id.fragment_books_container));

        // Book
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        fragmentTest(activityRule, true, withId(R.id.fragment_book_view_flipper));

        // Note
        onView(withText("Note")).perform(click());
        fragmentTest(activityRule, false, withId(R.id.fragment_note_container));
        pressBack();

        // Preface
        onListItem(0).perform(click());
        fragmentTest(activityRule, false, withId(R.id.fragment_book_preface_container));

        // Opened drawer
        onView(withId(R.id.drawer_layout)).perform(open());
        fragmentTest(activityRule, false, withText(R.string.searches));

        // Saved searches
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.searches)).perform(click());
        fragmentTest(activityRule, true, withId(R.id.fragment_filters_flipper));

        // Search
        onListItem(0).perform(click());
        fragmentTest(activityRule, false, withId(R.id.fragment_filter_flipper));

        // Search results
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());
        fragmentTest(activityRule, true, withId(R.id.fragment_query_search_view_flipper));

        // Agenda
        searchForText("t.tag3 ad.3");
        fragmentTest(activityRule, true, withId(R.id.fragment_query_agenda_container));
    }

    @Test
    public void testReposActivityFragments() {
        ActivityTestRule rule = new ActivityTestRule<>(ReposActivity.class, true, false);

        shelfTestUtils.setupBook("book-one", "Preface\n\n* Note");
        shelfTestUtils.setupRepo("file:/");
        shelfTestUtils.setupRepo("dropbox:/orgzly");
        rule.launchActivity(null);

        // List of repos
        fragmentTest(rule, false, withId(R.id.activity_repos_flipper));

        // Directory repo
        onListItem(1).perform(click());
        fragmentTest(rule, false, withId(R.id.activity_repo_directory_container));
        pressBack();

        // Dropbox repo
        onListItem(0).perform(click());
        fragmentTest(rule, false, withId(R.id.activity_repo_dropbox_container));
    }

    private void fragmentTest(ActivityTestRule rule, boolean hasSearchMenuItem, Matcher<View> matcher) {
        onView(matcher).check(matches(isDisplayed()));
        toPortrait(rule);
        onView(matcher).check(matches(isDisplayed()));
        toLandscape(rule);
        onView(matcher).check(matches(isDisplayed()));
        toPortrait(rule);
        onView(matcher).check(matches(isDisplayed()));
        toLandscape(rule);
        onView(matcher).check(matches(isDisplayed()));
        toPortrait(rule);

        if (hasSearchMenuItem) {
            onView(withId(R.id.activity_action_search)).check(matches(isDisplayed()));
        } else {
            onView(withId(R.id.activity_action_search)).check(doesNotExist());
        }
    }

    @Test
    public void testBookTitleFromInBufferSettingsDisplayed() {
        shelfTestUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        activityRule.launchActivity(null);

        /* Books fragment. */
        onListItem(0).onChildView(withId(R.id.item_book_title)).check(matches(withText("Notebook Title")));
        onListItem(0).onChildView(withId(R.id.item_book_subtitle)).check(matches(withText("book-name")));

        /* Books in drawer. */
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("Notebook Title"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).check(matches(isDisplayed()));
    }

    @Test
    public void testBookTitleSettingIsPartOfPreface() {
        shelfTestUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        activityRule.launchActivity(null);

        onView(allOf(withText("Notebook Title"), isDisplayed())).perform(click());
        onListItem(0).onChildView(withId(R.id.fragment_book_header_text))
                .check(matches(withText(containsString("#+TITLE: Notebook Title"))));
    }

    @Test
    public void testBookTitleChangeOnPrefaceEdit() {
        shelfTestUtils.setupBook("book-name", "* TODO Note #1.\n");
        activityRule.launchActivity(null);

        onListItem(0).onChildView(withId(R.id.item_book_title)).check(matches(withText("book-name")));

        /* Set #+TITLE */
        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
        onActionItemClick(R.id.books_options_menu_book_preface, R.string.edit_book_preface);
        onView(withId(R.id.fragment_book_preface_content))
                .perform(replaceText("#+TITLE: Notebook Title"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        pressBack();

        onListItem(0).onChildView(withId(R.id.item_book_title)).check(matches(withText("Notebook Title")));
    }

    @Test
    public void testBookTitleRemoving() {
        shelfTestUtils.setupBook("book-name", "#+TITLE: Notebook Title\n* TODO Note #1.\n");
        activityRule.launchActivity(null);

        onListItem(0).onChildView(withId(R.id.item_book_title)).check(matches(withText("Notebook Title")));
        onListItem(0).onChildView(withId(R.id.item_book_subtitle)).check(matches(withText("book-name")));

        onView(allOf(withText("Notebook Title"), isDisplayed())).perform(click());
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_book_preface_content)).perform(replaceText("#+TTL: Notebook Title"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(0).onChildView(withId(R.id.fragment_book_header_text))
                .check(matches(withText(containsString("#+TTL: Notebook Title"))));
        pressBack();

        onListItem(0).onChildView(withId(R.id.item_book_title)).check(matches(withText("book-name")));
        onListItem(0).onChildView(withId(R.id.item_book_subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testBookReparseOnStateConfigChange() {
        shelfTestUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "SCHEDULED: <2014-05-22 Thu> DEADLINE: <2014-05-22 Thu>\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-01-01 Tue 20:07]\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* ANTIVIVISECTIONISTS Note #8.\n" +
                "**** Note #9.\n" +
                "** Note #10.\n" +
                "");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
        onListItem(8).onChildView(withId(R.id.item_head_title))
                .check(matches(withText(startsWith("ANTIVIVISECTIONISTS "))))
                .perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(withText("ANTIVIVISECTIONISTS Note #8.")));
        settingsSetTodoKeywords("TODO ANTIVIVISECTIONISTS");
        /* Must go to books and back, or the click below will not work for some reason. */
        pressBack(); // Leave book
        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
        onListItem(8).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note #8.")));
    }

    @Test
    public void testCabStaysOpenWhenSelectingTheSameBookFromDrawer() {
        shelfTestUtils.setupBook("booky", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(click());
        onListItem(3).perform(longClick());
        onView(withId(R.id.book_cab_move)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("booky"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.book_cab_move)).check(matches(isDisplayed()));
    }

    @Test
    public void testNewlyCreatedBookShouldNotHaveEncodingsDisplayed() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("booky"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.create)).perform(click());
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used_container)).check(matches(not(isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected_container)).check(matches(not(isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSelectingNoteThenOpeningAnotherBook() {
        shelfTestUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        shelfTestUtils.setupBook("booky-two", "* TODO Note A\n* Note B\n* Note C");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky-one"), withId(R.id.item_book_title))).perform(click());
        onListItem(3).perform(longClick());
        onView(withId(R.id.book_cab_move)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("booky-two"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.book_cab_move)).check(doesNotExist());
    }

    @Test
    public void testOpenBookAlreadyInBackStack() {
        shelfTestUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        shelfTestUtils.setupBook("booky-two", "* TODO Note A\n* Note B\n* Note C");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky-one"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("booky-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCheckboxInTitle() {
        shelfTestUtils.setupBook("book-name", "* - [ ] Checkbox");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());

        onListItem(0).onChildView(withId(R.id.item_head_title)).perform(clickClickableSpan("[ ]"));

        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }
}

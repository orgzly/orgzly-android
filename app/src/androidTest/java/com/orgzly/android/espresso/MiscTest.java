package com.orgzly.android.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.R;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.DrawerFragment;
import com.orgzly.android.ui.fragments.SyncFragment;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

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
import static android.support.test.espresso.contrib.DrawerActions.close;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.PickerActions.setDate;
import static android.support.test.espresso.contrib.PickerActions.setTime;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.isHighlighted;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetDoneKeywords;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;


@SuppressWarnings("unchecked")
public class MiscTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Test
    public void testBooksFragmentExists() {
        activityRule.launchActivity(null);
        Fragment f = ((MainActivity) activityRule.getActivity()).getSupportFragmentManager().findFragmentByTag(BooksFragment.FRAGMENT_TAG);
        assertTrue(f instanceof BooksFragment);
    }

    @Test
    public void testDrawerFragmentExists() {
        activityRule.launchActivity(null);
        Fragment f = ((MainActivity) activityRule.getActivity()).getSupportFragmentManager().findFragmentByTag(DrawerFragment.FRAGMENT_TAG);
        assertTrue(f instanceof DrawerFragment);
    }

    @Test
    public void testSyncFragmentExists() {
        activityRule.launchActivity(null);
        Fragment f = ((MainActivity) activityRule.getActivity()).getSupportFragmentManager().findFragmentByTag(SyncFragment.FRAGMENT_TAG);
        assertTrue(f instanceof SyncFragment);
    }

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
        onData(anything())
                .inAdapterView(withContentDescription(activityRule.getActivity().getString(R.string.fragment_left_drawer_list_view_content_description)))
                .atPosition(5)
                .perform(click());
        onView(withText("Note #2.")).perform(click());
        onActionItemClick(R.id.activity_action_settings, "Settings");
        onListItem(EspressoUtils.SETTINGS_CLEAR_DATABASE).perform(click());
        onView(withText(R.string.ok)).perform(click());

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
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
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
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("Schedule")).perform(click());
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText("OK"), withText("Set"), withText("Done"))).perform(click());
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText("OK"), withText("Set"), withText("Done"))).perform(click());
        onView(withText("Set")).perform(click());

        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText("2014-04-01 Tue 09:15"))));
        onListItem(2).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText("2014-04-01 Tue 09:15"))));
        onListItem(3).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText("2014-04-01 Tue 09:15"))));
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
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onData(allOf(instanceOf(String.class), is("NOTE"))).perform(click());
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
        toLandscape();
        onView(withId(R.id.fab)).perform(click()); // Failing here
        onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));
        toPortrait();
        onView(withId(R.id.dialog_new_book_container)).check(matches(isDisplayed()));
        onView(withId(R.id.dialog_input)).perform(replaceText("notebook"), closeSoftKeyboardWithDelay());
        onView(withText("Create")).perform(click());

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
        onView(allOf(isDescendantOfA(withClassName(containsString("ActionBarContainer"))), withText("Notebooks"))).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(isDescendantOfA(withId(R.id.fragment_left_drawer_container)), withText("book-one"), isDisplayed())).perform(click());
        onView(allOf(isDescendantOfA(withClassName(containsString("ActionBarContainer"))), withText("book-one"))).check(matches(isDisplayed()));
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
        onView(withId(R.id.dialog_timestamp_time)).perform(scrollTo(), click());
        onView(withId(R.id.dialog_timestamp_time_picker)).check(matches(withText(containsString(s))));
        onView(withId(R.id.dialog_timestamp_time)).perform(click());
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

        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(allOf(isDisplayed(), withText("2015-01-18 Sun 04:05 .+6d"))));
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
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("DONE")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("TODO"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* DONE -> NOTE */
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("NOTE")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("Note"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(not(isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* NOTE -> DONE */
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("DONE")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("DONE"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* DONE -> OLD */
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("OLD")).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText(startsWith("OLD"))));
        onListItem(1).onChildView(withId(R.id.item_head_closed)).check(matches(isDisplayed()));
        onListItem(1).onChildView(withId(R.id.item_head_scheduled_text)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));
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
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onData(allOf(instanceOf(String.class), is("DONE"))).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(withText(not(containsString("-")))));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* DONE -> NOTE */
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onData(allOf(instanceOf(String.class), is("NOTE"))).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(withText(not(containsString("-")))));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* NOTE -> DONE */
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onData(allOf(instanceOf(String.class), is("DONE"))).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(withText(containsString("-"))));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        /* DONE -> OLD */
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onData(allOf(instanceOf(String.class), is("OLD"))).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(withText(containsString("-"))));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("2015-01-24 Sat 04:05 +6d")));

        // onListItem().atPosition(1).perform(click(), click()); // failing with one click
        // onView(withId(R.id.fragment_note_content_edit)).check(matches(withText(withPattern("\\- State \"DONE\"       from \"TODO\"       [2015-01-24 Sat 19:05]\n"))));
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
        onView(withId(R.id.book_cab_edit)).perform(click());
        onView(withText("State")).perform(click());
        onView(withText("TODO")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 1"), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 2"), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 3"), isDisplayed())));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("Note 4"), isDisplayed())));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText("TODO  Note 5"), isDisplayed())));
    }

    /**
     * Visits every fragment used in the app and calls {@link #fragmentTest} on it.
     *
     * FIXME: Fails randomly
     */
    @Test
    public void testFragments() {
        shelfTestUtils.setupBook("book-one", "Preface\n\n* Note");
        shelfTestUtils.setupRepo("file:/");
        shelfTestUtils.setupRepo("dropbox:/orgzly");
        activityRule.launchActivity(null);

        fragmentTest(true, withId(R.id.fragment_books_container));
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        fragmentTest(true, withId(R.id.fragment_book_view_flipper));
        onView(withText("Note")).perform(click());
        fragmentTest(false, withId(R.id.fragment_note_container));
        pressBack();
        onListItem(0).perform(click());
        fragmentTest(false, withId(R.id.fragment_book_preface_container));
        onActionItemClick(R.id.activity_action_settings, "Settings");
        fragmentTest(false, withText("Interface"));
        onListItem(EspressoUtils.SETTINGS_REPOS).perform(click());
        fragmentTest(false, withId(R.id.fragment_repos_flipper));
        onListItem(1).perform(click());
        fragmentTest(false, withId(R.id.fragment_repo_directory_container));
        onView(withId(R.id.fragment_repo_directory_browse_button)).perform(click());
        fragmentTest(false, withId(R.id.browser_container));
        pressBack();
        pressBack();
        onView(withId(R.id.fragment_repos_flipper)).check(matches(isDisplayed()));
        onListItem(0).perform(click());
        fragmentTest(false, withId(R.id.fragment_repo_dropbox_container));
        pressBack();
        pressBack();
        pressBack();
        pressBack(); // In Settings after this
        onView(withId(R.id.drawer_layout)).perform(open());
        fragmentTest(false, withText(R.string.searches));
        onView(withId(R.id.drawer_layout)).perform(close());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());
        fragmentTest(true, withId(R.id.fragment_query_view_flipper));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.searches)).perform(click());
        fragmentTest(true, withId(R.id.fragment_filters_flipper));
        onListItem(0).perform(click());
        fragmentTest(false, withId(R.id.fragment_filter_flipper));
    }

    private void fragmentTest(boolean hasSearchMenuItem, Matcher<View> matcher) {
        onView(matcher).check(matches(isDisplayed()));
        toPortrait();
        onView(matcher).check(matches(isDisplayed()));
        toLandscape();
        onView(matcher).check(matches(isDisplayed()));
        toPortrait();
        onView(matcher).check(matches(isDisplayed()));
        toLandscape();
        onView(matcher).check(matches(isDisplayed()));
        toPortrait();

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
        onView(allOf(isDescendantOfA(withId(R.id.fragment_left_drawer_container)), withId(R.id.item_drawer_text), withText("Notebook Title")))
                .check(matches(isDisplayed()));
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
        onActionItemClick(R.id.books_options_menu_book_preface, "Edit preface");
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
        onView(withId(R.id.book_cab_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(isDescendantOfA(withId(R.id.fragment_left_drawer_container)), withText("booky"), isDisplayed())).perform(click());
        onView(withId(R.id.book_cab_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void testNewlyCreatedBookShouldNotHaveEncodingsDisplayed() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("booky"), closeSoftKeyboardWithDelay());
        onView(withText("Create")).perform(click());
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used_container)).check(matches(not(isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected_container)).check(matches(not(isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSelectingNoteThenOpeningAnotherBook() {
        shelfTestUtils.setupBook("booky-one", "* TODO Note 1\n* Note 2\n* Note 3\n* Note 4\n* TODO Note 5");
        shelfTestUtils.setupBook("booky-two", "* TODO Note A\n* Note B\n* Note C");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky-one"), isDisplayed())).perform(click());
        onListItem(3).perform(longClick());
        onView(withId(R.id.book_cab_edit)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_layout)).perform(open());
        onData(anything())
                .inAdapterView(withContentDescription(activityRule.getActivity().getString(R.string.fragment_left_drawer_list_view_content_description)))
                .atPosition(5)
                .perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.book_cab_edit)).check(matches(not(isDisplayed())));
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
        onView(allOf(isDescendantOfA(withId(R.id.fragment_left_drawer_container)), withText("booky-one"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }
}

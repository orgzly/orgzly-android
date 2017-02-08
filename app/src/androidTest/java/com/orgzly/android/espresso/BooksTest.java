package com.orgzly.android.espresso;

import android.os.Environment;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class BooksTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-one",
                "First book used for testing\n" +
                "* Note A.\n" +
                "** Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "*** TODO Note E.\n" +
                ""
        );

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
                ""
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testOpenSettings() {
        onActionItemClick(R.id.activity_action_settings, "Settings");
        onView(withText("Interface")).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnToNonExistentBookByPressingBack() {
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Notebooks")).perform(click());
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onData(hasToString(containsString("Delete"))).perform(click());
        onView(withText(R.string.ok)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onView(withText("This notebook does not exist any more.")).check(matches(isDisplayed()));
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())));
        pressBack();
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("book-two"), isDisplayed())).perform(click());
        onView(allOf(withText("This notebook does not exist any more."), isDisplayed())).check(doesNotExist());
    }

    @Test
    public void testExport() {
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onData(hasToString(containsString("Export"))).perform(click());

        /*
         * Depending on whether external storage is available or not,
         * export should either succeed or fail.
         */
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            onSnackbar().check(matches(withText(startsWith("Notebook exported to"))));
        } else {
            onSnackbar().check(matches(withText(startsWith("Failed exporting book:"))));
        }
    }

    @Test
    public void testCreateNewBookWithoutExtension() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("book-created-from-scratch"), closeSoftKeyboard());
        onView(withText("Create")).perform(click());
        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateNewBookWithExtension() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("book-created-from-scratch.org"));
        onView(withText("Create")).perform(click());
        onView(allOf(withText("book-created-from-scratch.org"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAndDeleteBook() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("book-created-from-scratch"), closeSoftKeyboard());
        onView(withText("Create")).perform(click());

        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).check(matches(isDisplayed()));

        onListItem(0).perform(longClick());
        onView(withText("Delete")).perform(click());
        onView(withText("OK")).perform(click());

        onView(withText("book-created-from-scratch")).check(doesNotExist());
    }

    @Test
    public void testDifferentBookLoading() {
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note A.")));
        pressBack();
        onView(allOf(withText("book-two"), isDisplayed())).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("Note #1.")));
    }

    @Test
    public void testLoadingBookOnlyIfFragmentHasViewCreated() {
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Notebooks")).perform(click());

        onListItem(1).perform(longClick());
        onView(withText("Delete")).perform(click());
        onView(withText("OK")).perform(click());
    }

    @Test
    public void testCreateNewBookWithExistingName() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("new-book"), closeSoftKeyboardWithDelay());
        onView(withText("Create")).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText("new-book"), closeSoftKeyboardWithDelay());
        onView(withText("Create")).perform(click());

        onSnackbar().check(matches(withText("Can't insert notebook with the same name: new-book")));
    }

    @Test
    public void testCreateNewBookWithWhiteSpace() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceText(" new-book  "), closeSoftKeyboardWithDelay());
        onView(withText("Create")).perform(click());

        onListItem(2).onChildView(withId(R.id.item_book_title)).check(matches(withText("new-book")));
    }

    @Test
    public void testRenameBookToExistingName() {
        onListItem(0).perform(longClick());
        onView(withText("Rename")).perform(click());
        onView(withId(R.id.name)).perform(replaceText("book-two"), closeSoftKeyboardWithDelay());
        onView(withText("Rename")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(withText(endsWith("Notebook with that name already exists"))));
    }
}

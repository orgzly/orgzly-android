package com.orgzly.android.espresso;

import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

public class ActionModeTest extends OrgzlyTest {
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
                "");

        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());
    }

    @Test
    public void testQueryFragmentCabShouldBeOpenedOnNoteLongClick() {
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("Scheduled"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());

        onListItem(1).perform(longClick());

        onView(withId(R.id.query_cab_edit)).check(matches(isDisplayed()));
    }

    @Test
    public void testCabStaysOpenOnRotation() {
        toPortrait(activityRule);

        onListItem(3).perform(longClick());

        toLandscape(activityRule);

        onView(withId(R.id.book_cab_move)).check(matches(isDisplayed()));

        // TODO: Check *the expected* note is selected.
    }

    @Test
    public void testCabStaysOpenOnRotationInQueryFragment() {
        toPortrait(activityRule);

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Scheduled")).perform(click());

        onListItem(1).perform(longClick());

        toLandscape(activityRule);

        // TODO: Check *the expected* note is selected.

        toPortrait(activityRule);

        onView(withId(R.id.query_cab_edit)).check(matches(isDisplayed()));
    }

    /* This is for when note click action is reversed - notes can be selected and
     * while selected a note can be opened.
     */
    @Test
    public void testSelectingNoteThenOpeningNoteAndGoingBack() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_look_and_feel")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_is_reverse_click_action")).perform(click());
        pressBack();
        pressBack();
        onListItem(3).perform(click()); // Select note
        onListItem(3).perform(longClick()); // Open note
        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.book_cab_move)).check(doesNotExist());
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.book_cab_move)).check(doesNotExist());
    }

    @Test
    public void testBackPressClosesDrawer() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.drawer_navigation_view)).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.drawer_navigation_view)).check(matches(not(isDisplayed())));
    }
}

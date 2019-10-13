package com.orgzly.android.espresso;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onSavedSearch;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static org.hamcrest.Matchers.allOf;

public class SavedSearchesFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("book-one", "Preface\n* Note A.\n");

        activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.searches)).perform(click());
    }

    @Test
    public void testNewSameNameSavedSearch() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_saved_search_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.fragment_saved_search_name)).perform(replaceTextCloseKeyboard("Scheduled"));
        onView(withId(R.id.fragment_saved_search_query)).perform(replaceTextCloseKeyboard("s.done"));
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.filter_name_already_exists)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_saved_search_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.fragment_saved_search_name)).perform(replaceTextCloseKeyboard("SCHEDULED"));
        onView(withId(R.id.fragment_saved_search_query)).perform(replaceTextCloseKeyboard("s.done"));
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.filter_name_already_exists)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_saved_search_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testUpdateSameNameSavedSearch() {
        onView(withId(R.id.fragment_saved_searches_flipper)).check(matches(isDisplayed()));
        onSavedSearch(0).perform(click());
        onView(withId(R.id.fragment_saved_search_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_saved_search_query)).perform(typeText(" edited"));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.fragment_saved_searches_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testDeletingSavedSearchThenGoingBackToIt() {
        onView(withId(R.id.fragment_saved_searches_flipper)).check(matches(isDisplayed()));

        onSavedSearch(0).perform(click());
        onView(withId(R.id.fragment_saved_search_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.searches), isDescendantOfA(withId(R.id.drawer_navigation_view))))
                .perform(click());
        onView(withId(R.id.fragment_saved_searches_flipper)).check(matches(isDisplayed()));

        onSavedSearch(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());

        pressBack();

        onView(withText(R.string.search_does_not_exist_anymore)).check(matches(isDisplayed()));
    }


    @Test
    public void testActionModeWhenSelectingSavedSearchThenOpeningBook() {
        onSavedSearch(0).perform(longClick());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.saved_searches_cab_move_up)).check(doesNotExist());
    }

    @Test
    public void testMovingSavedSearchDown() {
        onSavedSearch(0).perform(longClick());
        onView(withId(R.id.saved_searches_cab_move_down)).perform(click());
    }

    @Test
    public void testExportSavedSearches() {
        onActionItemClick(R.id.saved_searches_export, R.string.export);
        onSnackbar().check(matches(withText(
                context.getResources().getQuantityString(R.plurals.exported_searches, 4, 4))));
    }
}

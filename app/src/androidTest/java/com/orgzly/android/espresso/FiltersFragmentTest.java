package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static org.hamcrest.Matchers.allOf;

public class FiltersFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-one", "Preface\n* Note A.\n");

        activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.searches)).perform(click());
    }

    @Test
    public void testNewSameNameFilter() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_filter_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.fragment_filter_name)).perform(replaceText("Scheduled"));
        onView(withId(R.id.fragment_filter_query)).perform(replaceText("s.done"));
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.filter_name_already_exists)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_filter_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.fragment_filter_name)).perform(replaceText("SCHEDULED"));
        onView(withId(R.id.fragment_filter_query)).perform(replaceText("s.done"));
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.filter_name_already_exists)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_filter_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testUpdateSameNameFilter() {
        onView(withId(R.id.fragment_filters_flipper)).check(matches(isDisplayed()));
        onListItem(0).perform(click());
        onView(withId(R.id.fragment_filter_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_filter_query)).perform(typeText(" edited"));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.fragment_filters_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testDeletingFilterThenGoingBackToIt() {
        onView(withId(R.id.fragment_filters_flipper)).check(matches(isDisplayed()));

        onListItem(0).perform(click());
        onView(withId(R.id.fragment_filter_flipper)).check(matches(isDisplayed()));

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.searches), isDescendantOfA(withId(R.id.drawer_navigation_view))))
                .perform(click());
        onView(withId(R.id.fragment_filters_flipper)).check(matches(isDisplayed()));

        onListItem(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());

        pressBack();

        onView(withText(R.string.search_does_not_exist_anymore)).check(matches(isDisplayed()));
    }


    @Test
    public void testActionModeWhenSelectingFilterThenOpeningBook() {
        onListItem(0).perform(longClick());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText("book-one"), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.filters_cab_move_up)).check(doesNotExist());
    }

    @Test
    public void testMovingFilterDown() {
        onListItem(0).perform(longClick());
        onView(withId(R.id.filters_cab_move_down)).perform(click());
    }

    @Test
    public void testExportFilters() {
        onActionItemClick(R.id.filters_export, R.string.export);
        onSnackbar().check(matches(withText(context.getResources().getQuantityString(R.plurals.exported_searches, 4, 4))));
    }
}

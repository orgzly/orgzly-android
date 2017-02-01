package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;

public class FiltersFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        activityRule.launchActivity(null);

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText("Searches")).perform(click());
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
}

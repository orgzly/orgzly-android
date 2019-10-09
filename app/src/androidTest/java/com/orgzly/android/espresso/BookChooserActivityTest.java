package com.orgzly.android.espresso;

import android.content.Intent;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.BookChooserActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static android.app.Activity.RESULT_OK;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class BookChooserActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(BookChooserActivity.class);

    private void startActivityWithIntent(String action) {
        Intent intent = new Intent();

        if (action != null) {
            intent.setAction(action);
        }

        activityRule.launchActivity(intent);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("book-one", "");
        testUtils.setupBook("book-two", "");
        testUtils.setupBook("book-three", "");
    }

    @Test
    public void testDisplayBooks() {
        startActivityWithIntent(Intent.ACTION_CREATE_SHORTCUT);
        onView(allOf(withText("book-one"), isDisplayed())).check(matches(isDisplayed()));
    }

    @Ignore("SecurityException")
    @Test
    public void testLongClickChoosesBook() {
        startActivityWithIntent(Intent.ACTION_CREATE_SHORTCUT);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        // java.lang.SecurityException: Injecting to another application requires INJECT_EVENTS permission

        assertTrue(activityRule.getActivity().isFinishing());
    }

    @Test
    public void testCreateShortcut() throws Exception {
        startActivityWithIntent(Intent.ACTION_CREATE_SHORTCUT);
        onView(allOf(withText("book-one"), isDisplayed())).perform(click());

        assertThat(getActivityResultCode(activityRule.getActivity()), is(RESULT_OK));
        Intent resultIntent = getActivityResultData(activityRule.getActivity());
        assertThat(resultIntent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME), is("book-one"));
    }
}

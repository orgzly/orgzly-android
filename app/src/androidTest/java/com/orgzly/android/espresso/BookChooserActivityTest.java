package com.orgzly.android.espresso;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.BookChooserActivity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.app.Activity.RESULT_OK;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class BookChooserActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(BookChooserActivity.class, true, false);

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

        shelfTestUtils.setupBook("book-one", "");
        shelfTestUtils.setupBook("book-two", "");
        shelfTestUtils.setupBook("book-three", "");
    }

    @Test
    public void testDisplayBooks() {
        startActivityWithIntent(Intent.ACTION_CREATE_SHORTCUT);
        onView(allOf(withText("book-one"), isDisplayed())).check(matches(isDisplayed()));
    }

    @Test
    public void testNoContextMenu() {
        startActivityWithIntent(Intent.ACTION_CREATE_SHORTCUT);
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
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

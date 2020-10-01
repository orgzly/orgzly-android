package com.orgzly.android.espresso;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;

import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.BookChooserActivity;

import org.junit.Before;
import org.junit.Ignore;
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
    private ActivityScenario<BookChooserActivity> startActivityWithCreateShortcutAction() {
        Intent intent = new Intent(context, BookChooserActivity.class);
        intent.setAction(Intent.ACTION_CREATE_SHORTCUT);
        return ActivityScenario.launch(intent);
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
        startActivityWithCreateShortcutAction();

        onView(allOf(withText("book-one"), isDisplayed())).check(matches(isDisplayed()));
    }

    @Ignore("SecurityException")
    @Test
    public void testLongClickChoosesBook() {
        ActivityScenario<BookChooserActivity> scenario = startActivityWithCreateShortcutAction();

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        // java.lang.SecurityException: Injecting to another application requires INJECT_EVENTS permission

        scenario.onActivity(activity -> assertTrue(activity.isFinishing()));
    }

    @Test
    public void testCreateShortcut() {
        ActivityScenario<BookChooserActivity> scenario = startActivityWithCreateShortcutAction();

        onView(allOf(withText("book-one"), isDisplayed())).perform(click());

        Instrumentation.ActivityResult result = scenario.getResult();

        assertThat(result.getResultCode(), is(RESULT_OK));
        assertThat(result.getResultData().getStringExtra(Intent.EXTRA_SHORTCUT_NAME), is("book-one"));
    }
}

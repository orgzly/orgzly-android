package com.orgzly.android.espresso;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.ShareActivity;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ShareActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(ShareActivity.class, true, false);

    private void startActivityWithIntent(String action, String type, String extraText, String filterQuery) {
        Intent intent = new Intent();

        if (action != null) {
            intent.setAction(action);
        }

        if (type != null) {
            intent.setType(type);
        }

        if (extraText != null) {
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
        }

        if (filterQuery != null) {
            intent.putExtra(AppIntent.EXTRA_FILTER, filterQuery);
        }

        activityRule.launchActivity(intent);
    }

    @Test
    public void testDefaultBookRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);
        toPortrait(activityRule);
        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText(context.getString(R.string.default_share_notebook))));
        toLandscape(activityRule);
        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText(context.getString(R.string.default_share_notebook))));
    }

    @Test
    public void testBookRemainsSetAfterRotation() {
        shelfTestUtils.setupBook("book-one", "");
        shelfTestUtils.setupBook("book-two", "");
        shelfTestUtils.setupBook("book-three", "");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);
        toPortrait(activityRule);
        onView(withId(R.id.fragment_note_location_button)).perform(click());
        onView(withText("book-two")).perform(click());
        onView(withId(R.id.fragment_note_location_button)).check(matches(withText("book-two")));
        toLandscape(activityRule);
        onView(withId(R.id.fragment_note_location_button)).check(matches(withText("book-two")));
    }

    @Test
    public void testDefaultBookName() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);

        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText(context.getString(R.string.default_share_notebook))));
    }

    @Test
    public void testTextSimple() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testSaveAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);
        toLandscape(activityRule);
        toPortrait(activityRule);
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testTextEmpty() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "", null);
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testTextNull() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", null, null);
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testNoMatchingType() {
        startActivityWithIntent(Intent.ACTION_SEND, "image/png", null, null);

        onView(withId(R.id.fragment_note_title)).check(matches(withText("")));
        onSnackbar().check(matches(withText(context.getString(R.string.share_type_not_supported, "image/png"))));
    }

    @Test
    public void testNoActionSend() {
        startActivityWithIntent(null, null, null, null);

        onView(withId(R.id.fragment_note_title)).check(matches(withText("")));
    }

    @Test
    public void testSettingScheduledTimeRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", null);
        toPortrait(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))));
        toLandscape(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))));
    }

    @Test
    public void testNoteInsertedLast() {
        shelfTestUtils.setupBook("book-one", "* Note 1\n** Note 2");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "Note 3", null);

        onView(withId(R.id.done)).perform(click());

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
    public void testPresetBookFromFilterQuery() {
        shelfTestUtils.setupBook("foo", "doesn't matter");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", "b.foo");

        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText("foo")));
    }
}

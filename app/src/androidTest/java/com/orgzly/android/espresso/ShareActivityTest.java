package com.orgzly.android.espresso;

import android.content.Intent;
import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.db.entity.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.share.ShareActivity;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

//@Ignore
@SuppressWarnings("unchecked")
public class ShareActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(ShareActivity.class, true, false);

    private void startActivityWithIntent(String action, String type, String extraText, String searchQuery) {
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

        if (searchQuery != null) {
            intent.putExtra(AppIntent.EXTRA_QUERY_STRING, searchQuery);
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
        onView(withId(R.id.fragment_note_location_button)).perform(scrollTo());
        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText(context.getString(R.string.default_share_notebook))));
    }

    @Test
    public void testBookRemainsSetAfterRotation() {
        testUtils.setupBook("book-one", "");
        testUtils.setupBook("book-two", "");
        testUtils.setupBook("book-three", "");
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
        startActivityWithIntent(Intent.ACTION_SEND, "application/octet-stream", null, null);

        onView(withId(R.id.fragment_note_title)).check(matches(withText("")));
        onSnackbar().check(matches(withText(context.getString(R.string.share_type_not_supported, "application/octet-stream"))));
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
        testUtils.setupBook("book-one", "* Note 1\n** Note 2");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "Note 3", null);

        onView(withId(R.id.done)).perform(click());

        NotePosition n1 = dataRepository.getLastNote("Note 1").getPosition();
        NotePosition n2 = dataRepository.getLastNote("Note 2").getPosition();
        NotePosition n3 = dataRepository.getLastNote("Note 3").getPosition();

        assertTrue(n1.getLft() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n3.getLft());
        assertTrue(n3.getLft() < n3.getRgt());
    }

    @Test
    public void testPresetBookFromSearchQuery() {
        testUtils.setupBook("foo", "doesn't matter");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text", "b.foo");

        onView(allOf(withId(R.id.fragment_note_location_button), isDisplayed()))
                .check(matches(withText("foo")));
    }
}

package com.orgzly.android.espresso;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.ShareActivity;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onSpinnerString;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static com.orgzly.android.espresso.EspressoUtils.withPattern;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ShareActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(ShareActivity.class, true, false);

    private void startActivityWithIntent(String action, String type, String extraText) {
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

        activityRule.launchActivity(intent);
    }

    @Test
    public void testDefaultBookRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toPortrait();
        onData(anything())
                .inAdapterView(allOf(withId(R.id.activity_share_books_spinner), isDisplayed()))
                .atPosition(0)
                .check(matches(withText("Share")));
        toLandscape();
        onData(anything())
                .inAdapterView(allOf(withId(R.id.activity_share_books_spinner), isDisplayed()))
                .atPosition(0)
                .check(matches(withText("Share")));
    }

    @Test
    public void testBookRemainsSetAfterRotation() {
        shelfTestUtils.setupBook("book-one", "");
        shelfTestUtils.setupBook("book-two", "");
        shelfTestUtils.setupBook("book-three", "");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toPortrait();
        onView(withId(R.id.activity_share_books_spinner)).perform(click()); // Open spinner
        onView(withText("book-two")).perform(click());
        onView(withText("book-two")).check(matches(isDisplayed()));
        toLandscape();
        onView(withText("book-two")).check(matches(isDisplayed()));
    }

    @Test
    public void testDefaultBookName() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");

        onData(anything())
                .inAdapterView(allOf(withId(R.id.activity_share_books_spinner), isDisplayed()))
                .atPosition(0)
                .check(matches(withText("Share")));
    }

    @Test
    public void testTextSimple() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testSaveAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toLandscape();
        toPortrait();
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testTextEmpty() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "");
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testTextNull() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", null);
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testNoMatchingType() {
        startActivityWithIntent(Intent.ACTION_SEND, "image/png", null);

        onView(withId(R.id.fragment_note_title))
                .check(matches(withText("Share type image/png is not supported")));
    }

    @Test
    public void testNoActionSend() {
        startActivityWithIntent(null, null, null);

        onView(withId(R.id.fragment_note_title)).check(matches(withText("Share action not set")));
    }


    // TODO: Failing from time to time due to:
    // android.view.WindowLeaked: Activity com.orgzly.android.ui.ShareActivity has leaked window android.widget.PopupWindow$PopupDecorView
    @Test
    public void testSettingStateRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toPortrait();
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onSpinnerString("TODO").perform(click());
        onView(withId(R.id.fragment_note_state)).perform(scrollTo());
        onView(withText("TODO")).check(matches(isDisplayed()));
        toLandscape();
        onView(withId(R.id.fragment_note_state)).perform(scrollTo());
        onView(withText("TODO")).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingPriorityRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toPortrait();
        onView(withId(R.id.fragment_note_priority)).perform(click()); // Open spinner
        onSpinnerString("B").perform(click());
        onView(withId(R.id.fragment_note_priority)).perform(scrollTo());
        onView(withText("B")).check(matches(isDisplayed()));
        toLandscape();
        onView(withId(R.id.fragment_note_priority)).perform(scrollTo());
        onView(withText("B")).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingScheduledTimeRemainsSetAfterRotation() {
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "This is some shared text");
        toPortrait();
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(not(containsString("-")))));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(anyOf(withText("OK"), withText("Set"), withText("Done"))).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(withPattern("^\\d{4}-\\d{2}-\\d{2} .*")), isDisplayed())));
        toLandscape();
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(withPattern("^\\d{4}-\\d{2}-\\d{2} .*")), isDisplayed())));
    }

    @Test
    public void testNoteInsertedLast() {
        shelfTestUtils.setupBook("book-one", "* Note 1\n** Note 2");
        startActivityWithIntent(Intent.ACTION_SEND, "text/plain", "Note 3");

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
}

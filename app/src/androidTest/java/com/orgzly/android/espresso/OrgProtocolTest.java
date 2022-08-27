package com.orgzly.android.espresso;

import android.content.Intent;
import android.net.Uri;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.ShareActivity;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
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
public class OrgProtocolTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-a",
                "* Note [b-1]\n" +
                        ":PROPERTIES:\n" +
                        ":CUSTOM_ID: DIFFERENT case CUSTOM id\n" +
                        ":END:\n" +

                        "* Note [b-2]\n" +
                        ":PROPERTIES:\n" +
                        ":END:\n" +

                        "* Note [b-3]\n" +
                        ":PROPERTIES:\n" +
                        ":CUSTOM_ID: Link to note in a different book\n" +
                        ":END:\n" +
                        ""
        );
        shelfTestUtils.setupBook("book-b",
                "* Note [b-4]\n" +
                        ":PROPERTIES:\n" +
                        ":ID: BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F\n" +
                        ":END:\n" +

                        ""
        );

    }
    private void startActivityWithIntent(String action, Uri uri) {
        Intent intent = new Intent();

        if (action != null) {
            intent.setAction(action);
        }
        intent.setData(uri);

        activityRule.launchActivity(intent);
    }

    @Test
    public void testOrgProtocolOpensNote() {
        Uri uri = Uri.parse("org-protocol://org-id-goto?id=BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F");
        startActivityWithIntent(Intent.ACTION_VIEW,  uri);
        toPortrait(activityRule);


        ViewInteraction textView = onView(
                allOf(withId(R.id.fragment_note_location),
                        isDisplayed()));
        textView.check(matches(withText("book-b")));

        ViewInteraction editText = onView(
                allOf(withId(R.id.fragment_note_title),
                        isDisplayed()));
        editText.check(matches(withText("Note [b-4]")));
    }

    @Test
    public void testOrgProtocolBadLink1() {
        Uri uri = Uri.parse("org-protocol://org-id-goto://BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F");
        startActivityWithIntent(Intent.ACTION_VIEW,  uri);
        toPortrait(activityRule);

        ViewInteraction textView = onView(
                allOf(withId(R.id.snackbar_text),
                        isDisplayed()));

        textView.check(matches(withText("Note with “ID” property set to “org-protocol://org-id-goto://BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F” not found")));

    }
    @Test
    public void testOrgProtocolBadLink2() {
        Uri uri = Uri.parse("org-protocol://some-other-protocol?x=1&y=2");
        startActivityWithIntent(Intent.ACTION_VIEW,  uri);
        toPortrait(activityRule);

        ViewInteraction textView = onView(
                allOf(withId(R.id.snackbar_text),
                        isDisplayed()));

        textView.check(matches(withText("Note with “url” property set to “org-protocol://some-other-protocol?x=1&y=2” not found")));

    }

}

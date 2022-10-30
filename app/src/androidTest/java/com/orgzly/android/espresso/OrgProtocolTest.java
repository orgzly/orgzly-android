package com.orgzly.android.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Test;

public class OrgProtocolTest extends OrgzlyTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("book-a",
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
        testUtils.setupBook("book-b",
                "* Note [b-4]\n" +
                        ":PROPERTIES:\n" +
                        ":ID: BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F\n" +
                        ":END:\n" +
                        ""
        );
    }

    @Test
    public void testOrgProtocolOpensNote() {
        launchActivity("org-protocol://org-id-goto?id=BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F");

        ViewInteraction textView = onView(allOf(withId(R.id.location_button), isDisplayed()));
        textView.check(matches(withText("book-b")));

        ViewInteraction editText = onView(allOf(withId(R.id.title), isDisplayed()));
        editText.check(matches(withText("Note [b-4]")));
    }

    @Test
    public void testOrgProtocolBadLink1() {
        launchActivity("org-protocol://org-id-goto://BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F");

        ViewInteraction textView = onView(allOf(withId(R.id.snackbar_text), isDisplayed()));

        textView.check(matches(withText("Note with “ID” property set to “org-protocol://org-id-goto://BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F” not found")));

    }
    @Test
    public void testOrgProtocolBadLink2() {
        launchActivity("org-protocol://some-other-protocol?x=1&y=2");

        ViewInteraction textView = onView(allOf(withId(R.id.snackbar_text), isDisplayed()));

        textView.check(matches(withText("Note with “url” property set to “org-protocol://some-other-protocol?x=1&y=2” not found")));
    }

    private void launchActivity(String uriString) {
        Uri uri = Uri.parse(uriString);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        ActivityScenario.launch(intent);

        // SystemClock.sleep(5000);
    }
}

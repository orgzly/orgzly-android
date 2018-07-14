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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickClickableSpan;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;

public class InternalLinksTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-a",
                "* Note [a-1]\n" +
                "[[id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F]]\n" +

                "* Note [a-2]\n" +
                "[[#Different case custom id]]\n" +

                "* Note [a-3]\n" +
                "[[#Link to note in a different book]]\n" +
                ""
        );

        shelfTestUtils.setupBook("book-b",
                "* Note [b-1]\n" +
                ":PROPERTIES:\n" +
                ":CUSTOM_ID: DIFFERENT case CUSTOM id\n" +
                ":END:\n" +

                "* Note [b-2]\n" +
                ":PROPERTIES:\n" +
                ":ID: BDCE923B-C3CD-41ED-B58E-8BDF8BABA54F\n" +
                ":END:\n" +

                "* Note [b-3]\n" +
                ":PROPERTIES:\n" +
                ":CUSTOM_ID: Link to note in a different book\n" +
                ":END:\n" +
                ""
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testDifferentCaseUuidInternalLink() {
        onListItem(0).perform(click());
        onListItem(0).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("id:bdce923b-C3CD-41ED-B58E-8BDF8BABA54F"));
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-2]")));
    }

    @Test
    public void testDifferentCaseCustomIdInternalLink() {
        onListItem(0).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("#Different case custom id"));
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-1]")));
    }

    @Test
    public void testInternalLink() {
        onListItem(0).perform(click());
        onListItem(2).onChildView(withId(R.id.item_head_content))
                .perform(clickClickableSpan("#Link to note in a different book"));
        onView(withId(R.id.fragment_note_title)).check(matches(withText("Note [b-3]")));
    }
}

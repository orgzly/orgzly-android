package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

/**
 *
 */
@SuppressWarnings("unchecked")
public class CreatedAtTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook(
                "book-a",
                "* [#B] Note [a-1]\n" +
                ":PROPERTIES:\n" +
                ":CREATED: [2018-01-05]\n" +
                ":ADDED: [2018-01-03]\n" +
                ":END:\n" +
                "SCHEDULED: <2018-01-01>\n" +
                "Content for [a-1]\n" +
                "* Note [a-2]\n" +
                ":PROPERTIES:\n" +
                ":CREATED: [2018-01-02]\n" +
                ":ADDED: [2018-01-04]\n" +
                ":END:\n" +
                "SCHEDULED: <2014-01-01>\n"
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testChangeCreatedAtPropertyResultsShouldBeReordered() {
        searchForText("o.m");

        // Enable created-at property sync
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_CREATED_AT);
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));

        // Change created-at property name
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_CREATED_AT_PROPERTY);
        onView(instanceOf(EditText.class)).perform(replaceText("ADDED"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
    }
}

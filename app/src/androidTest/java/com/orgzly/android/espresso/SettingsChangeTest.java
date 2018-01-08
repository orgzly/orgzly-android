package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;

/**
 *
 */
@SuppressWarnings("unchecked")
public class SettingsChangeTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-a",
                "* [#B] Note [a-1]\n" +
                ":PROPERTIES:\n" +
                ":CREATED: [2018-01-01]\n" +
                ":ADDED: [2017-01-01]\n" +
                ":END:\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "Content for [a-1]\n" +

                "* Note [a-2]\n" +
                ":PROPERTIES:\n" +
                ":CREATED: [2017-01-01]\n" +
                ":ADDED: [2018-01-01]\n" +
                ":END:\n" +
                "SCHEDULED: <2014-01-01>\n"
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testChangeDefaultPrioritySearchResultsShouldBeReordered() {
        searchForText("o.p");
        testChangeDefaultPriorityResultsShouldBeReordered(0);
    }

    @Test
    public void testChangeDefaultPriorityAgendaResultsShouldBeReordered() {
        searchForText("o.p ad.2");
        testChangeDefaultPriorityResultsShouldBeReordered(1);
    }

    private void testChangeDefaultPriorityResultsShouldBeReordered(int index) {
        onListItem(index).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
        onListItem(index + 1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_DEFAULT_PRIORITY);
        onData(hasToString(containsString("A"))).perform(click());
        pressBack();
        pressBack();

        onListItem(index).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onListItem(index + 1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
    }

    @Test
    public void testChangeCreatedAtPropertyResultsShouldBeReordered() {
        searchForText("o.m");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_CREATED_AT);
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_CREATED_AT_PROPERTY);
        onView(instanceOf(EditText.class)).perform(replaceText("ADDED"));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();

        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
    }

    @Test
    public void testDisplayedContentInBook() {
        onListItem(0).perform(click());
        onListItem(0).onChildView(withId(R.id.item_head_content)).check(matches(allOf(withText(containsString("Content for [a-1]")), isDisplayed())));

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_DISPLAY_CONTENT);
        pressBack();
        pressBack();

        onListItem(0).onChildView(withId(R.id.item_head_content)).check(matches(not(isDisplayed())));
    }
}

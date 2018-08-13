package com.orgzly.android.espresso;

import android.support.test.espresso.matcher.PreferenceMatchers;
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
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onList;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;


@SuppressWarnings("unchecked")
public class CreatedAtTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        String createdProperty = context.getString(R.string.created_property_name);

        shelfTestUtils.setupBook(
                "book-a",
                "* Note [a-1]\n" +
                ":PROPERTIES:\n" +
                ":" + createdProperty + ": [2018-01-05]\n" +
                ":ADDED: [2018-01-01]\n" +
                ":END:\n" +
                "SCHEDULED: <2018-01-01>\n" +
                "* Note [a-2]\n" +
                ":PROPERTIES:\n" +
                ":" + createdProperty + ": [2018-01-02]\n" +
                ":ADDED: [2018-01-04]\n" +
                ":END:\n" +
                "SCHEDULED: <2014-01-01>\n"
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testCondition() {
        enableCreatedAt();

        searchForText("cr.le.today");
        onList().check(matches(listViewItemCount(2)));

        // TODO: Search before/after 2018-01-03 expecting 1 note
    }

    @Test
    public void testSortOrder() {
        enableCreatedAt();

        searchForText("o.cr");
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note [a-2]"), isDisplayed())));

        searchForText(".o.cr");
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note [a-1]"), isDisplayed())));
    }

    @Test
    public void testChangeCreatedAtPropertyResultsShouldBeReordered() {
        searchForText("o.cr");

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-1]")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));

        enableCreatedAt();

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-1]")), isDisplayed())));

        changeCreatedAtProperty("ADDED");

        onListItem(0).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-1]")), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
    }

    @Test
    public void testNewNote() {
        onView(allOf(withText("book-a"), isDisplayed())).perform(click());

        enableCreatedAt();

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceText("new note created by test"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());

        onListItem(2).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));

        searchForText("o.cr");
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("Note [a-2]"), isDisplayed())));

        searchForText(".o.cr");
        onListItem(0).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));
    }

    private void enableCreatedAt() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_is_created_at_added")).perform(click());
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();
    }

    private void changeCreatedAtProperty(String propName) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_created_at_property")).perform(click());
        onView(instanceOf(EditText.class)).perform(replaceText(propName), closeSoftKeyboardWithDelay());
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();
    }
}

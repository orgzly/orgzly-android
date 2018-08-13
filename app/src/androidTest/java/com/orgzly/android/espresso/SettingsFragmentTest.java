package com.orgzly.android.espresso;

import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
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
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;


@SuppressWarnings("unchecked")
public class SettingsFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        activityRule.launchActivity(null);
    }

    @Test
    public void testImportingGettingStartedFromGettingStartedNotebook() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_app")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_reload_getting_started")).perform(click());
        pressBack();
        pressBack();
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("Getting Started with Orgzly"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_app")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_reload_getting_started")).perform(click());
        pressBack();
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddingNewTodoKeywordInSettingsAndChangingStateToItForNewNote() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_states")).perform(click());

        onView(withId(R.id.todo_states)).perform(replaceText("TODO XXX YYY ZZZ"));

        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());

        onData(PreferenceMatchers.withKey("pref_key_new_note_state")).perform(click());

        onData(hasToString(containsString("ZZZ"))).perform(click());
    }

    @Test
    public void testAddingNewTodoKeywordInSettingsNewNoteShouldHaveDefaultState() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_states")).perform(click());

        onView(withId(R.id.todo_states)).perform(replaceText("TODO ZZZ"), closeSoftKeyboardWithDelay());
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());

        onData(PreferenceMatchers.withKey("pref_key_new_note_state")).perform(click());

        onData(hasToString(containsString("NOTE"))).perform(click());
    }

    @Test
    public void testStateSummaryAfterNoStates() {
        AppPreferences.states(context, "|");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_states")).perform(click());
        onView(withId(R.id.todo_states)).perform(replaceText("TODO"));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());
        onView(allOf(withText(R.string.states), hasSibling(withText("TODO |")))).check(matches(isDisplayed()));
    }

    @Test
    public void testNewNoteDefaultStateIsInitiallyVisibleInSummary() {
        AppPreferences.states(context, "XXX YYY ZZZ | DONE");
        AppPreferences.newNoteState(context, "YYY");

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_new_note_state")).perform(click());

        onView(withText("YYY")).check(matches(isDisplayed()));
    }

    @Test
    public void testNewNoteDefaultStateIsSetInitially() {
        AppPreferences.states(context, "XXX YYY ZZZ | DONE");
        AppPreferences.newNoteState(context, "YYY");

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_new_note_state")).perform(click());

        onData(hasToString(containsString("YYY"))).perform(click());
    }

    @Test
    public void testDefaultPriorityUpdateOnLowestPriorityChange() {
        AppPreferences.defaultPriority(context, "C");
        AppPreferences.minPriority(context, "E");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_min_priority")).perform(click());
        onData(hasToString(containsString("B"))).perform(click());

        onData(PreferenceMatchers.withKey("pref_key_default_priority"))
                .onChildView(withText("B"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLowestPriorityUpdateOnDefaultPriorityChange() {
        AppPreferences.defaultPriority(context, "C");
        AppPreferences.minPriority(context, "E");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_default_priority")).perform(click());
        onData(hasToString(containsString("X"))).perform(click());

        onData(PreferenceMatchers.withKey("pref_key_min_priority"))
                .onChildView(withText("X"))
                .check(matches(isDisplayed()));
    }
}

package com.orgzly.android.espresso;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;

public class SettingsFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        activityRule.launchActivity(null);
    }

    @Test
    public void testImportingGettingStartedFromGettingStartedNotebook() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_app", R.string.app);
        clickSetting("pref_key_reload_getting_started", R.string.reload_getting_started);
        pressBack();
        pressBack();
        onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withText(R.string.getting_started_notebook_name), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_app", R.string.app);
        clickSetting("pref_key_reload_getting_started", R.string.reload_getting_started);
        pressBack();
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onNoteInBook(1).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddingNewTodoKeywordInSettingsAndChangingStateToItForNewNote() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);

        onView(withId(R.id.todo_states)).perform(replaceTextCloseKeyboard("TODO AAA BBB CCC"));

        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());

        clickSetting("pref_key_new_note_state", R.string.state);

        onData(hasToString(containsString("CCC"))).perform(click());
    }

    @Test
    public void testAddingNewTodoKeywordInSettingsNewNoteShouldHaveDefaultState() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);

        onView(withId(R.id.todo_states)).perform(replaceTextCloseKeyboard("TODO CCC"));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());

        clickSetting("pref_key_new_note_state", R.string.state);

        onData(hasToString(containsString("NOTE"))).perform(click());
    }

    @Test
    public void testStateSummaryAfterNoStates() {
        AppPreferences.states(context, "|");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);
        onView(withId(R.id.todo_states)).perform(replaceTextCloseKeyboard("TODO"));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());
        onView(allOf(withText(R.string.states), hasSibling(withText("TODO |")))).check(matches(isDisplayed()));
    }

    @Test
    public void testStatesDuplicateDetectedIgnoringCase() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);

        onView(withId(R.id.todo_states)).perform(replaceTextCloseKeyboard("TODO NEXT next"));

        onView(withText(context.getString(R.string.duplicate_keywords_not_allowed, "NEXT")))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testNewNoteDefaultStateIsInitiallyVisibleInSummary() {
        AppPreferences.states(context, "AAA BBB CCC | DONE");
        AppPreferences.newNoteState(context, "BBB");

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_new_note_state", R.string.state);

        onView(withText("BBB")).check(matches(isDisplayed()));
    }

    @Test
    public void testNewNoteDefaultStateIsSetInitially() {
        AppPreferences.states(context, "AAA BBB CCC | DONE");
        AppPreferences.newNoteState(context, "BBB");

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_new_note_state", R.string.state);

        onData(hasToString(containsString("BBB"))).perform(click());
    }

    @Test
    public void testDefaultPriorityUpdateOnLowestPriorityChange() {
        AppPreferences.defaultPriority(context, "C");
        AppPreferences.minPriority(context, "E");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_min_priority", R.string.lowest_priority);
        onData(hasToString(containsString("B"))).perform(click());

        clickSetting("pref_key_default_priority", R.string.default_priority);
        onData(hasToString("B")).check(matches(isChecked()));
    }

    @Test
    public void testLowestPriorityUpdateOnDefaultPriorityChange() {
        AppPreferences.defaultPriority(context, "C");
        AppPreferences.minPriority(context, "E");
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_default_priority", R.string.default_priority);
        onData(hasToString(containsString("X"))).perform(click());

        clickSetting("pref_key_min_priority", R.string.lowest_priority);
        onData(hasToString("X")).check(matches(isChecked()));
    }

    @Test
    public void testLowercaseStateConvertedToUppercase() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);

        onView(withId(R.id.todo_states)).perform(replaceTextCloseKeyboard("TODO NEXT wait"));

        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.not_now)).perform(click());

        onView(allOf(withText(R.string.states), hasSibling(withText("TODO NEXT WAIT | DONE"))))
                .check(matches(isDisplayed()));
    }
}

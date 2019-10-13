package com.orgzly.android.espresso;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onBook;
import static com.orgzly.android.espresso.EspressoUtils.onItemInAgenda;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInSearch;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;

public class SettingsChangeTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook(
                "book-a",
                "* [#B] Note [a-1]\n" +
                "SCHEDULED: <2018-01-01>\n" +
                "Content for [a-1]\n" +
                "* Note [a-2]\n" +
                "SCHEDULED: <2014-01-01>\n"
        );

        activityRule.launchActivity(null);
    }

    @Test
    public void testChangeDefaultPrioritySearchResultsShouldBeReordered() {
        searchForText("o.p");

        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));

        setDefaultPriority("A");

        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onNoteInSearch(1, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
    }

    @Test
    public void testChangeDefaultPriorityAgendaResultsShouldBeReordered() {
        searchForText("o.p ad.2");

        onItemInAgenda(1, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
        onItemInAgenda(2, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));

        setDefaultPriority("A");

        onItemInAgenda(1, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("Note [a-2]")), isDisplayed())));
        onItemInAgenda(2, R.id.item_head_title)
                .check(matches(allOf(withText(containsString("#B  Note [a-1]")), isDisplayed())));
    }

    @Test
    public void testDisplayedContentInBook() {
        onBook(0).perform(click());

        onNoteInBook(1, R.id.item_head_content)
                .check(matches(allOf(withText(containsString("Content for [a-1]")), isDisplayed())));

        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_is_notes_content_displayed_in_list", R.string.display_content);
        pressBack();
        pressBack();

        onNoteInBook(1, R.id.item_head_content).check(matches(not(isDisplayed())));
    }

    private void setDefaultPriority(String priority) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_default_priority", R.string.default_priority);
        onData(hasToString(containsString(priority))).perform(click());
        pressBack();
        pressBack();
    }
}

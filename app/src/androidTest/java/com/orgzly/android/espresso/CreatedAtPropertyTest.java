package com.orgzly.android.espresso;

import android.widget.EditText;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInSearch;
import static com.orgzly.android.espresso.EspressoUtils.onNotesInSearch;
import static com.orgzly.android.espresso.EspressoUtils.recyclerViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.EspressoUtils.searchForText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;

public class CreatedAtPropertyTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        String createdProperty = context.getString(R.string.created_property_name);

        testUtils.setupBook(
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
        onNotesInSearch().check(matches(recyclerViewItemCount(2)));

        // TODO: Search before/after 2018-01-03 expecting 1 note
    }

    @Test
    public void testSortOrder() {
        enableCreatedAt();

        searchForText("o.cr");
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note [a-2]"), isDisplayed())));

        searchForText(".o.cr");
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note [a-1]"), isDisplayed())));
    }

    @Test
    public void testChangeCreatedAtPropertyResultsShouldBeReordered() {
        searchForText("o.cr");

        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note [a-1]")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note [a-2]")));

        enableCreatedAt();

        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note [a-2]")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note [a-1]")));

        changeCreatedAtProperty("ADDED");

        onNoteInSearch(0, R.id.item_head_title).check(matches(withText("Note [a-1]")));
        onNoteInSearch(1, R.id.item_head_title).check(matches(withText("Note [a-2]")));
    }

    @Test
    public void testNewNote() {
        onView(allOf(withText("book-a"), isDisplayed())).perform(click());

        enableCreatedAt();

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceTextCloseKeyboard("new note created by test"));
        onView(withId(R.id.done)).perform(click());

        onNoteInBook(3, R.id.item_head_title)
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));

        searchForText("o.cr");
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("Note [a-2]"), isDisplayed())));

        searchForText(".o.cr");
        onNoteInSearch(0, R.id.item_head_title)
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));
    }

    private void enableCreatedAt() {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_sync", R.string.sync);
        clickSetting("pref_key_is_created_at_added", R.string.use_created_at_property);
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();
    }

    private void changeCreatedAtProperty(String propName) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_sync", R.string.sync);
        clickSetting("pref_key_created_at_property", R.string.created_at_property);
        onView(instanceOf(EditText.class)).perform(replaceTextCloseKeyboard(propName));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());
        pressBack();
        pressBack();
    }
}

package com.orgzly.android.espresso;

import androidx.test.rule.ActivityTestRule;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.PickerActions.setDate;
import static androidx.test.espresso.contrib.PickerActions.setTime;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onBook;
import static com.orgzly.android.espresso.EspressoUtils.onListView;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.EspressoUtils.setNumber;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;


//@Ignore
@SuppressWarnings("unchecked")
public class NoteFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("book-name",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "SCHEDULED: <2014-05-22 Thu> DEADLINE: <2014-05-22 Thu>\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "SCHEDULED: <2015-01-11 Sun .+1d/2d>\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-01-01 Wed 20:07]\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* ANTIVIVISECTIONISTS Note #8.\n" +
                "**** Note #9.\n" +
                "** Note #10.\n" +
                "");

        activityRule.launchActivity(null);

        onBook(0).perform(click());
    }

    @Test
    public void testDeleteNote() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_view_flipper)).check(matches(isDisplayed()));

        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));

        onSnackbar().check(matches(withText(
                context.getResources().getQuantityString(R.plurals.notes_deleted, 1, 1))));
    }

    @Test
    public void testUpdateNoteTitle() {
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note #1.")));

        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceTextCloseKeyboard("Note title changed"));
        onView(withId(R.id.done)).perform(click());

        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note title changed")));
    }

    @Test
    public void testSettingScheduleTime() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button))
                .check(matches(withText(startsWith(defaultDialogUserDate()))));
    }

    @Test
    public void testAbortingOfSettingScheduledTime() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
    }

    @Test
    public void testRemovingScheduledTime() {
        onNoteInBook(2).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(""))));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
    }

    @Test
    public void testSettingDeadlineTime() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
    }

    @Test
    public void testAbortingOfSettingDeadlineTime() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));
    }

    @Test
    public void testRemovingDeadlineTime() {
        onNoteInBook(2).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(not(withText(""))));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));
    }

    @Test
    public void testStateToDoneShouldAddClosedTime() {
        onNoteInBook(2).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("DONE")).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(allOf(withText(startsWith(currentUserDate())), isDisplayed())));
    }

    @Test
    public void testStateToDoneForNoteShouldShiftTime() {
        onNoteInBook(4).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_state_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")), isDisplayed())));
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));

        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("DONE")).perform(click());

        onView(withId(R.id.fragment_note_state_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")))));
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testChangingStateSettingsFromNoteFragment() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        settingsSetTodoKeywords("");
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onListView().check(matches(listViewItemCount(1))); // Only DONE
        pressBack();
        settingsSetTodoKeywords("TODO");
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onListView().check(matches(listViewItemCount(2)));
    }

    @Test
    public void testTitleCanNotBeEmptyForNewNote() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.done)).perform(click());
        onSnackbar().check(matches(withText(R.string.title_can_not_be_empty)));
    }

    @Test
    public void testTitleCanNotBeEmptyForExistingNote() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceTextCloseKeyboard(""));
        onView(withId(R.id.done)).perform(click());
        onSnackbar().check(matches(withText(R.string.title_can_not_be_empty)));
    }

    @Test
    public void testSavingNoteWithRepeater() {
        onNoteInBook(4).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testClosedTimeInNoteFragmentIsSameAsInList() {
        onNoteInBook(5).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())));
    }

    @Test
    public void testSettingStateRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText("TODO")).perform(click());
        onView(withText("TODO")).check(matches(isDisplayed()));
        toLandscape(activityRule);
        onView(withText("TODO")).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingPriorityRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_priority_button)).perform(click());
        onView(withText("B")).perform(click());
        onView(withId(R.id.fragment_note_priority_button)).check(matches(withText("B")));
        toLandscape(activityRule);
        onView(withId(R.id.fragment_note_priority_button)).check(matches(withText("B")));
    }

    @Test
    public void testSettingScheduledTimeRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))));
        toLandscape(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))));
    }

    @Test
    public void testSetScheduledTimeAfterRotation() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        toPortrait(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        toLandscape(activityRule);
        onView(withText(R.string.set)).perform(closeSoftKeyboardWithDelay(), click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(startsWith(defaultDialogUserDate()))));
    }

    @Test
    public void testRemovingDoneStateRemovesClosedTime() {
        onNoteInBook(5).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())));
        onView(withId(R.id.fragment_note_state_button)).perform(click());
        onView(withText(R.string.clear)).perform(click());
        onView(withId(R.id.fragment_note_closed_edit_text)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingPmTimeDisplays24HourTime() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());

        /* Set date. */
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());

        /* Set time. */
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(15, 15));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());

        onView(withText(R.string.set)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button))
                .check(matches(withText(userDateTime("<2014-04-01 Tue 15:15>"))));
    }

    @Test
    public void testDateTimePickerKeepsValuesAfterRotation() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText("")));

        toPortrait(activityRule);

        onView(withId(R.id.fragment_note_deadline_button)).perform(click());

        /* Set date. */
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());

        /* Set time. */
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(anyOf(withText(R.string.ok), withText(R.string.done))).perform(click());

        /* Set repeater. */
        onView(withId(R.id.dialog_timestamp_repeater_check)).perform(scrollTo(), click());
        onView(withId(R.id.dialog_timestamp_repeater_picker)).perform(scrollTo(), click());
        onView(withId(R.id.dialog_timestamp_repeater_value)).perform(setNumber(3));
        onView(withText(R.string.ok)).perform(click());

        /* Rotate screen. */
        toLandscape(activityRule);

        /* Set time. */
        onView(withText(R.string.set)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button))
                .check(matches(withText(userDateTime("<2014-04-01 Tue 09:15 .+3w>"))));
    }

    @Test
    public void testChangingPrioritySettingsFromNoteFragment() {
        /* Open note which has no priority set. */
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        /* Change lowest priority to A. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_min_priority", R.string.lowest_priority);
        onData(hasToString(containsString("A"))).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.fragment_note_priority_button)).perform(click());
        onListView().check(matches(listViewItemCount(1)));
        pressBack();

        /* Change lowest priority to C. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_min_priority", R.string.lowest_priority);
        onData(hasToString(containsString("C"))).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.fragment_note_priority_button)).perform(click());
        onListView().check(matches(listViewItemCount(3)));
    }

    @Test
    public void testPropertiesAfterRotatingDevice() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.name))
                .perform(replaceText("prop-name-1"));
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-1"))))
                .perform(replaceTextCloseKeyboard("prop-value-1"));

        onView(allOf(withId(R.id.name), not(withText("prop-name-1"))))
                .perform(replaceText("prop-name-2"));
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-2"))))
                .perform(replaceTextCloseKeyboard("prop-value-2"));

        toLandscape(activityRule);
        toPortrait(activityRule);

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.name), withText("prop-name-2"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-2"))).check(matches(isDisplayed()));
    }

    @Test
    public void testSavingProperties() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(withId(R.id.name))
                .perform(replaceText("prop-name-1"));
        onView(allOf(withId(R.id.value), hasSibling(withText("prop-name-1"))))
                .perform(replaceTextCloseKeyboard("prop-value-1"));

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()));

        onView(withId(R.id.done)).perform(click());

        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()));
    }

    @Test
    public void testContentLineCountUpdatedOnNoteUpdate() {
        onNoteInBook(1).perform(click());
        onView(withId(R.id.bottom_action_bar_open)).perform(click());
        onView(withId(R.id.body_edit)).perform(replaceTextCloseKeyboard("a\nb\nc"));
        onView(withId(R.id.done)).perform(click());
        onNoteInBook(1, R.id.item_head_fold_button).perform(click());
        onNoteInBook(1, R.id.item_head_title).check(matches(withText(endsWith("3"))));
    }
}

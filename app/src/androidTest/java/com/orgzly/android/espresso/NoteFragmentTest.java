package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;
import android.widget.DatePicker;
import android.widget.TimePicker;

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
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.PickerActions.setDate;
import static android.support.test.espresso.contrib.PickerActions.setTime;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.onSpinnerString;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static com.orgzly.android.espresso.EspressoUtils.spinnerItemCount;
import static com.orgzly.android.espresso.EspressoUtils.toLandscape;
import static com.orgzly.android.espresso.EspressoUtils.toPortrait;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;


@SuppressWarnings("unchecked")
public class NoteFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        shelfTestUtils.setupBook("book-name",
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

        onView(allOf(withText("book-name"), isDisplayed())).perform(click());
    }

    @Test
    public void testSettingScheduleTime() {
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
    }

    @Test
    public void testAbortingOfSettingScheduledTime() {
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
    }

    @Test
    public void testRemovingScheduledTime() {
        onListItem(2).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(R.string.schedule_button_hint))));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText("Clear")).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
    }

    @Test
    public void testSettingDeadlineTime() {
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText(R.string.deadline_button_hint)));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
    }

    @Test
    public void testAbortingOfSettingDeadlineTime() {
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText(R.string.deadline_button_hint)));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText(R.string.deadline_button_hint)));
    }

    @Test
    public void testRemovingDeadlineTime() {
        onListItem(2).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(not(withText(R.string.deadline_button_hint))));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());
        onView(withText("Clear")).perform(click());
        onView(withId(R.id.fragment_note_deadline_button)).check(matches(withText(R.string.deadline_button_hint)));
    }

    @Test
    public void testStateToDoneShouldAddClosedTime() {
        onListItem(2).perform(click());

        onView(withId(R.id.fragment_note_closed_button)).check(matches(not(isDisplayed())));
        onView(withId(R.id.fragment_note_state)).perform(click());
        onView(withText("DONE")).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(allOf(withText(startsWith(currentUserDate())), isDisplayed())));
    }

    @Test
    public void testStateToDoneForNoteShouldShiftTime() {
        onListItem(4).perform(click());

        onView(withText("NOTE")).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")), isDisplayed())));
        onView(withId(R.id.fragment_note_closed_button)).check(matches(not(isDisplayed())));

        onView(withId(R.id.fragment_note_state)).perform(click());
        onView(withText("DONE")).perform(click());

        onView(withText("NOTE")).check(matches(isDisplayed()));
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(not(withText(userDateTime("<2015-01-11 Sun .+1d/2d>")))));
        onView(withId(R.id.fragment_note_closed_button)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testChangingStateSettingsFromNoteFragment() {
        onListItem(1).perform(click());
        settingsSetTodoKeywords("");
        onView(withId(R.id.fragment_note_state)).check(matches(spinnerItemCount(2))); // NOTE and others
        settingsSetTodoKeywords("TODO");
        onView(withId(R.id.fragment_note_state)).check(matches(spinnerItemCount(3))); // NOTE and others
    }

    @Test
    public void testTitleCanNotBeEmptyForNewNote() {
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.can_not_be_empty)).check(matches(isDisplayed()));
    }

    @Test
    public void testTitleCanNotBeEmptyForExistingNote() {
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText(""));
        onView(withId(R.id.done)).perform(click());
        onView(withText(R.string.can_not_be_empty)).check(matches(isDisplayed()));
    }

    @Test
    public void testSavingNoteWithRepeater() {
        onListItem(4).perform(click());
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testClosedTimeInNoteFragmentIsSameAsInList() {
        onListItem(5).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())));
    }

    @Test
    public void testSettingStateRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onSpinnerString("TODO").perform(click());
        onView(withText("TODO")).check(matches(isDisplayed()));
        toLandscape(activityRule);
        onView(withText("TODO")).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingPriorityRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_priority)).perform(click()); // Open spinner
        onSpinnerString("B").perform(click());
        onView(withText("B")).check(matches(isDisplayed()));
        toLandscape(activityRule);
        onView(withText("B")).check(matches(isDisplayed()));
    }

    @Test
    public void testSettingScheduledTimeRemainsSetAfterRotation() {
        toPortrait(activityRule);
        onListItem(1).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        onView(withText(R.string.set)).perform(click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
        toLandscape(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
    }

    @Test
    public void testSetScheduledTimeAfterRotation() {
        onListItem(1).perform(click());
        toPortrait(activityRule);
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(withText(R.string.schedule_button_hint)));
        onView(withId(R.id.fragment_note_scheduled_button)).perform(click());
        toLandscape(activityRule);
        onView(withText(R.string.set)).perform(closeSoftKeyboardWithDelay(), click());
        onView(withId(R.id.fragment_note_scheduled_button)).check(matches(allOf(withText(startsWith(defaultDialogUserDate())), isDisplayed())));
    }

    @Test
    public void testRemovingDoneStateRemovesClosedTime() {
        onListItem(5).perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(allOf(withText(userDateTime("[2014-01-01 Wed 20:07]")), isDisplayed())));
        onView(withId(R.id.fragment_note_state)).perform(click()); // Open spinner
        onSpinnerString("NOTE").perform(click());
        onView(withId(R.id.fragment_note_closed_button)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingPmTimeDisplays24HourTime() {
        onListItem(1).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(R.string.deadline_button_hint), isDisplayed())));
        onView(withId(R.id.fragment_note_deadline_button)).perform(click());

        /* Set date. */
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(withText(R.string.ok)).perform(click());

        /* Set time. */
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(15, 15));
        onView(withText(R.string.ok)).perform(click());

        onView(withText(R.string.set)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(userDateTime("<2014-04-01 Tue 15:15>")), isDisplayed())));
    }

    @Test
    public void testDateTimePickerKeepsValuesAfterRotation() {
        onListItem(1).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(R.string.deadline_button_hint), isDisplayed())));

        toPortrait(activityRule);

        onView(withText(R.string.deadline_button_hint)).perform(click());

        /* Set date. */
        onView(withId(R.id.dialog_timestamp_date_picker)).perform(click());
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(setDate(2014, 4, 1));
        onView(withText(R.string.ok)).perform(click());

        /* Set time. */
        onView(withId(R.id.dialog_timestamp_time_picker)).perform(scrollTo(), click());
        onView(withClassName(equalTo(TimePicker.class.getName()))).perform(setTime(9, 15));
        onView(withText(R.string.ok)).perform(click());

        /* Rotate screen. */
        toLandscape(activityRule);

        /* Set time. */
        onView(withText(R.string.set)).perform(click());

        onView(withId(R.id.fragment_note_deadline_button)).check(matches(allOf(withText(userDateTime("<2014-04-01 Tue 09:15>")), isDisplayed())));
    }

    @Test
    public void testChangingPrioritySettingsFromNoteFragment() {
        /* Open note which has no priority set. */
        onListItem(1).perform(click());

        /* Change lowest priority to A. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_LOWEST_PRIORITY);
        onData(hasToString(containsString("A"))).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.fragment_note_priority)).check(matches(spinnerItemCount(2)));

        /* Change lowest priority to C. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        EspressoUtils.tapToSetting(EspressoUtils.SETTINGS_LOWEST_PRIORITY);
        onData(hasToString(containsString("C"))).perform(click());
        pressBack();
        pressBack();

        onView(withId(R.id.fragment_note_priority)).check(matches(spinnerItemCount(4)));
    }

    @Test
    public void testPropertiesAfterRotatingDevice() {
        onListItem(1).perform(click());

        onView(withId(R.id.add_property)).perform(click());
        onView(withId(R.id.name)).perform(replaceText("prop-name-1"));
        onView(withId(R.id.value)).perform(replaceText("prop-value-1"), closeSoftKeyboardWithDelay());

        onView(withId(R.id.add_property)).perform(click());
        onView(allOf(withId(R.id.name), not(withText("prop-name-1")))).perform(replaceText("prop-name-2"));
        onView(allOf(withId(R.id.value), not(withText("prop-value-1")))).perform(replaceText("prop-value-2"), closeSoftKeyboardWithDelay());

        toLandscape(activityRule);
        toPortrait(activityRule);

        onView(allOf(withId(R.id.name), withText("prop-name-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-1"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.name), withText("prop-name-2"))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.value), withText("prop-value-2"))).check(matches(isDisplayed()));
    }
}

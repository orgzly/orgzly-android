package com.orgzly.android.espresso;

import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

/**
 *
 */
@SuppressWarnings("unchecked")
public class NewNoteTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Test
    public void testNewNoteInEmptyNotebook() {
        shelfTestUtils.setupBook("notebook", "");
        activityRule.launchActivity(null);

        onView(allOf(withText("notebook"), isDisplayed())).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceText("new note created by test"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());

        onListItem(1)
                .onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));
    }

    @Test
    public void testNewNote() {
        shelfTestUtils.setupBook("booky", "Booky Preface\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        activityRule.launchActivity(null);
        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        /* Enable "Created at" in settings. */
        onActionItemClick(R.id.activity_action_settings, "Settings");
        onListItem(EspressoUtils.SETTINGS_CREATED_AT).perform(click());
        pressBack();

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceText("new note created by test"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(7).onChildView(withId(R.id.item_head_title))
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));
    }

    @Test
    public void testNewNoteUnder() {
        shelfTestUtils.setupBook("notebook", "description\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        activityRule.launchActivity(null);
        onView(allOf(withText("notebook"), isDisplayed())).perform(click());

        onListItem(2).perform(longClick());
        onView(withId(R.id.book_cab_new)).perform(click());
        onView(withText("New under")).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText("A"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText("2")));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText("3")));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(withText("4")));
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText("A")));
        shelfTestUtils.assertBook("notebook", "description\n\n* 1\n** 2\n*** 3\n*** 4\n*** A\n** 5\n* 6\n");
    }

    @Test
    public void testNewNoteAbove() {
        shelfTestUtils.setupBook("notebook", "description\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        activityRule.launchActivity(null);
        onView(allOf(withText("notebook"), isDisplayed())).perform(click());

        onListItem(2).perform(longClick());
        onView(withId(R.id.book_cab_new)).perform(click());
        onView(withText("New above")).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText("A"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(1).onChildView(withId(R.id.item_head_title)).check(matches(withText("1")));
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText("A")));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText("2")));
        shelfTestUtils.assertBook("notebook", "description\n\n* 1\n** A\n** 2\n*** 3\n*** 4\n** 5\n* 6\n");
    }

    @Test
    public void testNewNoteBelow() {
        shelfTestUtils.setupBook("booky", "Booky Preface\n" +
                                          "* 1\n" +
                                          "** 2\n" +
                                          "*** 3\n" +
                                          "*** 4\n" +
                                          "** 5\n" +
                                          "* 6");

        activityRule.launchActivity(null);
        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        onListItem(2).perform(longClick());
        onView(withId(R.id.book_cab_new)).perform(click());
        onView(withText("New below")).perform(click());
        onView(withId(R.id.fragment_note_title)).perform(replaceText("A"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());
        onListItem(2).onChildView(withId(R.id.item_head_title)).check(matches(withText("2")));
        onListItem(3).onChildView(withId(R.id.item_head_title)).check(matches(withText("3")));
        onListItem(4).onChildView(withId(R.id.item_head_title)).check(matches(withText("4")));
        onListItem(5).onChildView(withId(R.id.item_head_title)).check(matches(withText("A")));
        onListItem(6).onChildView(withId(R.id.item_head_title)).check(matches(withText("5")));

        shelfTestUtils.assertBook("booky", "Booky Preface\n" +
                                           "\n" +
                                           "* 1\n" +
                                           "** 2\n" +
                                           "*** 3\n" +
                                           "*** 4\n" +
                                           "** A\n" +
                                           "** 5\n" +
                                           "* 6\n");
    }

    @Test
    public void testNewNoteFromQuickMenuWhenCabIsDisplayed() {
        shelfTestUtils.setupBook("booky", "Booky Preface\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        activityRule.launchActivity(null);
        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        onView(withId(R.id.action_context_bar)).check(matches(not(isDisplayed())));
        onListItem(2).perform(longClick());
        /* Swipe left. */
        onListItem(2).perform(new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER, GeneralLocation.CENTER_LEFT, Press.FINGER));
        onView(withId(R.id.action_context_bar)).check(matches(isDisplayed()));
        onListItem(2).onChildView(withId(R.id.item_menu_new_under_btn)).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(isDisplayed()));
        onView(withId(R.id.done)).check(matches(isDisplayed()));
        onView(withId(R.id.action_context_bar)).check(matches(not(isDisplayed())));
    }
}

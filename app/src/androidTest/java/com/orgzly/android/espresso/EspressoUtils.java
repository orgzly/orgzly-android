package com.orgzly.android.espresso;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CloseKeyboardAction;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;
import com.orgzly.R;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.*;

/*
 * Few espresso-related notes:
 *
 * - closeSoftKeyboardWithDelay() is often used, as on some devices keyboard will cover the view
 *   which is supposed to be clicked next, causing java.lang.SecurityException to get thrown.
 *
 * - replaceText() is preferred over typeText() as it is much faster.
 */
class EspressoUtils {
    static final int SETTINGS_REVERSED_NOTE_CLICK_ACTION = 1;

    static final int SETTINGS_STATE_KEYWORDS = 21;
    static final int SETTINGS_DEFAULT_PRIORITY = 22;
    static final int SETTINGS_LOWEST_PRIORITY = 23;

    static final int SETTINGS_NEW_NOTE_STATE = 25;
    static final int SETTINGS_CREATED_AT = 27;

    static final int SETTINGS_REPOS = 33;

    static final int SETTINGS_SYNC_AFTER = 34;

    static final int IMPORT_GETTING_STARTED = 43;
    static final int SETTINGS_CLEAR_DATABASE = 44;

    /**
     */
    @SuppressWarnings("unchecked")
    public static DataInteraction onListItem(int pos) {
        return onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list), isDisplayed()))
                .atPosition(pos);
    }

    static ViewInteraction onSnackbar() {
        return onView(withId(android.support.design.R.id.snackbar_text));
    }

    /*
     * Regular expression matching.
     * https://github.com/hamcrest/JavaHamcrest/issues/65
     */
//    static TypeSafeMatcher<String> withPattern(final String pattern) {
//        checkNotNull(pattern);
//
//        return new TypeSafeMatcher<String>() {
//            @Override
//            public boolean matchesSafely(String s) {
//                return Pattern.compile(pattern).matcher(s).matches();
//            }
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("a string matching the pattern '" + pattern + "'");
//            }
//        };
//    }

    /**
     * Item could either be on the action bar (visible) or in the overflow menu.
     */
    static void onActionItemClick(int id, int resourceId) {
        try {
            onView(withId(id)).perform(click());

        } catch (Exception e) {
            e.printStackTrace();

            // Open the overflow menu OR open the options menu,
            // depending on if the device has a hardware or software overflow menu button.
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
            onView(withText(resourceId)).perform(click());
        }
    }

    static void toLandscape(ActivityTestRule activityRule) {
        toOrientation(activityRule, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    static void toPortrait(ActivityTestRule activityRule) {
        toOrientation(activityRule, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private static void toOrientation(ActivityTestRule activityRule, int requestedOrientation) {
        activityRule.getActivity().setRequestedOrientation(requestedOrientation);

        /* Not pretty, but it does seem to fix testFragments from randomly failing. */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static DataInteraction onSpinnerString(String value) {
        return onData(allOf(instanceOf(String.class), is(value))).inRoot(not(isDialog()));
    }

    static void settingsSetTodoKeywords(String keywords) {
        settingsSetKeywords(R.id.todo_states, keywords);
    }

    static void settingsSetDoneKeywords(String keywords) {
        settingsSetKeywords(R.id.done_states, keywords);
    }

    private static void settingsSetKeywords(int viewId, String keywords) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        onListItem(SETTINGS_STATE_KEYWORDS).perform(click());

        onView(withId(viewId)).perform(replaceText(keywords), closeSoftKeyboardWithDelay());
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());

        pressBack();
    }

    static void searchForText(String str) {
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        onView(withHint(R.string.search_hint)).perform(replaceText(str), pressKey(66));

        /* TODO: Ugh. */
        try { Thread.sleep(300); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    /**
     * Give keyboard time to close, to avoid java.lang.SecurityException
     * if hidden button is clicked next.
     */
    static ViewAction closeSoftKeyboardWithDelay() {
        return new ViewAction() {
            /**
             * The delay time to allow the soft keyboard to dismiss.
             */
            private static final long KEYBOARD_DISMISSAL_DELAY_MILLIS = 1000L;

            /**
             * The real {@link CloseKeyboardAction} instance.
             */
            private final ViewAction mCloseSoftKeyboard = new CloseKeyboardAction();

            @Override
            public Matcher<View> getConstraints() {
                return mCloseSoftKeyboard.getConstraints();
            }

            @Override
            public String getDescription() {
                return mCloseSoftKeyboard.getDescription();
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                mCloseSoftKeyboard.perform(uiController, view);
                uiController.loopMainThreadForAtLeast(KEYBOARD_DISMISSAL_DELAY_MILLIS);
            }
        };
    }

    /**
     * Matcher for ListView with exactly specified number of items.
     */
    static TypeSafeMatcher<View> listViewItemCount(final int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return count == ((ListView) view).getCount();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a ListView which contains " + count + " item(s)");
            }
        };
    }

    /**
     * Matcher for Spinner with exactly specified number of items.
     */
    static TypeSafeMatcher<View> spinnerItemCount(final int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return count == ((Spinner) view).getCount();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a Spinner which contains " + count + " item(s)");
            }
        };
    }

    /**
     * Checks if view has a background set.
     * Used for checking if note is selected.
     */
    static Matcher<View> isHighlighted() {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return view.getBackground() != null;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a View which is highlighted");
            }
        };
    }
}

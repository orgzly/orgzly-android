package com.orgzly.android.espresso;

import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CloseKeyboardAction;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.orgzly.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/*
 * Few espresso-related notes:
 *
 * - closeSoftKeyboardWithDelay() is often used, as on some devices keyboard will cover the view
 *   which is supposed to be clicked next, causing java.lang.SecurityException to get thrown.
 *
 * - replaceText() is preferred over typeText() as it is much faster.
 */
class EspressoUtils {
    static ViewInteraction onList() {
        return onView(allOf(isAssignableFrom(ListView.class), isDisplayed()));
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

    @SuppressWarnings("unchecked")
    public static DataInteraction onListItem(int pos) {
        return onData(anything())
                .inAdapterView(allOf(isAssignableFrom(ListView.class), isDisplayed()))
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
        SystemClock.sleep(750);
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

        onData(PreferenceMatchers.withKey("prefs_screen_notebooks")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_states")).perform(click());

        onView(withId(viewId)).perform(replaceText(keywords), closeSoftKeyboardWithDelay());
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());

        pressBack();
        pressBack();
    }

    static void openContextualToolbarOverflowMenu() {
        onView(allOf(
                withContentDescription(R.string.abc_action_menu_overflow_description),
                isDescendantOfA(withId(R.id.toolbar))
        )).perform(click());
    }

    static void searchForText(String str) {
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        onView(withHint(R.string.search_hint)).perform(replaceText(str), pressKey(66));
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

    public static ViewAction setNumber(final int num) {
        return new ViewAction() {
            @Override
            public void perform(UiController uiController, View view) {
                NumberPicker np = (NumberPicker) view;
                np.setValue(num);

            }

            @Override
            public String getDescription() {
                return "Set the passed number into the NumberPicker";
            }

            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(NumberPicker.class);
            }
        };
    }

    public static ViewAction clickClickableSpan(final CharSequence textToClick) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return Matchers.instanceOf(TextView.class);
            }

            @Override
            public String getDescription() {
                return "Click text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView textView = (TextView) view;
                Spanned spannableString = (Spanned) textView.getText();

                // Get the links inside the TextView and check if we find textToClick
                ClickableSpan[] spans = spannableString.getSpans(0, spannableString.length(), ClickableSpan.class);
                if (spans.length > 0) {
                    ClickableSpan spanCandidate;
                    for (ClickableSpan span : spans) {
                        spanCandidate = span;
                        int start = spannableString.getSpanStart(spanCandidate);
                        int end = spannableString.getSpanEnd(spanCandidate);
                        CharSequence sequence = spannableString.subSequence(start, end);
                        if (textToClick.toString().equals(sequence.toString())) {
                            span.onClick(textView);
                            return;
                        }
                    }
                }

                // Fall-back to just click the entire view
                click().perform(uiController, view);
            }
        };
    }
}

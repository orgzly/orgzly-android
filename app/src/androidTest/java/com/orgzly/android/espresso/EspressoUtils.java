package com.orgzly.android.espresso;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.CloseKeyboardAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.ui.SpanUtils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.Matchers.anything;

/*
 * Few espresso-related notes:
 *
 * - closeSoftKeyboardWithDelay() is often used, as on some devices keyboard will cover the view
 *   which is supposed to be clicked next, causing java.lang.SecurityException to get thrown.
 *
 * - replaceText() is preferred over typeText() as it is much faster.
 */
class EspressoUtils {
    static ViewInteraction onListView() {
        return onView(allOf(isAssignableFrom(ListView.class), isDisplayed()));
    }

    static ViewInteraction onRecyclerView() {
        return onView(allOf(isAssignableFrom(RecyclerView.class), isDisplayed()));
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

    static TypeSafeMatcher<View> recyclerViewItemCount(final int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return count == ((RecyclerView) view).getAdapter().getItemCount();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                        "a RecyclerView adapter which contains " + count + " item(s)");
            }
        };
    }

    static TypeSafeMatcher<View> toolbarItemCount(final int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return count == ((Toolbar) view).getChildCount();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a Toolbar which contains " + count + " item(s)");
            }
        };
    }

    public static DataInteraction onListItem(int pos) {
        return onData(anything())
                .inAdapterView(allOf(isAssignableFrom(ListView.class),isDisplayed()))
                .atPosition(pos);
    }

    public static ViewInteraction onItemInDrawer(int position, @IdRes int childView) {
        return onRecyclerViewItem(R.id.design_navigation_view, position, childView);
    }

    public static ViewInteraction onBook(int position) {
        return onBook(position, -1);
    }

    public static ViewInteraction onBook(int position, @IdRes int childView) {
        return onRecyclerViewItem(R.id.fragment_books_recycler_view, position, childView);
    }

    public static ViewInteraction onPreface() {
        return onNoteInBook(0, -1);
    }

    public static ViewInteraction onPreface(@IdRes int childView) {
        return onNoteInBook(0, childView);
    }

    public static ViewInteraction onNotesInBook() {
        return onView(withId(R.id.fragment_notes_book_recycler_view));
    }

    public static ViewInteraction onNoteInBook(int position) {
        return onNoteInBook(position, -1);
    }

    public static ViewInteraction onNoteInBook(int position, @IdRes int childView) {
        return onRecyclerViewItem(R.id.fragment_notes_book_recycler_view, position, childView);
    }

    public static ViewInteraction onNotesInSearch() {
        return onView(withId(R.id.fragment_query_search_recycler_view));
    }

    public static ViewInteraction onNoteInSearch(int position) {
        return onNoteInSearch(position, -1);
    }

    public static ViewInteraction onNoteInSearch(int position, @IdRes int childView) {
        return onRecyclerViewItem(R.id.fragment_query_search_recycler_view, position, childView);
    }

    public static ViewInteraction onNotesInAgenda() {
        return onView(withId(R.id.fragment_query_agenda_recycler_view));
    }

    public static ViewInteraction onItemInAgenda(int position) {
        return onItemInAgenda(position, -1);
    }

    public static ViewInteraction onItemInAgenda(int position, @IdRes int childView) {
        return onRecyclerViewItem(R.id.fragment_query_agenda_recycler_view, position, childView);
    }

    public static ViewInteraction onSavedSearch(int position) {
        return onRecyclerViewItem(R.id.fragment_saved_searches_recycler_view, position, -1);
    }

    public static ViewInteraction onRecyclerViewItem(@IdRes int recyclerView, int position, @IdRes int childView) {
        onView(withId(recyclerView)).perform(RecyclerViewActions.scrollToPosition(position));

        return onView(new EspressoRecyclerViewMatcher(recyclerView)
                .atPositionOnView(position, childView));
    }

    private static class EspressoRecyclerViewMatcher {
        private final int recyclerViewId;

        private EspressoRecyclerViewMatcher(int recyclerViewId) {
            this.recyclerViewId = recyclerViewId;
        }

        public Matcher<View> atPosition(final int position) {
            return atPositionOnView(position, -1);
        }

        public Matcher<View> atPositionOnView(final int position, final int targetViewId) {

            return new TypeSafeMatcher<View>() {
                Resources resources = null;
                View childView;

                public void describeTo(Description description) {
                    String idDescription = Integer.toString(recyclerViewId);
                    if (this.resources != null) {
                        try {
                            idDescription = this.resources.getResourceName(recyclerViewId);
                        } catch (Resources.NotFoundException var4) {
                            idDescription = String.format("%s (resource name not found)", recyclerViewId);
                        }
                    }

                    description.appendText("with id: " + idDescription);
                }

                public boolean matchesSafely(View view) {

                    this.resources = view.getResources();

                    if (childView == null) {
                        RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                        if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                            if (holder != null) {
                                childView = holder.itemView;
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }

                    if (targetViewId == -1) {
                        return view == childView;
                    } else {
                        View targetView = childView.findViewById(targetViewId);
                        return view == targetView;
                    }

                }
            };
        }
    }

    static ViewInteraction onSnackbar() {
        return onView(withId(com.google.android.material.R.id.snackbar_text));
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
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
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

    public static void clickSetting(String key, int title) {
        onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(title)), click()));
    }

    static void settingsSetTodoKeywords(String keywords) {
        settingsSetKeywords(R.id.todo_states, keywords);
    }

    static void settingsSetDoneKeywords(String keywords) {
        settingsSetKeywords(R.id.done_states, keywords);
    }

    private static void settingsSetKeywords(int viewId, String keywords) {
        onActionItemClick(R.id.activity_action_settings, R.string.settings);

        clickSetting("prefs_screen_notebooks", R.string.pref_title_notebooks);
        clickSetting("pref_key_states", R.string.states);

        onView(withId(viewId)).perform(replaceTextCloseKeyboard(keywords));
        onView(withText(R.string.ok)).perform(click());
        onView(withText(R.string.yes)).perform(click());

        pressBack();
        pressBack();
    }

    static void openContextualToolbarOverflowMenu() {
        onView(allOf(
                anyOf(
                        withContentDescription(R.string.abc_action_menu_overflow_description),
                        withClassName(endsWith("OverflowMenuButton"))),
                isDescendantOfA(withId(R.id.toolbar))
        )).perform(click());
    }

    static void searchForText(String str) {
        onView(allOf(withId(R.id.activity_action_search), isDisplayed())).perform(click());
        onView(withHint(R.string.search_hint)).perform(replaceText(str), pressKey(KeyEvent.KEYCODE_ENTER));
    }

    static ViewAction[] replaceTextCloseKeyboard(String str) {
        return new ViewAction[] { replaceText(str), closeSoftKeyboardWithDelay() };
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
                Spanned spannable = (Spanned) textView.getText();

                ClickableSpan clickable = null;
                for (ClickableSpan span: SpanUtils.getSpans(spannable, ClickableSpan.class)) {
                    int start = spannable.getSpanStart(span);
                    int end = spannable.getSpanEnd(span);

                    CharSequence sequence = spannable.subSequence(start, end);
                    if (sequence.toString().contains(textToClick)) {
                        clickable = span;
                        break;
                    }
                }

                if (clickable != null) {
                    clickable.onClick(textView);
                } else {
                    throw new IllegalStateException("No clickable span found in " + spannable);
                }
            }
        };
    }
}

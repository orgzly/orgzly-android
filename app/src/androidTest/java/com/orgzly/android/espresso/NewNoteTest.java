package com.orgzly.android.espresso;

import androidx.test.core.app.ActivityScenario;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.util.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.util.EspressoUtils.onBook;
import static com.orgzly.android.espresso.util.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.util.EspressoUtils.replaceTextCloseKeyboard;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

public class NewNoteTest extends OrgzlyTest {
    @Test
    public void testNewNoteInEmptyNotebook() {
        testUtils.setupBook("notebook", "");
        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit))
                .perform(replaceTextCloseKeyboard("new note created by test"));
        onView(withId(R.id.done)).perform(click()); // Note done

        onNoteInBook(1, R.id.item_head_title_view)
                .check(matches(allOf(withText("new note created by test"), isDisplayed())));
    }

    @Test
    public void testNewNoteUnder() {
        testUtils.setupBook("notebook", "description\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.new_note, R.string.new_note);
        onView(withText(R.string.new_under)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("A"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText("2")));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText("3")));
        onNoteInBook(4, R.id.item_head_title_view).check(matches(withText("4")));
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText("A")));
        testUtils.assertBook("notebook", "description\n\n* 1\n** 2\n*** 3\n*** 4\n*** A\n** 5\n* 6\n");
    }

    @Test
    public void testNewNoteAbove() {
        testUtils.setupBook("notebook", "description\n* 1\n** 2\n*** 3\n*** 4\n** 5\n* 6");
        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.new_note, R.string.new_note);
        onView(withText(R.string.new_above)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("A"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onNoteInBook(1, R.id.item_head_title_view).check(matches(withText("1")));
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText("A")));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText("2")));
        testUtils.assertBook("notebook", "description\n\n* 1\n** A\n** 2\n*** 3\n*** 4\n** 5\n* 6\n");
    }

    @Test
    public void testNewNoteBelow() {
        testUtils.setupBook("booky", "Booky Preface\n" +
                                          "* 1\n" +
                                          "** 2\n" +
                                          "*** 3\n" +
                                          "*** 4\n" +
                                          "** 5\n" +
                                          "* 6");
        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        onNoteInBook(2).perform(longClick());
        onActionItemClick(R.id.new_note, R.string.new_note);
        onView(withText(R.string.new_below)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("A"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onNoteInBook(2, R.id.item_head_title_view).check(matches(withText("2")));
        onNoteInBook(3, R.id.item_head_title_view).check(matches(withText("3")));
        onNoteInBook(4, R.id.item_head_title_view).check(matches(withText("4")));
        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText("A")));
        onNoteInBook(6, R.id.item_head_title_view).check(matches(withText("5")));

        testUtils.assertBook("booky", "Booky Preface\n" +
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
    public void testNewNoteAfterMovingNotesAround() {
        testUtils.setupBook("notebook-1", "");
        ActivityScenario.launch(MainActivity.class);

        onBook(0).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("A"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("B"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("C"));
        onView(withId(R.id.done)).perform(click()); // Note done
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("Parent 1"));
        onView(withId(R.id.done)).perform(click()); // Note done

        /* Move A B and C under Parent 1. */
        for (int i = 0; i < 3; i++) {
            onNoteInBook(1).perform(longClick());
            onActionItemClick(R.id.move, R.string.move);
            onView(withId(R.id.notes_action_move_down)).perform(click());
            onView(withId(R.id.notes_action_move_down)).perform(click());
            onView(withId(R.id.notes_action_move_down)).perform(click());
            onView(withId(R.id.notes_action_move_right)).perform(click());
            pressBack();
            pressBack();
        }

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("Parent 2"));
        onView(withId(R.id.done)).perform(click()); // Note done

        onNoteInBook(1).perform(longClick());
        onActionItemClick(R.id.new_note, R.string.new_note);
        onView(withText(R.string.new_under)).perform(click());
        onView(withId(R.id.title_edit)).perform(replaceTextCloseKeyboard("Note"));
        onView(withId(R.id.done)).perform(click()); // Note done

        onNoteInBook(5, R.id.item_head_title_view).check(matches(withText(endsWith("Note"))));
    }
}

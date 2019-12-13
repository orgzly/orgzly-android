package com.orgzly.android.espresso;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;
import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.BookFormat;
import com.orgzly.android.LocalStorage;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onBook;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class BooksTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("book-1",
                "First book used for testing\n" +
                "* Note A.\n" +
                "** Note B.\n" +
                "* TODO Note C.\n" +
                "SCHEDULED: <2014-01-01>\n" +
                "** Note D.\n" +
                "*** TODO Note E.\n" +
                ""
        );

        testUtils.setupBook("book-2",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "CLOSED: [2014-06-03 Tue 13:34]\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* DONE Note #8.\n" +
                "CLOSED: [2014-06-03 Tue 3:34]\n" +
                "**** Note #9.\n" +
                "SCHEDULED: <2014-05-26 Mon>\n" +
                "** Note #10.\n" +
                ""
        );
    }

    @Test
    public void testOpenSettings() {
        activityRule.launchActivity(null);
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onView(withText(R.string.look_and_feel)).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnToNonExistentBookByPressingBack() {
        activityRule.launchActivity(null);
        onView(allOf(withText("book-1"), withId(R.id.item_book_title))).perform(click());
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());
        onView(allOf(withText("book-1"), withId(R.id.item_book_title))).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());
        pressBack();
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onView(withText(R.string.book_does_not_exist_anymore)).check(matches(isDisplayed()));
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())));
        pressBack();
        onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withText("book-2"), withId(R.id.item_book_title))).perform(click());
        onView(allOf(withText(R.string.book_does_not_exist_anymore), isDisplayed())).check(doesNotExist());
    }

    @Test
    public void testEnterPrefaceForNonExistentBook() {
        activityRule.launchActivity(null);
        onView(allOf(withText("book-1"), withId(R.id.item_book_title))).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());

        onView(allOf(withText("book-1"), withId(R.id.item_book_title))).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());
        pressBack();

        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
        onView(withText(R.string.book_does_not_exist_anymore)).check(matches(isDisplayed()));

        openContextualToolbarOverflowMenu();
        onView(withText(R.string.edit_book_preface)).check(doesNotExist());
    }

    @Test
    public void testExport() throws IOException {
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            testExportQ();
//        } else {
            testExportPreQ();
//        }
    }

    private void testExportQ() {
        activityRule.launchActivity(null);
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();

        Intents.init();

        // Response to get after app sends Intent.ACTION_CREATE_DOCUMENT
        Intent resultData = new Intent();
        File file = new File(context.getCacheDir(), "book-1.org");
        resultData.setData(DocumentFile.fromFile(file).getUri());
        ActivityResult result = new ActivityResult(Activity.RESULT_OK, resultData);
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(result);

        // Perform export
        onView(withText(R.string.export)).perform(click());

        // Check that app has sent intent
        intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasExtra(Intent.EXTRA_TITLE, "book-1.org")));

        // Check that file was exported.
        onSnackbar().check(matches(withText(startsWith(context.getString(R.string.book_exported, "")))));

        // Delete exported file
        file.delete();

        Intents.release();
    }

    private void testExportPreQ() throws IOException {
        activityRule.launchActivity(null);
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.export)).perform(click());
        onSnackbar().check(matches(withText(startsWith(context.getString(R.string.book_exported, "")))));
        localStorage.getExportFile("book-1", BookFormat.ORG).delete();
    }

    @Test
    public void testCreateNewBookWithoutExtension() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("book-created-from-scratch"));
        onView(withText(R.string.create)).perform(click());
        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateNewBookWithExtension() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("book-created-from-scratch.org"));
        onView(withText(R.string.create)).perform(click());
        onView(allOf(withText("book-created-from-scratch.org"), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_book_view_flipper)).check(matches(isDisplayed()));
    }

    @Test
    public void testCreateAndDeleteBook() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("book-created-from-scratch"));
        onView(withText(R.string.create)).perform(click());

        onView(allOf(withText("book-created-from-scratch"), isDisplayed())).check(matches(isDisplayed()));

        onBook(2).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());

        onView(withText("book-created-from-scratch")).check(doesNotExist());
    }

    @Test
    public void testDifferentBookLoading() {
        activityRule.launchActivity(null);
        onView(allOf(withText("book-1"), isDisplayed())).perform(click());
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note A.")));
        pressBack();
        onView(allOf(withText("book-2"), isDisplayed())).perform(click());
        onNoteInBook(1, R.id.item_head_title).check(matches(withText("Note #1.")));
    }

    @Test
    public void testLoadingBookOnlyIfFragmentHasViewCreated() {
        activityRule.launchActivity(null);
        onView(allOf(withText("book-1"), isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withText(R.string.notebooks)).perform(click());

        onBook(1).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());
    }

    @Test
    public void testCreateNewBookWithExistingName() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("new-book"));
        onView(withText(R.string.create)).perform(click());

        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard("new-book"));
        onView(withText(R.string.create)).perform(click());

        onSnackbar().check(matches(
                withText(context.getString(R.string.book_name_already_exists, "new-book"))));
    }

    @Test
    public void testCreateNewBookWithWhiteSpace() {
        activityRule.launchActivity(null);
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.dialog_input)).perform(replaceTextCloseKeyboard(" new-book  "));
        onView(withText(R.string.create)).perform(click());
        onBook(2, R.id.item_book_title).check(matches(withText("new-book")));
    }

    @Test
    public void testRenameBookToExistingName() {
        activityRule.launchActivity(null);
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceTextCloseKeyboard("book-2"));
        onView(withText(R.string.rename)).perform(click());
        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(context.getString(R.string.book_name_already_exists, "book-2")))));
    }

    @Test
    public void testRenameBookToSameName() {
        activityRule.launchActivity(null);
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.rename)).perform(click());
        onView(withText(R.string.rename)).check(matches(not(isEnabled())));
    }

    @Test
    public void testNoteCountDisplayed() throws IOException {
        testUtils.setupBook("book-3", "");
        activityRule.launchActivity(null);

        onBook(0, R.id.item_book_note_count)
                .check(matches(withText(context.getResources().getQuantityString(R.plurals.notes_count_nonzero, 5, 5))));
        onBook(1, R.id.item_book_note_count)
                .check(matches(withText(context.getResources().getQuantityString(R.plurals.notes_count_nonzero, 10, 10))));
        onBook(2, R.id.item_book_note_count)
                .check(matches(withText(R.string.notes_count_zero)));
    }
}

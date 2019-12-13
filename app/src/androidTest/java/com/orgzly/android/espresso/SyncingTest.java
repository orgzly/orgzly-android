package com.orgzly.android.espresso;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.repos.RepoType;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.main.MainActivity;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.close;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.clickSetting;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onBook;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.onNoteInBook;
import static com.orgzly.android.espresso.EspressoUtils.onNotesInBook;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.openContextualToolbarOverflowMenu;
import static com.orgzly.android.espresso.EspressoUtils.recyclerViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

@SuppressWarnings("unchecked")
public class SyncingTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule =
            new EspressoActivityTestRule<>(MainActivity.class);

    /**
     * Utility method for starting sync using drawer button.
     */
    private void sync() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.sync_button_container)).perform(click());
        onView(withId(R.id.drawer_layout)).perform(close());
    }

    @Test
    public void testRunSync() {
        activityRule.launchActivity(null);
        sync();
    }

    @Test
    public void testForceLoadingBookWithLink() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org", "New content", "abc", 1234567890000L);
        testUtils.setupBook("booky", "First book used for testing\n* Note A");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onBook(0, R.id.item_book_last_action)
                .check(matches((withText(containsString(context.getString(R.string.force_loaded_from_uri, "mock://repo-a/booky.org"))))));

        onView(allOf(withText("booky"), isDisplayed())).perform(click());
        onView(withText("New content")).check(matches(isDisplayed()));
    }

    @Test
    public void testAutoSyncIsTriggeredAfterCreatingNote() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org", "", "abc", 1234567890000L);
        activityRule.launchActivity(null);
        sync();

        // Set preference
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_sync", R.string.sync);
        clickSetting("prefs_screen_auto_sync", R.string.auto_sync);
        clickSetting("pref_key_auto_sync", R.string.auto_sync);
        clickSetting("pref_key_auto_sync_on_note_create", R.string.pref_title_sync_after_note_create);
        pressBack();
        pressBack();
        pressBack();

        // Open book
        onBook(0).perform(click());

        // Create note
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceTextCloseKeyboard("new note created by test"));
        onView(withId(R.id.done)).perform(click());

        // Check it is synced
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), withId(R.id.item_book_title))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.item_book_sync_needed_icon))).check(matches(not(isDisplayed())));
    }

    @Test
    public void testPrefaceModificationMakesBookOutOfSync() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org", "", "abc", 1234567890000L);
        activityRule.launchActivity(null);
        sync();

        onBook(0, R.id.item_book_sync_needed_icon).check(matches(not(isDisplayed())));

        // Change preface
        onBook(0).perform(click());
        onActionItemClick(R.id.books_options_menu_book_preface, R.string.edit_book_preface);
        onView(withId(R.id.fragment_book_preface_content))
                .perform(replaceTextCloseKeyboard("Modified preface"));
        onView(withId(R.id.done)).perform(click());
        pressBack();

        onBook(0, R.id.item_book_sync_needed_icon).check(matches(isDisplayed()));
    }

    @Test
    public void nonLinkedBookCannotBeMadeOutOfSync() {
        testUtils.setupBook("booky", "* Note A");
        activityRule.launchActivity(null);

        onBook(0, R.id.item_book_sync_needed_icon).check(matches(not(isDisplayed())));

        // Modify book
        onBook(0).perform(click());
        onNoteInBook(1).perform(longClick());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        pressBack();
        pressBack();

        onBook(0, R.id.item_book_sync_needed_icon).check(matches(not(isDisplayed())));
    }

    @Test
    public void testForceLoadingBookWithNoLinkNoRepos() {
        testUtils.setupBook("booky", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);
        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onSnackbar().check(matches(withText(endsWith(context.getString(R.string.message_book_has_no_link)))));
    }

    @Test
    public void testForceLoadingBookWithNoLinkSingleRepo() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onSnackbar().check(matches(withText(endsWith(context.getString(R.string.message_book_has_no_link)))));
    }

    /* Books view was returning multiple entries for the same book, due to duplicates in encodings
     * table. The last statement in this method will fail if there are multiple books matching.
     */
    @Test
    public void testForceLoadingMultipleTimes() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book-one.org", "New content", "abc", 1234567890000L);
        testUtils.setupBook("book-one", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_loaded_from_uri, "mock://repo-a/book-one.org")))));

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_loaded_from_uri, "mock://repo-a/book-one.org")))));
    }

    /*
     * Book is left with out-of-sync icon when it's modified, then force-loaded.
     * This is because book's mtime was not being updated and was greater then remote book's mtime.
     */
    @Test
    public void testForceLoadingAfterModification() {
        testUtils.setupBook("book-one", "* Note");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        activityRule.launchActivity(null);

        // Force save
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        // Modify book
        onBook(0).perform(click());
        onNoteInBook(1).perform(longClick());
        onView(withId(R.id.bottom_action_bar_done)).perform(click());
        pressBack();
        pressBack();

        // Force load
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        // Check sync icon
        onView(allOf(withId(R.id.item_book_sync_needed_icon))).check(matches(not(isDisplayed())));
    }

    @Test
    public void testForceSavingBookWithNoLinkAndMultipleRepos() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupBook("book-one", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_saving_failed, context.getString(R.string.multiple_repos))))));

    }

    @Test
    public void testForceSavingBookWithNoLinkNoRepos() {
        testUtils.setupBook("book-one", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_saving_failed, context.getString(R.string.no_repos))))));
    }

    @Test
    public void testForceSavingBookWithNoLinkSingleRepo() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("book-one", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_saved_to_uri, "mock://repo-a/book-one.org")))));
    }

    @Test
    public void testForceSavingBookWithLink() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "First book used for testing\n* Note A", repo);
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withId(R.id.books_context_menu_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());

        onBook(0, R.id.item_book_last_action)
                .check(matches(withText(endsWith(
                        context.getString(R.string.force_saved_to_uri, "mock://repo-a/booky.org")))));
    }

    @Test
    public void testSyncButton() throws IOException {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("book-one", "First book used for testing\n* Note A");
        testUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);
        sync();
    }

    @Test
    public void testSavingAndLoadingBookBySyncing() throws IOException {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "");
        activityRule.launchActivity(null);

        sync();
        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());
        onView(withId(R.id.item_book_card_view)).check(matches(not(isDisplayed())));
        sync();
        onView(withId(R.id.item_book_card_view)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onBook(0).perform(click());
        onNoteInBook(2).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackToModifiedBookAfterSyncing() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");

        testUtils.setupBook("booky",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* Note #8.\n" +
                "**** Note #9.\n" +
                "** ANTIVIVISECTIONISTS Note #10.\n" +
                "** Note #11. DIE PERSER (Ü: Andreas Röhler) Schauspiel 1 D 3 H Stand:\n" +
                "");
        activityRule.launchActivity(null);

        sync();
        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDisplayed())).perform(click());

        /* Make sure book has been uploaded to repo and is linked now. */
        onBook(0, R.id.item_book_link_repo).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onBook(0, R.id.item_book_synced_url).check(matches(allOf(withText("mock://repo-a/booky.org"), isDisplayed())));

        /* Modify remote book directly. */
        testUtils.setupRook(repo, "mock://repo-a/booky.org", "NEW CONTENT", "abc", 1234567890000L);

        sync();

        /* Go back to book. */
        pressBack();
        onView(withText("NEW CONTENT")).check(matches(isDisplayed()));
    }

    @Test
    public void testBookParsingAfterKeywordsSettingChange() throws IOException {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky",
                "Sample book used for tests\n" +
                "* Note #1.\n" +
                "* Note #2.\n" +
                "** TODO Note #3.\n" +
                "** Note #4.\n" +
                "*** DONE Note #5.\n" +
                "**** Note #6.\n" +
                "** Note #7.\n" +
                "* Note #8.\n" +
                "**** Note #9.\n" +
                "** ANTIVIVISECTIONISTS Note #10.\n" +
                "** Note #11. DIE PERSER (Ü: Andreas Röhler) Schauspiel 1 D 3 H Stand:\n" +
                "");
        activityRule.launchActivity(null);

        sync();

        onView(allOf(withText("booky"), withId(R.id.item_book_title))).perform(click());

        /* Open note "ANTIVIVISECTIONISTS Note #10." and check title. */
        onNoteInBook(10).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(allOf(withText("ANTIVIVISECTIONISTS Note #10."), isDisplayed())));

        settingsSetTodoKeywords("ANTIVIVISECTIONISTS");

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_books_view_flipper)).check(matches(isDisplayed()));

        /* Delete book */
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.delete)).perform(click());

        sync();

        onView(allOf(withText("booky"), withId(R.id.item_book_title))).perform(click());

        /* Open note "ANTIVIVISECTIONISTS Note #10." and check title. */
        onNoteInBook(10).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(allOf(withText("Note #10."), isDisplayed())));
    }

    @Test
    public void testChangeBookLink() throws IOException {
        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        Repo repoB = testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupRook(repoA, "mock://repo-a/book-1.org", "Remote content for book in repo a", "abc", 1234567890);
        testUtils.setupRook(repoB, "mock://repo-b/book-1.org", "Remote content for book in repo b", "def", 1234567891);
        activityRule.launchActivity(null);

        sync();

        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS.msg())), isDisplayed())));

        /* Set link to repo-b. */
        onView(allOf(withText("book-1"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-b")).perform(click());

        onBook(0, R.id.item_book_link_repo).check(matches(withText("mock://repo-b")));

        sync();

        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITH_LINK.msg())), isDisplayed())));

        onBook(0).perform(click());
        onView(withText("Remote content for book in repo b")).check(matches(isDisplayed()));
        pressBack();

        /* Set link to repo-a. */
        onView(allOf(withText("book-1"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onBook(0, R.id.item_book_link_repo).check(matches(withText("mock://repo-a")));

        sync();

        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT.msg())), isDisplayed())));

        /* Still the same content due to conflict. */
        onBook(0).perform(click());
        onView(withText("Remote content for book in repo b")).check(matches(isDisplayed()));
    }

    @Test
    public void testSyncTwiceInARow() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        Repo repoB = testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupRook(repoB, "mock://repo-b/book-2.org", "Remote content for book 2", "abc", 1234567890000L);
        testUtils.setupRook(repoB, "mock://repo-b/book-3.org", "Remote content for book 3", "def", 1234567891000L);
        testUtils.setupBook("book-1", "Local content for book 1");
        testUtils.setupBook("book-2", "Local content for book 2", repoB);
        activityRule.launchActivity(null);

        sync();

        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS.msg())), isDisplayed())));
        onBook(1, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE.msg())), isDisplayed())));
        onBook(2, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK.msg())), isDisplayed())));

        onBook(0).perform(click());
        onView(withText("Local content for book 1")).check(matches(isDisplayed()));
        pressBack();
        onBook(1).perform(click());
        onView(withText("Local content for book 2")).check(matches(isDisplayed()));
        pressBack();
        /* Whole notebook view is too big to fit on small devices' screen, so we get
         * "at least 90 percent of the view's area is displayed to the user"
         * when trying to click on it. Clicking on specific view inside (book name) instead.
         */
        onBook(2, R.id.item_book_title).perform(click());
        onView(withText("Remote content for book 3")).check(matches(isDisplayed()));
        pressBack();

        sync();

        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS.msg())), isDisplayed())));

        onBook(1, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE.msg())), isDisplayed())));

        onBook(2, R.id.item_book_last_action)
                .check(matches(allOf(withText(containsString(BookSyncStatus.NO_CHANGE.msg())), isDisplayed())));

        onBook(0).perform(click());
        onView(withText("Local content for book 1")).check(matches(isDisplayed()));
        pressBack();
        onBook(1).perform(click());
        onView(withText("Local content for book 2")).check(matches(isDisplayed()));
        pressBack();
        onBook(2).perform(click());
        onView(withText("Remote content for book 3")).check(matches(isDisplayed()));
    }

    @Test
    public void testEncodingAfterSyncSaving() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book-one.org", "Täht", "1abcde", 1400067156000L);
        activityRule.launchActivity(null);

        sync();
        onBook(0, R.id.item_book_encoding_used)
                .check(matches((withText(context.getString(R.string.argument_used, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_detected)
                .check(matches((withText(context.getString(R.string.argument_detected, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_selected)
                .check(matches(not(isDisplayed())));

        sync();
        onBook(0, R.id.item_book_encoding_used)
                .check(matches((withText(context.getString(R.string.argument_used, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_detected)
                .check(matches((withText(context.getString(R.string.argument_detected, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_selected)
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingLinkToRenamedRepo() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org", "Täht", "1abcde", 1400067156000L);
        activityRule.launchActivity(null);

        sync();
        onBook(0, R.id.item_book_link_repo)
                .check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onBook(0, R.id.item_book_synced_url)
                .check(matches(allOf(withText("mock://repo-a/booky.org"), isDisplayed())));
        onBook(0, R.id.item_book_encoding_used)
                .check(matches((withText(context.getString(R.string.argument_used, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_detected)
                .check(matches((withText(context.getString(R.string.argument_detected, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_selected)
                .check(matches(not(isDisplayed())));

        /* Rename repository. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_sync", R.string.sync);
        clickSetting("pref_key_repos", R.string.repos_preference_title);
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceTextCloseKeyboard("repo-b"));
        onActionItemClick(R.id.done, R.string.done);
        pressBack();
        pressBack();
        pressBack();

        /* Set link to new repository. */
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("dropbox:/repo-b")).perform(click());

        onBook(0, R.id.item_book_link_repo)
                .check(matches(allOf(withText("dropbox:/repo-b"), isDisplayed())));
        onBook(0, R.id.item_book_synced_url)
                .check(matches(not(isDisplayed())));
        onBook(0, R.id.item_book_encoding_used)
                .check(matches((withText(context.getString(R.string.argument_used, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_detected)
                .check(matches((withText(context.getString(R.string.argument_detected, "UTF-8")))));
        onBook(0, R.id.item_book_encoding_selected)
                .check(matches(not(isDisplayed())));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("dropbox:/repo-b")).check(matches(isDisplayed())); // Current value
    }

    @Test
    public void testRenamingReposRemovesLinksWhatUsedThem() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupBook("booky", "");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onBook(0, R.id.item_book_link_repo).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));

        /* Rename all repositories. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        clickSetting("prefs_screen_sync", R.string.sync);
        clickSetting("pref_key_repos", R.string.repos_preference_title);
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceTextCloseKeyboard("repo-1"));
        onActionItemClick(R.id.done, R.string.done);
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceTextCloseKeyboard("repo-2"));
        onActionItemClick(R.id.done, R.string.done);
        pressBack();
        pressBack();
        pressBack();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());

        onBook(0, R.id.item_book_link_repo).check(matches(not(isDisplayed())));
    }

    @Test
    public void testRemovingLinkFromBook() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "", repo);
        activityRule.launchActivity(null);

        onBook(0, R.id.item_book_link_repo).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText(R.string.remove_notebook_link)).perform(click());

        onBook(0, R.id.item_book_link_container).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingLinkForLoadedOrgTxtBook() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org.txt", "", "1abcdef", 1400067155);
        activityRule.launchActivity(null);

        sync();

        onBook(0, R.id.item_book_link_repo).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onBook(0, R.id.item_book_last_action).check(matches(withText(containsString("Loaded from mock://repo-a/booky.org.txt"))));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onBook(0, R.id.item_book_link_repo).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onBook(0, R.id.item_book_synced_url).check(matches(allOf(withText("mock://repo-a/booky.org.txt"), isDisplayed())));
    }

    @Test
    public void testSpaceSeparatedBookName() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/Book%20Name.org", "", "1abcdef", 1400067155);
        activityRule.launchActivity(null);

        sync();

        onBook(0, R.id.item_book_synced_url)
                .check(matches(allOf(withText("mock://repo-a/Book%20Name.org"), isDisplayed())));
        onBook(0, R.id.item_book_last_action)
                .check(matches(allOf(withText(endsWith("Loaded from mock://repo-a/Book%20Name.org")), isDisplayed())));
    }

    @Test
    public void testRenameModifiedBook() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "* Note");
        activityRule.launchActivity(null);

        sync();

        onBook(0).perform(click()); // Open notebook
        onNoteInBook(1).perform(click()); // Open note
        onView(withId(R.id.fragment_note_title)).perform(replaceTextCloseKeyboard("New title"));
        onView(withId(R.id.done)).perform(click());

        pressBack(); // Back to the list of notebooks

        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.books_context_menu_item_rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceTextCloseKeyboard("book-two"));
        onView(withText(R.string.rename)).perform(click());

        String errMsg = context.getString(
                R.string.failed_renaming_book_with_reason,
                "Notebook is not synced");

        onBook(0, R.id.item_book_last_action).check(matches(withText(endsWith(errMsg))));
    }

    @Test
    public void testDeSelectRemovedNote() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");

        testUtils.setupRook(
                repo,
                "mock://repo-a/book-a.org",
                "* TODO Note [a-1]\n* TODO Note [a-2]",
                "1520077116000",
                1520077116000L);
        activityRule.launchActivity(null);

        sync();

        onBook(0).perform(click());

        onNotesInBook().check(matches(recyclerViewItemCount(3)));

        onNoteInBook(1).perform(longClick());

        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        testUtils.setupRook(
                repo,
                "mock://repo-a/book-a.org",
                "* TODO Note [a-1]",
                "1520681916000",
                1520681916000L);

        // Sync by starting the service directly, to keep note selected
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(AppIntent.ACTION_SYNC_START);
        context.startService(intent);
        SystemClock.sleep(1000);

        onNotesInBook().check(matches(recyclerViewItemCount(2)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }

    @Test
    public void testDeleteNonExistentRemoteFile() throws IOException {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "Sample book used for tests");
        activityRule.launchActivity(null);

        sync();

        dbRepoBookRepository.deleteBook(Uri.parse("mock://repo-a/booky.org"));

        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withId(R.id.delete_linked_checkbox)).perform(click());
        onView(withText(R.string.delete)).perform(click());
    }

    @Test
    public void testDeleteExistingRemoteFile() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("booky", "Sample book used for tests");
        activityRule.launchActivity(null);

        sync();

        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onBook(0).perform(longClick());
        openContextualToolbarOverflowMenu();
        onView(withText(R.string.delete)).perform(click());
        onView(withId(R.id.delete_linked_checkbox)).perform(click());
        onView(withText(R.string.delete)).perform(click());

        Assert.assertEquals(0, dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).size());
    }
}

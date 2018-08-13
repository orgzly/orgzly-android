package com.orgzly.android.espresso;

import android.content.Intent;
import android.os.SystemClock;
import android.support.test.espresso.matcher.PreferenceMatchers;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.close;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.closeSoftKeyboardWithDelay;
import static com.orgzly.android.espresso.EspressoUtils.listViewItemCount;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onList;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.settingsSetTodoKeywords;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;

@SuppressWarnings("unchecked")
public class SyncingTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    /**
     * Utility method for starting sync using drawer button.
     */
    private void sync() {
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.sync_button_container)).perform(click());
        onView(withId(R.id.drawer_layout)).perform(close());
    }

    @Test
    public void testForceLoadingBookWithLink() {
        shelfTestUtils.setupBook("booky", "First book used for testing\n* Note A");
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "New content", "abc", 1234567890000L);
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches((withText(containsString(context.getString(R.string.force_loaded_from_uri, "mock://repo-a/booky.org"))))));

        onView(allOf(withText("booky"), isDisplayed())).perform(click());
        onView(withText("New content")).check(matches(isDisplayed()));
    }

    @Test
    public void testSyncAfterNoteCreatedPreference() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "", "abc", 1234567890000L);
        activityRule.launchActivity(null);
        sync();

        // Set preference
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_sync")).perform(click());
        onData(PreferenceMatchers.withKey("prefs_screen_auto_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_auto_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_auto_sync_on_note_create")).perform(click());
        pressBack();
        pressBack();
        pressBack();

        // Open book
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());

        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onListItem(0).perform(click());

        // Add note
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fragment_note_title))
                .perform(replaceText("new note created by test"), closeSoftKeyboardWithDelay());
        onView(withId(R.id.done)).perform(click());

        // Check it is synced
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), withId(R.id.item_book_title))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.item_book_modified_after_sync_icon))).check(matches(not(isDisplayed())));
    }

    @Test
    public void testForceLoadingBookWithNoLinkNoRepos() {
        shelfTestUtils.setupBook("booky", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);
        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(endsWith(context.getString(R.string.message_book_has_no_link)))).check(matches(isDisplayed()));
    }

    @Test
    public void testForceLoadingBookWithNoLinkSingleRepo() {
        shelfTestUtils.setupBook("booky", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(endsWith(context.getString(R.string.message_book_has_no_link))))
                .check(matches(isDisplayed()));
    }

    /* Books view was returning multiple entries for the same book, due to duplicates in encodings
     * table. The last statement in this method will fail if there are multiple books matching.
     */
    @Test
    public void testForceLoadingMultipleTimes() {
        shelfTestUtils.setupBook("book-one", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book-one.org", "New content", "abc", 1234567890000L);
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(containsString(context.getString(R.string.force_loaded_from_uri, "mock://repo-a/book-one.org")))).check(matches(isDisplayed()));

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_load)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(containsString(context.getString(R.string.force_loaded_from_uri, "mock://repo-a/book-one.org")))).check(matches(isDisplayed()));
    }

    @Test
    public void testForceSavingBookWithNoLinkAndMultipleRepos() {
        shelfTestUtils.setupBook("book-one", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(endsWith(context.getString(R.string.force_saving_failed, context.getString(R.string.multiple_repos))))).check(matches(isDisplayed()));
    }

    @Test
    public void testForceSavingBookWithNoLinkNoRepos() {
        shelfTestUtils.setupBook("book-one", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        activityRule.launchActivity(null);
        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(endsWith(context.getString(R.string.force_saving_failed, context.getString(R.string.no_repos))))).check(matches(isDisplayed()));
    }

    @Test
    public void testForceSavingBookWithNoLinkSingleRepo() {
        shelfTestUtils.setupBook("book-one", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        onView(allOf(withText("book-one"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(containsString(context.getString(R.string.force_saved_to_uri, "mock://repo-a/book-one.org")))).check(matches(isDisplayed()));
    }

    @Test
    public void testForceSavingBookWithLink() {
        shelfTestUtils.setupBook("booky", "First book used for testing\n* Note A", "mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_force_save)).perform(click());
        onView(withText(R.string.overwrite)).perform(click());
        onView(withText(containsString(context.getString(R.string.force_saved_to_uri, "mock://repo-a/booky.org")))).check(matches(isDisplayed()));
    }

    @Test
    public void testSyncButton() throws IOException {
        shelfTestUtils.setupBook("book-one", "First book used for testing\n* Note A");
        shelfTestUtils.setupBook("book-two", "Second book used for testing\n* Note 1\n* Note 2");
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);
        sync();
    }

    @Test
    public void testSavingAndLoadingBookBySyncing() throws IOException {
        shelfTestUtils.setupBook("booky",
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
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        sync();
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onListItem(0).perform(longClick());
        onData(hasToString(containsString(context.getString(R.string.delete)))).perform(click());
        onView(withText(R.string.delete)).perform(click());
        onView(withId(R.id.item_book_card_view)).check(doesNotExist());

        sync();
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));
        onView(allOf(withText("booky"), isDisplayed())).check(matches(isDisplayed()));
        onListItem(0).perform(click());
        onListItem(11).perform(click());
        onView(withText("Note #11. DIE PERSER (Ü: Andreas Röhler) Schauspiel 1 D 3 H Stand:")).perform(click());
        onView(withId(R.id.fragment_note_container)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackToModifiedBookAfterSyncing() throws IOException {
        shelfTestUtils.setupBook("booky",
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
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        sync();
        onView(allOf(withText("booky"), isDisplayed())).perform(click());

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDisplayed())).perform(click());

        /* Make sure book has been uploaded to repo and is linked now. */
        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_synced_url)).check(matches(allOf(withText("mock://repo-a/booky.org"), isDisplayed())));

        /* Modify remote book directly. */
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "NEW CONTENT", "abc", 1234567890000L);

        sync();

        /* Go back to book. */
        pressBack();
        onView(withText("NEW CONTENT")).check(matches(isDisplayed()));
    }

    @Test
    public void testBookParsingAfterKeywordsSettingChange() throws IOException {
        shelfTestUtils.setupBook("booky",
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
        shelfTestUtils.setupRepo("mock://repo-a");
        activityRule.launchActivity(null);

        sync();

        onView(allOf(withText("booky"), withId(R.id.item_book_title))).perform(click());

        /* Open note "ANTIVIVISECTIONISTS Note #10." and check title. */
        onListItem(10).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(allOf(withText("ANTIVIVISECTIONISTS Note #10."), isDisplayed())));

        settingsSetTodoKeywords("ANTIVIVISECTIONISTS");

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_books_container)).check(matches(isDisplayed()));

        /* Delete book */
        onListItem(0).perform(longClick());
        onData(hasToString(containsString(context.getString(R.string.delete)))).perform(click());
        onView(withText(R.string.delete)).perform(click());

        sync();

        onView(allOf(withText("booky"), withId(R.id.item_book_title))).perform(click());

        /* Open note "ANTIVIVISECTIONISTS Note #10." and check title. */
        onListItem(10).perform(click());
        onView(withId(R.id.fragment_note_title)).check(matches(allOf(withText("Note #10."), isDisplayed())));
    }

    @Test
    public void testChangeBookLink() throws IOException {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book-1.org", "Remote content for book in repo a", "abc", 1234567890);
        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupRook("mock://repo-b", "mock://repo-b/book-1.org", "Remote content for book in repo b", "def", 1234567891);
        activityRule.launchActivity(null);

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS.msg())), isDisplayed())));

        /* Set link to repo-b. */
        onView(allOf(withText("book-1"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-b")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(withText("mock://repo-b")));

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITH_LINK.msg())), isDisplayed())));

        onListItem(0).perform(click());
        onView(withText("Remote content for book in repo b")).check(matches(isDisplayed()));
        pressBack();

        /* Set link to repo-a. */
        onView(allOf(withText("book-1"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(withText("mock://repo-a")));

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_LAST_SYNCED_ROOK_AND_LATEST_ROOK_ARE_DIFFERENT.msg())), isDisplayed())));

        /* Still the same content due to conflict. */
        onListItem(0).perform(click());
        onView(withText("Remote content for book in repo b")).check(matches(isDisplayed()));
    }

    @Test
    public void test1() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupRook("mock://repo-b", "mock://repo-b/book-2.org", "Remote content for book 2", "abc", 1234567890000L);
        shelfTestUtils.setupRook("mock://repo-b", "mock://repo-b/book-3.org", "Remote content for book 3", "def", 1234567891000L);
        shelfTestUtils.setupBook("book-1", "Local content for book 1");
        shelfTestUtils.setupBook("book-2", "Local content for book 2", "mock://repo-b");
        activityRule.launchActivity(null);

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS.msg())), isDisplayed())));
        onListItem(1).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE.msg())), isDisplayed())));
        onListItem(2).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK.msg())), isDisplayed())));

        onListItem(0).perform(click());
        onView(withText("Local content for book 1")).check(matches(isDisplayed()));
        pressBack();
        onListItem(1).perform(click());
        onView(withText("Local content for book 2")).check(matches(isDisplayed()));
        pressBack();
        /* Whole notebook view is too big to fit on small devices' screen, so we get
         * "at least 90 percent of the view's area is displayed to the user"
         * when trying to click on it. Clicking on specific view inside (book name) instead.
         */
        onListItem(2).onChildView(withId(R.id.item_book_title)).perform(click());
        onView(withText("Remote content for book 3")).check(matches(isDisplayed()));
        pressBack();

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_MULTIPLE_REPOS.msg())), isDisplayed())));

        onListItem(1).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.CONFLICT_BOOK_WITH_LINK_AND_ROOK_BUT_NEVER_SYNCED_BEFORE.msg())), isDisplayed())));

        onListItem(2).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(containsString(BookSyncStatus.NO_CHANGE.msg())), isDisplayed())));

        onListItem(0).perform(click());
        onView(withText("Local content for book 1")).check(matches(isDisplayed()));
        pressBack();
        onListItem(1).perform(click());
        onView(withText("Local content for book 2")).check(matches(isDisplayed()));
        pressBack();
        onListItem(2).perform(click());
        onView(withText("Remote content for book 3")).check(matches(isDisplayed()));
        pressBack();
    }

    @Test
    public void testEncodingAfterSyncSaving() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book-one.org", "Täht", "1abcde", 1400067156000L);
        activityRule.launchActivity(null);

        sync();
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used)).check(matches((withText("UTF-8 used"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected)).check(matches((withText("UTF-8 detected"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected)).check(matches(not(isDisplayed())));

        sync();
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used)).check(matches((withText("UTF-8 used"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected)).check(matches((withText("UTF-8 detected"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingLinkToRenamedRepo() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "Täht", "1abcde", 1400067156000L);
        activityRule.launchActivity(null);

        sync();
        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_synced_url)).check(matches(allOf(withText("mock://repo-a/booky.org"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used)).check(matches((withText("UTF-8 used"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected)).check(matches((withText("UTF-8 detected"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected)).check(matches(not(isDisplayed())));

        /* Rename repository. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_repos")).perform(click());
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceText("repo-b"));
        onActionItemClick(R.id.done, R.string.done);
        pressBack();
        pressBack();
        pressBack();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("dropbox:/repo-b")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("dropbox:/repo-b"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_synced_url)).check(matches(allOf(withText("mock://repo-a/booky.org"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_used)).check(matches((withText("UTF-8 used"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_detected)).check(matches((withText("UTF-8 detected"))));
        onListItem(0).onChildView(withId(R.id.item_book_encoding_selected)).check(matches(not(isDisplayed())));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("dropbox:/repo-b")).check(matches(isDisplayed())); // Current value
    }

    @Test
    public void testRenamingReposRemovesLinksWhatUsedThem() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupBook("booky", "");
        activityRule.launchActivity(null);

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));

        /* Rename all repositories. */
        onActionItemClick(R.id.activity_action_settings, R.string.settings);
        onData(PreferenceMatchers.withKey("prefs_screen_sync")).perform(click());
        onData(PreferenceMatchers.withKey("pref_key_repos")).perform(click());
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceText("repo-1"));
        onActionItemClick(R.id.done, R.string.done);
        onListItem(0).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceText("repo-2"));
        onActionItemClick(R.id.done, R.string.done);
        pressBack();
        pressBack();
        pressBack();

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(allOf(withText(R.string.notebooks), isDescendantOfA(withId(R.id.drawer_navigation_view)))).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testRemovingLinkFromBook() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupBook("booky", "", "mock://repo-a");
        activityRule.launchActivity(null);

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText(R.string.remove_notebook_link)).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSettingLinkForLoadedOrgTxtBook() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org.txt", "", "1abcdef", 1400067155);
        activityRule.launchActivity(null);

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_last_action)).check(matches(withText(containsString("Loaded from mock://repo-a/booky.org.txt"))));

        onView(allOf(withText("booky"), isDisplayed())).perform(longClick());
        onView(withText(R.string.books_context_menu_item_set_link)).perform(click());
        onView(withText("mock://repo-a")).perform(click());

        onListItem(0).onChildView(withId(R.id.item_book_link_repo)).check(matches(allOf(withText("mock://repo-a"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_synced_url)).check(matches(allOf(withText("mock://repo-a/booky.org.txt"), isDisplayed())));
    }

    @Test
    public void testSpaceSeparatedBookName() throws IOException {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/Book%20Name.org", "", "1abcdef", 1400067155);
        activityRule.launchActivity(null);

        sync();

        onListItem(0).onChildView(withId(R.id.item_book_synced_url))
                .check(matches(allOf(withText("mock://repo-a/Book%20Name.org"), isDisplayed())));
        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(allOf(withText(endsWith("Loaded from mock://repo-a/Book%20Name.org")), isDisplayed())));
    }

    @Test
    public void testRenameModifiedBook() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupBook("booky", "* Note");
        activityRule.launchActivity(null);

        sync();

        onListItem(0).perform(click()); // Open notebook
        onListItem(0).perform(click()); // Open note
        onView(withId(R.id.fragment_note_title)).perform(replaceText("New title"));
        onView(withId(R.id.done)).perform(click());

        pressBack(); // Back to the list of notebooks

        onListItem(0).perform(longClick());
        onView(withText(R.string.books_context_menu_item_rename)).perform(click());
        onView(withId(R.id.name)).perform(replaceText("book-two"), closeSoftKeyboard());
        onView(withText(R.string.rename)).perform(click());

        String errMsg = context.getString(
                R.string.failed_renaming_book_with_reason,
                "Notebook is not synced");

        onListItem(0).onChildView(withId(R.id.item_book_last_action))
                .check(matches(withText(endsWith(errMsg))));
    }

    @Test
    public void testDeSelectRemovedNote() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook(
                "mock://repo-a",
                "mock://repo-a/book-a.org",
                "* TODO Note [a-1]\n* TODO Note [a-2]",
                "1520077116000",
                1520077116000L);
        activityRule.launchActivity(null);

        sync();

        onListItem(0).perform(click());
        onListItem(0).perform(longClick());

        onList().check(matches(listViewItemCount(2)));
        onView(withId(R.id.action_bar_title)).check(matches(withText("1")));

        shelfTestUtils.setupRook(
                "mock://repo-a",
                "mock://repo-a/book-a.org",
                "* TODO Note [a-1]",
                "1520681916000",
                1520681916000L);

        // Sync by starting the service directly, to keep note selected
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(AppIntent.ACTION_SYNC_START);
        context.startService(intent);
        SystemClock.sleep(1000);

        onList().check(matches(listViewItemCount(1)));
        onView(withId(R.id.action_bar_title)).check(doesNotExist());
    }
}

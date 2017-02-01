package com.orgzly.android.espresso;

import android.content.Intent;
import android.os.Environment;
import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.ReposActivity;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;

/**
 *
 */
public class ReposActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(ReposActivity.class, true, false);

    private void startActivityWithIntent(String action, String type, String extraText) {
        Intent intent = new Intent();

        if (action != null) {
            intent.setAction(action);
        }

        if (type != null) {
            intent.setType(type);
        }

        if (extraText != null) {
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
        }

        activityRule.launchActivity(intent);
    }

    @Test
    public void testBrowserDirectorySelection() {
        startActivityWithIntent(Intent.ACTION_VIEW, null, null);

        String d = Environment.getExternalStorageDirectory().getPath();
        String repoUri = "file:" + d;

        onActionItemClick(R.id.repos_options_menu_item_new, "New Repository");
        onView(withText(R.string.directory)).perform(click());
        onView(withId(R.id.fragment_repo_directory_browse_button)).perform(click());
        onView(withId(R.id.browser_title)).check(matches(withText(d)));
        onData(hasToString(containsString("Download"))).perform(click());
        onView(withId(R.id.browser_button_use)).perform(click());
        onView(withId(R.id.fragment_repo_directory)).check(matches(withText(repoUri + "/Download")));
    }

    @Test
    public void testDirectoryRepoSelectingDifferentDirectoryThenStarting() {
        String repoUri = "file:" + Environment.getExternalStorageDirectory().getPath();

        shelfTestUtils.setupRepo(repoUri);
        startActivityWithIntent(Intent.ACTION_VIEW, null, null);

        onListItem(0).perform(click());
        onView(withId(R.id.fragment_repo_directory_browse_button)).perform(click());
        onData(hasToString(containsString("Download"))).perform(click());
        onView(withId(R.id.browser_button_use)).perform(click());
        onView(withId(R.id.fragment_repo_directory)).check(matches(withText(repoUri + "/Download")));
    }

    @Test
    public void testDirectoryRepoBrowsingStartsWithInvalidDirectory() {
        startActivityWithIntent(Intent.ACTION_VIEW, null, null);

        onActionItemClick(R.id.repos_options_menu_item_new, "New Repository");
        onView(withText(R.string.directory)).perform(click());
        onView(withId(R.id.fragment_repo_directory)).perform(replaceText("non-existent-directory"));
        onView(withId(R.id.fragment_repo_directory_browse_button)).perform(click());
    }

    @Test
    public void testDirectoryRepoWithPercentCharacter() {
        String localBaseDir = context.getExternalCacheDir().getAbsolutePath();
        String localDir = localBaseDir + "/nextcloud/user@host%2Fdir";
        String repoUri = "file:" + localBaseDir + "/nextcloud/user@host%252Fdir";

        new File(localDir).mkdirs();

        startActivityWithIntent(Intent.ACTION_VIEW, null, null);

        onActionItemClick(R.id.repos_options_menu_item_new, "New Repository");
        onView(withText(R.string.directory)).perform(click());
        onView(withId(R.id.fragment_repo_directory)).perform(replaceText(repoUri));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.fragment_repos_flipper)).check(matches(isDisplayed()));

        onListItem(0).onChildView(withId(R.id.item_repo_url)).check(matches(withText("file:" + localDir)));
        onListItem(0).perform(click());

        onView(withId(R.id.fragment_repo_directory)).check(matches(withText(repoUri)));
    }

    @Test
    public void testDropboxRepoWithPercentCharacter() {
        String localDir = "/Documents/user@host%2Fdir";

        startActivityWithIntent(Intent.ACTION_VIEW, null, null);

        onActionItemClick(R.id.repos_options_menu_item_new, "New Repository");
        onView(withText(R.string.dropbox)).perform(click());
        onView(withId(R.id.fragment_repo_dropbox_directory)).perform(replaceText(localDir));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.fragment_repos_flipper)).check(matches(isDisplayed()));

        onListItem(0).onChildView(withId(R.id.item_repo_url)).check(matches(withText("dropbox:" + localDir)));
        onListItem(0).perform(click());

        onView(withId(R.id.fragment_repo_dropbox_directory)).check(matches(withText(localDir)));
    }
}

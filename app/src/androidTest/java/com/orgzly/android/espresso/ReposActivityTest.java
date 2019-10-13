package com.orgzly.android.espresso;

import androidx.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.repos.ReposActivity;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;
import static com.orgzly.android.espresso.EspressoUtils.onSnackbar;
import static com.orgzly.android.espresso.EspressoUtils.replaceTextCloseKeyboard;

public class ReposActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new EspressoActivityTestRule<>(ReposActivity.class);

    @Test
    public void testSavingWithBogusDirectoryUri() {
        activityRule.launchActivity(null);
        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_repos_directory)).perform(scrollTo(), click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceTextCloseKeyboard("non-existent-directory"));
        onView(withId(R.id.done)).perform(click());
    }

    @Test
    public void testDirectoryRepoWithPercentCharacter() {
        String localBaseDir = context.getExternalCacheDir().getAbsolutePath();
        String localDir = localBaseDir + "/nextcloud/user@host%2Fdir";
        String repoUri = "file:" + localBaseDir + "/nextcloud/user@host%252Fdir";

        new File(localDir).mkdirs();

        activityRule.launchActivity(null);

        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_repos_directory)).perform(scrollTo(), click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceTextCloseKeyboard(repoUri));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));

        onListItem(0).onChildView(withId(R.id.item_repo_url)).check(matches(withText(repoUri)));
        onListItem(0).perform(click());

        onView(withId(R.id.activity_repo_directory)).check(matches(withText(repoUri)));
    }

    @Test
    public void testDropboxRepoWithPercentCharacter() {
        String localDir = "/Documents/user@host%2Fdir";

        activityRule.launchActivity(null);

        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_repos_dropbox)).perform(click());
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceTextCloseKeyboard(localDir));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));

        onListItem(0).onChildView(withId(R.id.item_repo_url)).check(matches(withText("dropbox:/Documents/user%40host%252Fdir")));
        onListItem(0).perform(click());

        onView(withId(R.id.activity_repo_dropbox_directory)).check(matches(withText(localDir)));
    }

    @Test
    public void testCreateRepoWithExistingUrl() {
        activityRule.launchActivity(null);

        String url = "file:" + context.getExternalCacheDir().getAbsolutePath();

        onView(withId(R.id.activity_repos_directory)).perform(scrollTo(), click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceTextCloseKeyboard(url));
        onView(withId(R.id.done)).perform(click());

        onView(withId(R.id.repos_options_menu_item_new)).perform(click());
        onView(withText(R.string.directory)).perform(click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceTextCloseKeyboard(url));
        onView(withId(R.id.done)).perform(click());

        onSnackbar().check(matches(withText(R.string.repository_url_already_exists)));
    }
}

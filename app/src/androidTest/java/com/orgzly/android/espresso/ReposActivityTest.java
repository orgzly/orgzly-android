package com.orgzly.android.espresso;

import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.repos.ReposActivity;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onListItem;

/**
 *
 */
public class ReposActivityTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(ReposActivity.class, true, false);

    @Test
    public void testSavingWithBogusDirectoryUri() {
        activityRule.launchActivity(null);
        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_repos_directory)).perform(click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceText("non-existent-directory"));
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
        onView(withId(R.id.activity_repos_directory)).perform(click());
        onView(withId(R.id.activity_repo_directory)).perform(replaceText(repoUri));
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
        onView(withId(R.id.activity_repo_dropbox_directory)).perform(replaceText(localDir));
        onView(withId(R.id.done)).perform(click());
        onView(withId(R.id.activity_repos_flipper)).check(matches(isDisplayed()));

        onListItem(0).onChildView(withId(R.id.item_repo_url)).check(matches(withText("dropbox:/Documents/user%40host%252Fdir")));
        onListItem(0).perform(click());

        onView(withId(R.id.activity_repo_dropbox_directory)).check(matches(withText(localDir)));
    }
}

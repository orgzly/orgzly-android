package com.orgzly.android.ui.repos;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.orgzly.R;
import com.orgzly.android.Shelf;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.ContentRepo;
import com.orgzly.android.repos.DirectoryRepo;
import com.orgzly.android.repos.DropboxRepo;
import com.orgzly.android.repos.GitRepo;
import com.orgzly.android.repos.MockRepo;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.ui.fragments.FileBrowserOpener;
import com.orgzly.android.ui.fragments.GitRepoFragment;
import com.orgzly.android.ui.util.ActivityUtils;

/**
 * Configuring repositories.
 */
public class ReposActivity extends RepoActivity
        implements
        ReposFragment.ReposFragmentListener,
        RepoFragment.RepoFragmentListener,
        FileBrowserOpener {

    public static final String TAG = ReposActivity.class.getName();

    private Shelf mShelf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_repos);

        setupActionBar(R.string.repositories);

        mShelf = new Shelf(getApplicationContext());

        if (savedInstanceState == null) {
            Fragment fragment = ReposFragment.getInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_repos_frame, fragment, ReposFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ActivityUtils.INSTANCE.closeSoftKeyboard(this);
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRepoCreateRequest(Repo repo) {
        addRepoUrl(repo.getUri().toString());

        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onRepoUpdateRequest(long id, Repo repo) {
        updateRepoUrl(id, repo.getUri().toString());
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onRepoCancelRequest() {
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onRepoNewRequest(int id) {
        switch (id) {
            case R.id.repos_options_menu_item_new_dropbox:
                DropboxRepoActivity.start(this);
                return;

            case R.id.repos_options_menu_item_new_git:
                displayRepoFragment(GitRepoFragment.getInstance(), GitRepoFragment.FRAGMENT_TAG);
                return;

            case R.id.repos_options_menu_item_new_external_storage_directory:
                DirectoryRepoActivity.start(this);
                return;

            default:
                throw new IllegalArgumentException("Unknown repo menu item clicked: " + id);
        }
    }

    @Override
    public void onRepoDeleteRequest(long id) {
        deleteRepo(id);
    }

    @SuppressLint("StaticFieldLeak")
    private void deleteRepo(final long id) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.deleteRepo(id);
                return null;
            }
        }.execute();
    }

    @Override
    public void onRepoEditRequest(long id) {
        String url = ReposClient.getUrl(this, id);

        Repo repo = RepoFactory.getFromUri(this, url);

        if (repo instanceof DropboxRepo || repo instanceof MockRepo) {  // TODO: Remove Mock from here
            DropboxRepoActivity.start(this, id);

        } else if (repo instanceof DirectoryRepo || repo instanceof ContentRepo) {
            DirectoryRepoActivity.start(this, id);

        } else if (repo instanceof GitRepo) {
            displayRepoFragment(GitRepoFragment.getInstance(id), GitRepoFragment.FRAGMENT_TAG);

        } else {
            showSimpleSnackbarLong(R.string.message_unsupported_repository_type);
        }
    }

    private void displayRepoFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.activity_repos_frame, fragment, tag)
                .commit();
    }

    @Override
    public void browseDirectory(Uri uri, BrowserResultHandler resultHandler, boolean allowFileSelection) {

    }
}

package com.orgzly.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.ui.fragments.BookPrefaceFragment;
import com.orgzly.android.ui.fragments.BookFragment;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.FilterFragment;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.QueryFragment;
import com.orgzly.android.ui.fragments.SettingsFragment;
import com.orgzly.android.util.LogUtils;

public class DisplayManager {
    private static final String TAG = DisplayManager.class.getName();

    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_NONE;
    private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_FADE;

    /* To avoid jerky drawer close.
     * Previous title might be displayed if loading of the new fragment takes too long and it
     * might look ugly, showing for only a fraction of a second before being replaced with new one.
     * FIXME: Maybe move drawer handling here and add a flag for *not* changing the title.
     */
    private static final int DELAY_FRAGMENT_TRANSACTION_AFTER_DRAWER_CLOSE = 300;

    private final FragmentManager mFragmentManager;
    private final DrawerLayout mDrawerLayout;

    public DisplayManager(AppCompatActivity activity, Bundle savedInstanceState, DrawerLayout drawerLayout) {
        mFragmentManager = activity.getSupportFragmentManager();
        mDrawerLayout = drawerLayout;

        /* Display books if not a configuration change - starting for the first time. */
        if (savedInstanceState == null) {
            displayBooks(false);
        }
    }

    public void viewNoteRequest(long bookId, long noteId) {
        displayNote(false, bookId, noteId, Placement.UNDEFINED);
    }

    public void newNoteRequest(NotePlacement target) {
        displayNote(true, target.getBookId(), target.getNoteId(), target.getPlacement());
    }

    public void bookPrefaceRequest(Book book) {
        displayEditor(book);
    }

//    public void reposSettingsRequest() {
//        displayReposSettings();
//    }

    public void drawerFiltersRequest() {
        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                displayFilters();
            }
        }, DELAY_FRAGMENT_TRANSACTION_AFTER_DRAWER_CLOSE);
    }

    public void drawerBooksRequest() {
        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                displayBooks(true);
            }
        }, DELAY_FRAGMENT_TRANSACTION_AFTER_DRAWER_CLOSE);
    }

    public void drawerBookRequest(final long bookId) {
        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                displayBook(bookId, 0);
            }
        }, DELAY_FRAGMENT_TRANSACTION_AFTER_DRAWER_CLOSE);
    }

    public void drawerSearchRequest(final String query) {
        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                displayQuery(query);
            }
        }, DELAY_FRAGMENT_TRANSACTION_AFTER_DRAWER_CLOSE);
    }

    public void settingsRequest() {
        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        displaySettings();
    }

    public void bookRequest(long bookId, long noteId) {
        displayBook(bookId, noteId);
    }

    /**
     * Displays fragment for a new filter.
     */
    public void onFilterNewRequest() {
        /* Create fragment. */
        Fragment fragment = FilterFragment.getInstance();

        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FilterFragment.FRAGMENT_TAG)
                .commit();
    }

    /**
     * Displays fragment for existing filter.
     */
    public void onFilterEditRequest(long id) {
        /* Create fragment. */
        Fragment fragment = FilterFragment.getInstance(id);

        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FilterFragment.FRAGMENT_TAG)
                .commit();
    }

    /**
     * Return to original state.
     * Removes all fragments and then adds BooksFragment.
     */
    public void reset() {
        /* Clear the back stack. */
        mFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        /* Display starting fragment. */
        displayBooks(false);
    }

    private void displayFilters() {
        if (isFragmentDisplayed(FiltersFragment.FRAGMENT_TAG) != null) {
            return;
        }

        Fragment fragment = FiltersFragment.getInstance();

        FragmentTransaction t = mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FiltersFragment.FRAGMENT_TAG);


        t.commit();
    }

    /**
     * Show fragments listing books.
     * @param addToBackStack add to back stack or not
     */
    private void displayBooks(boolean addToBackStack) {
        if (isFragmentDisplayed(BooksFragment.FRAGMENT_TAG) != null) {
            return;
        }

        Fragment fragment = BooksFragment.getInstance();

        FragmentTransaction t = mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .replace(R.id.single_pane_container, fragment, BooksFragment.FRAGMENT_TAG);

        if (addToBackStack) {
            t.addToBackStack(null);
        }

        t.commit();
    }

    /**
     * Add fragment for book, unless the same book is already being displayed.
     */
    private void displayBook(long bookId, long noteId) {
        BookFragment f = getFragmentDisplayingBook(bookId);

        if (f == null) {
            /* Create fragment. */
            Fragment fragment = BookFragment.getInstance(bookId, noteId);

            /* Add fragment. */
            mFragmentManager
                    .beginTransaction()
                    .setTransition(FRAGMENT_TRANSITION)
                    .addToBackStack(null)
                    .replace(R.id.single_pane_container, fragment, BookFragment.FRAGMENT_TAG)
                    .commit();
        } else {
            Log.w(TAG, "Fragment displaying book " + bookId + " already exists");
        }
    }

    private void displayNote(boolean isNew, long bookId, long noteId, Placement placement) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId);

        if (bookId <= 0) {
            throw new IllegalArgumentException("Invalid book id (" + bookId + ")");
        }

        /* Create fragment. */
        Fragment fragment = NoteFragment.getInstance(isNew, bookId, noteId, placement, null, null);

        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                        // .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, NoteFragment.FRAGMENT_TAG)
                .commit();

        // .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left, R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        // .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
    }

    private void displayQuery(String query) {
        /* If the same query is already displayed, don't do anything. */
        SearchQuery displayedQuery = getDisplayedQuery();
        if (displayedQuery != null && displayedQuery.toString().equals(query)) {
            return;
        }

        /* Create fragment. */
        Fragment fragment = QueryFragment.getInstance(query);

        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, QueryFragment.FRAGMENT_TAG)
                .commit();
    }

//    private void displayReposSettings() {
//        Fragment fragment = ReposFragment.getInstance();
//
//        mFragmentManager
//                .beginTransaction()
//                .setTransition(FRAGMENT_TRANSITION)
//                .addToBackStack(null)
//                .replace(R.id.single_pane_container, fragment, ReposFragment.FRAGMENT_TAG)
//                .commit();
//    }

    private void displaySettings() {
        if (isFragmentDisplayed(SettingsFragment.FRAGMENT_TAG) != null) {
            return;
        }

        Fragment fragment = SettingsFragment.getInstance();

        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, SettingsFragment.FRAGMENT_TAG)
                .commit();
    }

    private void displayEditor(Book book) {
        /* Create fragment. */
        Fragment fragment = BookPrefaceFragment.getInstance(book.getId(), book.getPreface());

        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, BookPrefaceFragment.FRAGMENT_TAG)
                .commit();
    }

    private BookFragment getFragmentDisplayingBook(long bookId) {
        Fragment f = mFragmentManager.findFragmentByTag(BookFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            BookFragment bookFragment = (BookFragment) f;

            if (bookFragment.getBook() != null && bookFragment.getBook().getId() == bookId) {
                return bookFragment;
            }
        }

        return null;
    }

    private Fragment isFragmentDisplayed(String tag) {
        Fragment f = mFragmentManager.findFragmentByTag(tag);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "tag:" + tag + " fragment:" + f + " isVisible:" + (f != null ? f.isVisible() : "no"));

        if (f != null && f.isVisible()) {
            return f;
        } else {
            return null;
        }
    }

    public SearchQuery getDisplayedQuery() {
        Fragment f = mFragmentManager.findFragmentByTag(QueryFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            return ((QueryFragment) f).getQuery();
        }

        return null;
    }
}

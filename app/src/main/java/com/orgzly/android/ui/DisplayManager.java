package com.orgzly.android.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.ui.fragments.AgendaFragment;
import com.orgzly.android.ui.fragments.BookPrefaceFragment;
import com.orgzly.android.ui.fragments.BookFragment;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.FilterFragment;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.QueryFragment;
import com.orgzly.android.ui.settings.SettingsFragment;
import com.orgzly.android.util.LogUtils;

public class DisplayManager {
    private static final String TAG = DisplayManager.class.getName();

    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_NONE;
    private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_FADE;


    private final FragmentManager mFragmentManager;

    public DisplayManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
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
     * Removes all fragments except for the last one, displaying books.
     */
    public void clear() {
        /* Clear the back stack. */
        mFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void displayFilters() {
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
    public void displayBooks(boolean addToBackStack) {
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
    public void displayBook(long bookId, long noteId) {
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

    public void displayNote(long bookId, long noteId) {
        displayNote(false, bookId, noteId, Place.UNSPECIFIED);
    }

    public void displayNewNote(NotePlace target) {
        displayNote(true, target.getBookId(), target.getNoteId(), target.getPlace());
    }

    private void displayNote(boolean isNew, long bookId, long noteId, Place place) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId);

        if (bookId <= 0) {
            throw new IllegalArgumentException("Invalid book id (" + bookId + ")");
        }

        /* Create fragment. */
        Fragment fragment = NoteFragment.getInstance(isNew, bookId, noteId, place, null, null);

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

    public void displayQuery(String query) {
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

    public void displaySettings(String resource) {
        Fragment fragment = SettingsFragment.Companion.getInstance(resource);

        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, SettingsFragment.Companion.getFRAGMENT_TAG())
                .commit();
    }

    public void displayAgenda() {
        if (isFragmentDisplayed(AgendaFragment.FRAGMENT_TAG) != null) {
            return;
        }

        /* Create fragment. */
        Fragment fragment = AgendaFragment.getInstance();
        /* Add fragment. */
        mFragmentManager
                .beginTransaction()
                .setTransition(FRAGMENT_TRANSITION)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, AgendaFragment.FRAGMENT_TAG)
                .commit();
    }

    public void displayEditor(Book book) {
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

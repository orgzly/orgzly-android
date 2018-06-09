package com.orgzly.android.ui;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryParser;
import com.orgzly.android.query.user.InternalQueryParser;
import com.orgzly.android.ui.fragments.AgendaFragment;
import com.orgzly.android.ui.fragments.BookFragment;
import com.orgzly.android.ui.fragments.BookPrefaceFragment;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.FilterFragment;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.SearchFragment;
import com.orgzly.android.util.LogUtils;

/**
 * Manager for {@link MainActivity}'s fragments.
 */
public class DisplayManager {
    private static final String TAG = DisplayManager.class.getName();

    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_NONE;
    // private static final int FRAGMENT_TRANSITION = FragmentTransaction.TRANSIT_FRAGMENT_FADE;

    /**
     * Displays fragment for a new filter.
     */
    public static void onFilterNewRequest(FragmentManager fragmentManager) {
        /* Create fragment. */
        Fragment fragment = FilterFragment.getInstance();

        /* Add fragment. */
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FilterFragment.FRAGMENT_TAG)
                .commit();
    }

    /**
     * Displays fragment for existing filter.
     */
    public static void onFilterEditRequest(FragmentManager fragmentManager, long id) {
        /* Create fragment. */
        Fragment fragment = FilterFragment.getInstance(id);

        /* Add fragment. */
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FilterFragment.FRAGMENT_TAG)
                .commit();
    }

    /**
     * Return to original state.
     * Removes all fragments except for the last one, displaying books.
     */
    public static void clear(FragmentManager fragmentManager) {
        /* Clear the back stack. */
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public static void displayFilters(FragmentManager fragmentManager) {
        if (isFragmentDisplayed(fragmentManager, FiltersFragment.getFRAGMENT_TAG()) != null) {
            return;
        }

        Fragment fragment = FiltersFragment.getInstance();

        FragmentTransaction t = fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, FiltersFragment.getFRAGMENT_TAG());


        t.commit();
    }

    /**
     * Show fragments listing books.
     * @param addToBackStack add to back stack or not
     */
    public static void displayBooks(FragmentManager fragmentManager, boolean addToBackStack) {
        if (isFragmentDisplayed(fragmentManager, BooksFragment.FRAGMENT_TAG) != null) {
            return;
        }

        Fragment fragment = BooksFragment.getInstance();

        FragmentTransaction t = fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.single_pane_container, fragment, BooksFragment.FRAGMENT_TAG);

        if (addToBackStack) {
            t.addToBackStack(null);
        }

        t.commit();
    }

    /**
     * Add fragment for book, unless the same book is already being displayed.
     */
    public static void displayBook(FragmentManager fragmentManager, long bookId, long noteId) {
        if (getFragmentDisplayingBook(fragmentManager, bookId) == null) {
            /* Create fragment. */
            Fragment fragment = BookFragment.getInstance(bookId, noteId);

            /* Add fragment. */
            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                    .addToBackStack(null)
                    .replace(R.id.single_pane_container, fragment, BookFragment.FRAGMENT_TAG)
                    .commit();
        } else {
            Log.w(TAG, "Fragment displaying book " + bookId + " already exists");
        }
    }

    public static void displayNote(FragmentManager fragmentManager, long bookId, long noteId) {
        if (getFragmentDisplayingNote(fragmentManager, noteId) == null) {
            displayNote(fragmentManager, false, bookId, noteId, Place.UNSPECIFIED);
        }
    }

    public static void displayNewNote(FragmentManager fragmentManager, NotePlace target) {
        displayNote(fragmentManager, true, target.getBookId(), target.getNoteId(), target.getPlace());
    }

    private static void displayNote(FragmentManager fragmentManager, boolean isNew, long bookId, long noteId, Place place) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId);

        if (bookId > 0) {
            /* Create fragment. */
            Fragment fragment = NoteFragment.forBook(isNew, bookId, noteId, place);

            /* Add fragment. */
            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                    .addToBackStack(null)
                    .replace(R.id.single_pane_container, fragment, NoteFragment.FRAGMENT_TAG)
                    .commit();
        } else {
            Log.e(TAG, "displayNote: Invalid book id " + bookId);
        }
    }

    public static void displayQuery(FragmentManager fragmentManager, @NonNull String queryString) {
        // If the same query is already displayed, don't do anything.
        String displayedQuery = getDisplayedQuery(fragmentManager);
        if (displayedQuery != null && displayedQuery.equals(queryString)) {
            return;
        }

        // Parse query
        QueryParser queryParser = new InternalQueryParser();
        Query query = queryParser.parse(queryString);

        Fragment fragment;
        String tag;

        // Display agenda or query fragment
        if (query.getOptions().getAgendaDays() > 0) {
            fragment = AgendaFragment.getInstance(queryString);
            tag = AgendaFragment.FRAGMENT_TAG;

        } else {
            fragment = SearchFragment.getInstance(queryString);
            tag = SearchFragment.FRAGMENT_TAG;
        }

        // Add fragment.
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, tag)
                .commit();
    }

    public static void displayEditor(FragmentManager fragmentManager, Book book) {
        /* Create fragment. */
        Fragment fragment = BookPrefaceFragment.getInstance(book.getId(), book.getPreface());

        /* Add fragment. */
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.single_pane_container, fragment, BookPrefaceFragment.FRAGMENT_TAG)
                .commit();
    }

    public static String getDisplayedQuery(FragmentManager fragmentManager) {
        Fragment qf = fragmentManager.findFragmentByTag(SearchFragment.FRAGMENT_TAG);
        Fragment af = fragmentManager.findFragmentByTag(AgendaFragment.FRAGMENT_TAG);

        if (qf != null && qf.isVisible()) {
            return ((SearchFragment) qf).getQuery();

        } else if (af != null && af.isVisible()) {
            return ((AgendaFragment) af).getQuery();
        }

        return null;
    }

    private static BookFragment getFragmentDisplayingBook(FragmentManager fragmentManager, long bookId) {
        Fragment f = fragmentManager.findFragmentByTag(BookFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            BookFragment bookFragment = (BookFragment) f;

            if (bookFragment.getBook() != null && bookFragment.getBook().getId() == bookId) {
                return bookFragment;
            }
        }

        return null;
    }

    private static NoteFragment getFragmentDisplayingNote(FragmentManager fragmentManager, long noteId) {
        Fragment f = fragmentManager.findFragmentByTag(NoteFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            NoteFragment noteFragment = (NoteFragment) f;

            if (noteFragment.getNoteId() == noteId) {
                return noteFragment;
            }
        }

        return null;
    }

    private static Fragment isFragmentDisplayed(FragmentManager fragmentManager, String tag) {
        Fragment f = fragmentManager.findFragmentByTag(tag);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "tag:" + tag + " fragment:" + f + " isVisible:" + (f != null ? f.isVisible() : "no"));

        if (f != null && f.isVisible()) {
            return f;
        } else {
            return null;
        }
    }
}

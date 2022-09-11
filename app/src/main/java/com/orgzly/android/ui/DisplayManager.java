package com.orgzly.android.ui;

import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryParser;
import com.orgzly.android.query.user.InternalQueryParser;
import com.orgzly.android.ui.books.BooksFragment;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.android.ui.note.NoteFragment;
import com.orgzly.android.ui.notes.book.BookFragment;
import com.orgzly.android.ui.notes.book.BookPrefaceFragment;
import com.orgzly.android.ui.notes.query.agenda.AgendaFragment;
import com.orgzly.android.ui.notes.query.search.SearchFragment;
import com.orgzly.android.ui.savedsearch.SavedSearchFragment;
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment;
import com.orgzly.android.util.LogUtils;

/**
 * Manager for {@link MainActivity}'s fragments.
 */
public class DisplayManager {
    private static final String TAG = DisplayManager.class.getName();

    private static void replaceFragment(
            FragmentManager fragmentManager,
            @IdRes int containerViewId,
            @NonNull Fragment fragment,
            @Nullable String tag,
            boolean addToBackStack) {

        FragmentTransaction t = fragmentManager
                .beginTransaction()
                // .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(containerViewId, fragment, tag);

        if (addToBackStack) {
            t.addToBackStack(null);
        }

        t.commit();
    }
    /**
     * Displays fragment for a new saved search.
     */
    public static void onSavedSearchNewRequest(FragmentManager fragmentManager) {
        Fragment fragment = SavedSearchFragment.getInstance();

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                SavedSearchFragment.FRAGMENT_TAG,
                true);
    }

    /**
     * Displays fragment for existing saved search.
     */
    public static void onSavedSearchEditRequest(FragmentManager fragmentManager, long id) {
        Fragment fragment = SavedSearchFragment.getInstance(id);

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                SavedSearchFragment.FRAGMENT_TAG,
                true);
    }

    /**
     * Return to original state.
     * Removes all fragments except for the last one, displaying books.
     */
    public static void clear(FragmentManager fragmentManager) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public static void displaySavedSearches(FragmentManager fragmentManager) {
        if (isFragmentDisplayed(fragmentManager, SavedSearchesFragment.getFRAGMENT_TAG()) != null) {
            return;
        }

        Fragment fragment = SavedSearchesFragment.getInstance();

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                SavedSearchesFragment.getFRAGMENT_TAG(),
                true);
    }

    /**
     * Show fragments listing books.
     * @param addToBackStack add to back stack or not
     */
    public static void displayBooks(FragmentManager fragmentManager, boolean addToBackStack) {
        if (isFragmentDisplayed(fragmentManager, BooksFragment.Companion.getFRAGMENT_TAG()) != null) {
            return;
        }

        Fragment fragment = BooksFragment.getInstance();

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                BooksFragment.Companion.getFRAGMENT_TAG(),
                addToBackStack);
    }

    /**
     * Add fragment for book, unless the same book is already being displayed.
     */
    public static void displayBook(FragmentManager fragmentManager, long bookId, long noteId) {
        BookFragment existingFragment = getFragmentDisplayingBook(fragmentManager, bookId);

        if (existingFragment == null) {
            Fragment fragment = BookFragment.getInstance(bookId, noteId);

            replaceFragment(
                    fragmentManager,
                    R.id.single_pane_container,
                    fragment,
                    BookFragment.FRAGMENT_TAG,
                    true);

        } else {
            if (noteId > 0) {
                Log.w(TAG, "Fragment displaying book " + bookId + " already exists, jumping to note");
                existingFragment.scrollToNoteIfSet(noteId);
            } else {
                Log.w(TAG, "Fragment displaying book " + bookId + " already exists, ignoring");
            }
        }
    }

    public static void displayExistingNote(FragmentManager fragmentManager, long bookId, long noteId) {
        if (getFragmentDisplayingNote(fragmentManager, noteId) == null) {
            Fragment fragment = NoteFragment.forExistingNote(bookId, noteId);

            if (fragment != null) {
                displayNoteFragment(fragmentManager, fragment);
            }
        }
    }

    public static void displayNewNote(FragmentManager fragmentManager, NotePlace target) {
        Fragment fragment = NoteFragment.forNewNote(target);

        if (fragment != null) {
            displayNoteFragment(fragmentManager, fragment);
        }
    }

    private static void displayNoteFragment(FragmentManager fragmentManager, Fragment fragment) {
        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                NoteFragment.FRAGMENT_TAG,
                true);
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

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                tag,
                true);
    }

    public static void displayEditor(FragmentManager fragmentManager, Book book) {
        Fragment fragment = BookPrefaceFragment.Companion.getInstance(book.getId(), book.getPreface());

        replaceFragment(
                fragmentManager,
                R.id.single_pane_container,
                fragment,
                BookPrefaceFragment.Companion.getFRAGMENT_TAG(),
                true);
    }

    public static String getDisplayedQuery(FragmentManager fragmentManager) {
        Fragment qf = fragmentManager.findFragmentByTag(SearchFragment.FRAGMENT_TAG);
        Fragment af = fragmentManager.findFragmentByTag(AgendaFragment.FRAGMENT_TAG);

        if (qf != null && qf.isVisible()) {
            return ((SearchFragment) qf).getCurrentQuery();

        } else if (af != null && af.isVisible()) {
            return ((AgendaFragment) af).getCurrentQuery();
        }

        return null;
    }

    private static BookFragment getFragmentDisplayingBook(FragmentManager fragmentManager, long bookId) {
        Fragment f = fragmentManager.findFragmentByTag(BookFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            BookFragment bookFragment = (BookFragment) f;

            if (bookFragment.getCurrentBook() != null && bookFragment.getCurrentBook().getId() == bookId) {
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

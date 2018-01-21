package com.orgzly.android.provider;

import android.content.UriMatcher;

import com.orgzly.android.provider.ProviderContract.*;

class ProviderUris {
    final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    ProviderUris() {
        /* Filters (saved searches). */
        matcher.addURI(ProviderContract.AUTHORITY, Filters.MatcherUri.FILTERS_ID_UP, FILTER_UP);
        matcher.addURI(ProviderContract.AUTHORITY, Filters.MatcherUri.FILTERS_ID_DOWN, FILTER_DOWN);
        matcher.addURI(ProviderContract.AUTHORITY, Filters.MatcherUri.FILTERS_ID, FILTERS_ID);
        matcher.addURI(ProviderContract.AUTHORITY, Filters.MatcherUri.FILTERS, FILTERS);

        /* Repositories. */
        matcher.addURI(ProviderContract.AUTHORITY, Repos.MatcherUri.REPOS_ID, REPOS_ID);
        matcher.addURI(ProviderContract.AUTHORITY, Repos.MatcherUri.REPOS, REPOS);

        /* Notebooks. */
        matcher.addURI(ProviderContract.AUTHORITY, Books.MatcherUri.BOOKS_ID_NOTES, BOOKS_ID_NOTES);
        matcher.addURI(ProviderContract.AUTHORITY, BooksIdSaved.MatcherUri.BOOKS_ID_SAVED, BOOKS_ID_SAVED);
        matcher.addURI(ProviderContract.AUTHORITY, Books.MatcherUri.BOOKS_ID_CYCLE_VISIBILITY, BOOKS_ID_CYCLE_VISIBILITY);
        matcher.addURI(ProviderContract.AUTHORITY, Books.MatcherUri.BOOKS_ID_SPARSE_TREE, BOOKS_ID_SPARSE_TREE);
        matcher.addURI(ProviderContract.AUTHORITY, Books.MatcherUri.BOOKS_ID, BOOKS_ID);
        matcher.addURI(ProviderContract.AUTHORITY, Books.MatcherUri.BOOKS, BOOKS);

        matcher.addURI(ProviderContract.AUTHORITY, BookLinks.MatcherUri.BOOKS_ID_LINKS, LINKS_FOR_BOOK);

        matcher.addURI(ProviderContract.AUTHORITY, CurrentRooks.MatcherUri.CURRENT_ROOKS, CURRENT_ROOKS);

        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_SEARCH_QUERIED, NOTES_SEARCH_QUERIED);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_WITH_PROPERTY, NOTES_WITH_PROPERTY);

        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_ID_ABOVE, NOTE_ABOVE);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_ID_UNDER, NOTE_UNDER);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_ID_BELOW, NOTE_BELOW);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_STATE, NOTES_STATE);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_ID_TOGGLE_FOLDED_STATE, NOTE_TOGGLE_FOLDED_STATE);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES_ID, NOTE);
        matcher.addURI(ProviderContract.AUTHORITY, Notes.MatcherUri.NOTES, NOTES);

        matcher.addURI(ProviderContract.AUTHORITY, NoteProperties.MatcherUri.NOTES_PROPERTIES, NOTES_PROPERTIES);
        matcher.addURI(ProviderContract.AUTHORITY, NoteProperties.MatcherUri.NOTES_ID_PROPERTIES, NOTES_ID_PROPERTIES);

        matcher.addURI(ProviderContract.AUTHORITY, LocalDbRepo.MatcherUri.DB_REPOS, LOCAL_DB_REPO);

        /* Actions */
        matcher.addURI(ProviderContract.AUTHORITY, DbRecreate.MatcherUri.DB_RECREATE, DB_RECREATE);
        matcher.addURI(ProviderContract.AUTHORITY, DbTest.MatcherUri.DB_TEST, DB_SWITCH);
        matcher.addURI(ProviderContract.AUTHORITY, Cut.MatcherUri.CUT, CUT);
        matcher.addURI(ProviderContract.AUTHORITY, Paste.MatcherUri.PASTE, PASTE);
        matcher.addURI(ProviderContract.AUTHORITY, Delete.MatcherUri.DELETE, DELETE);
        matcher.addURI(ProviderContract.AUTHORITY, Promote.MatcherUri.PROMOTE, PROMOTE);
        matcher.addURI(ProviderContract.AUTHORITY, Demote.MatcherUri.DEMOTE, DEMOTE);
        matcher.addURI(ProviderContract.AUTHORITY, Move.MatcherUri.MOVE, MOVE);
        matcher.addURI(ProviderContract.AUTHORITY, LoadBookFromFile.MatcherUri.LOAD_FROM_FILE, LOAD_BOOK_FROM_FILE);

        matcher.addURI(ProviderContract.AUTHORITY, Times.MatcherUri.TIMES, TIMES);

    }

    static final int REPOS = 1;
    static final int REPOS_ID = 2;
    static final int BOOKS = 3;
    static final int BOOKS_ID = 4;
    static final int NOTES = 5;
    static final int NOTES_SEARCH_QUERIED = 6;
    static final int NOTES_STATE = 7;
    static final int NOTE_ABOVE = 8;
    static final int NOTE_UNDER = 9;
    static final int NOTE_BELOW = 10;
    static final int NOTES_ID_PROPERTIES = 11;
    static final int NOTE = 12;
    static final int CUT = 13;
    static final int PASTE = 14;
    static final int PROMOTE = 15;
    static final int MOVE = 16;
    static final int LOCAL_DB_REPO = 17;
    static final int DB_RECREATE = 18;
    static final int DB_SWITCH = 19;
    static final int LOAD_BOOK_FROM_FILE = 20;
    static final int FILTERS = 21;
    static final int FILTERS_ID = 22;
    static final int FILTER_UP = 23;
    static final int FILTER_DOWN = 24;
    static final int BOOKS_ID_NOTES = 25;
    static final int LINKS_FOR_BOOK = 26;
    static final int CURRENT_ROOKS = 27;
    static final int BOOKS_ID_SAVED = 28;
    static final int NOTES_PROPERTIES = 29;
    static final int BOOKS_ID_CYCLE_VISIBILITY = 30;
    static final int NOTE_TOGGLE_FOLDED_STATE = 31;
    static final int DEMOTE = 32;
    static final int DELETE = 33;
    static final int BOOKS_ID_SPARSE_TREE = 34;
    static final int TIMES = 35;
    static final int NOTES_WITH_PROPERTY = 36;
}

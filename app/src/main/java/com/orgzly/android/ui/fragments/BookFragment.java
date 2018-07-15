package com.orgzly.android.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ActionService;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Book;
import com.orgzly.android.BookUtils;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.views.DbNoteView;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.Fab;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.SelectionUtils;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.android.ui.views.TextViewWithMarkup;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Displays all notes from the notebook.
 * Allows moving, cutting, pasting etc.
 */
public class BookFragment extends NoteListFragment
        implements
        Fab,
        TimestampDialogFragment.OnDateTimeSetListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        DrawerItem {

    private static final String TAG = BookFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = BookFragment.class.getName();

    /* Arguments. */
    private static final String ARG_BOOK_ID = "bookId";
    private static final String ARG_NOTE_ID = "noteId";

    private static final int[] ITEMS_HIDDEN_ON_MULTIPLE_SELECTED_NOTES = {
            R.id.book_cab_new,
            R.id.book_cab_cut,
            R.id.book_cab_paste,
            R.id.book_cab_move,
            R.id.book_cab_refile
    };

    private BookFragmentListener listener;
    private boolean mIsViewCreated = false;


    private Book mBook;
    private Long mBookId;
    private static long mLastBookId;

    private View mHeader;
    private TextViewWithMarkup mPrefaceText;
    private View mNoNotesText;

    private SimpleCursorAdapter mListAdapter;

    private String mActionModeTag;

    /** Used to switch to book-does-not-exist view, if the book has been deleted. */
    private ViewFlipper mViewFlipper;


    /**
     * @param bookId Book ID
     * @param noteId Set position (scroll to) this note, if greater then zero
     */
    public static BookFragment getInstance(long bookId, long noteId) {
        BookFragment fragment = new BookFragment();

        Bundle args = new Bundle();
        args.putLong(BookFragment.ARG_BOOK_ID, bookId);
        args.putLong(BookFragment.ARG_NOTE_ID, noteId);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BookFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }


    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context);
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            listener = (BookFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + BookFragmentListener.class);
        }
        try {
            mActionModeListener = (ActionModeListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + ActionModeListener.class);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);

        parseArguments();

        if (savedInstanceState != null && savedInstanceState.getBoolean("actionModeMove", false)) {
            mActionModeTag = "M";
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        /*
         * If loader is for last loaded book, just init, do not restart.
         * Trying not to reset the loader and null the adapter, to keep the scroll position.
         *
         * NOTE: Opening a new book fragment for the same book will not restore the previous
         * fragment's list view, even though book ID is the same. This is because callbacks
         * will be different (they are two separate fragments displaying the same book).
         */
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Book Loader: Last book " + mLastBookId + ", new book " + mBookId);

        if (mLastBookId == mBookId) {
            getActivity().getSupportLoaderManager().initLoader(Loaders.BOOK_FRAGMENT_BOOK, null, this);
            getActivity().getSupportLoaderManager().initLoader(Loaders.BOOK_FRAGMENT_NOTES, null, this);
        } else {
            getActivity().getSupportLoaderManager().restartLoader(Loaders.BOOK_FRAGMENT_BOOK, null, this);
            getActivity().getSupportLoaderManager().restartLoader(Loaders.BOOK_FRAGMENT_NOTES, null, this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_book, container, false);

        final ListView listView = view.findViewById(android.R.id.list);

        mHeader = inflater.inflate(R.layout.item_head_book_preface, listView, false);

        mPrefaceText = mHeader.findViewById(R.id.fragment_book_header_text);
        if (getActivity() != null && AppPreferences.isFontMonospaced(getContext())) {
            mPrefaceText.setTypeface(Typeface.MONOSPACE);
        }

        /* If preface changes (for example by toggling the checkbox), update it. */
        mPrefaceText.setUserTextChangedListener(() ->
                ActionService.Companion.enqueueWork(
                        getActivity(),
                        new Intent(getActivity(), ActionService.class)
                                .setAction(AppIntent.ACTION_UPDATE_BOOK)
                                .putExtra(AppIntent.EXTRA_BOOK_ID, mBookId)
                                .putExtra(AppIntent.EXTRA_BOOK_PREFACE, mPrefaceText.getRawText())));


        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_book_view_flipper);

        /* Big new note button when there are no notes. */
        mNoNotesText = view.findViewById(R.id.fragment_book_no_notes);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        mIsViewCreated = true;

        /* Long click listener. */
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > getListView().getHeaderViewsCount() - 1) { /* Not a header. */
                    listener.onNoteLongClick(BookFragment.this, view, position, id, id);
                    return true;
                } else {
                    return false;
                }
            }
        });

        /* Item toolbar listener. */
        getListView().setOnItemMenuButtonClickListener(
                (itemView, buttonId, noteId) -> {
                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, buttonId, noteId);

                    switch (buttonId) {
                        case R.id.item_menu_delete_btn:
                            delete(MiscUtils.set(noteId));

                            /* Remove selection. */
                            mSelection.clearSelection();

                            break;

                        case R.id.item_menu_new_above_btn:
                            listener.onNoteNewRequest(new NotePlace(mBookId, noteId, Place.ABOVE));
                            break;

                        case R.id.item_menu_new_under_btn:
                            listener.onNoteNewRequest(new NotePlace(mBookId, noteId, Place.UNDER));
                            break;

                        case R.id.item_menu_new_below_btn:
                            listener.onNoteNewRequest(new NotePlace(mBookId, noteId, Place.BELOW));
                            break;

                        case R.id.item_menu_refile_btn:
                            openNoteRefileDialog(listener, mBookId, Collections.singleton(noteId));
                            break;

                        default:
                            onButtonClick(listener, itemView, buttonId, noteId);
                    }
                });

        /* If it's not set to null, we get java.lang.IllegalStateException:
         *   Cannot add header view to list -- setAdapter has already been called.
         * This happens when trying to set header for list, when getting fragment from back stack.
         */
        setListAdapter(null);

        /* Add selectable header to the list view. */
        getListView().addHeaderView(mHeader, null, true);

        /* Create a selection. Must be aware of number of headers. */
        mSelection = new Selection();

        mListAdapter = new HeadsListViewAdapter(getActivity(), mSelection, getListView().getItemMenus(), true);

        setListAdapter(mListAdapter);

        mSelection.restoreIds(savedInstanceState);
        /* Reset from ids will be performed after loading the data. */
    }

    @Override
    public NoteListFragmentListener getListener() {
        return listener;
    }

    @Override
    public void onPause() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onPause();

        /*
         * Stop list from scrolling.
         *
         * Workaround to avoid:
         *
         *   "java.lang.IllegalStateException: attempt to re-open an already-closed object".
         *
         * This happens after fling, when opening a different book from the drawer while the list
         * is still scrolling.  This can also be avoided by destroying loaders here, but then we
         * would lose book's scrolling position.
         */
        getListView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();

        mIsViewCreated = false;
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        listener = null;
        mActionModeListener = null;
    }

    private void parseArguments() {
        if (getArguments() == null) {
            throw new IllegalArgumentException("No arguments found to " + BookFragment.class.getSimpleName());
        }

        /* Book ID. */
        if (!getArguments().containsKey(ARG_BOOK_ID)) {
            throw new IllegalArgumentException(BookFragment.class.getSimpleName() + " requires "+ ARG_BOOK_ID + " argument passed");
        }
        mBookId = getArguments().getLong(ARG_BOOK_ID);
        if (mBookId <= 0) {
            throw new IllegalArgumentException("Passed argument book id is not valid (" + mBookId + ")");
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, listView, view, position, id);

        if ((position+1) > listView.getHeaderViewsCount()) { /* Not a header. */
            listener.onNoteClick(this, view, position, id, id);

        } else {
            if (mBook != null) {
                listener.onBookPrefaceEditRequest(mBook);
            } else {
                Log.e(TAG, "Book null on preface edit request");
            }
        }
    }

   /*
    * Options menu.
    */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.book_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        /* Remove some menu items if book doesn't exist or it doesn't contain any notes. */

        if (mBook == null || mListAdapter.getCount() == 0) {
            menu.removeItem(R.id.books_options_menu_item_cycle_visibility);
        }

        if (mBook == null) {
            menu.removeItem(R.id.books_options_menu_book_preface);
        }

//        /* Toggle paste item visibility. */
//        item = menu.findItem(R.id.books_options_menu_item_paste);
//        if (item != null && mShelf != null) {
//            item.setVisible(mShelf.haveCutNotes());
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item);

        switch (item.getItemId()) {
            case R.id.books_options_menu_item_cycle_visibility:
                listener.onCycleVisibilityRequest(mBook);
                return true;

            case R.id.books_options_menu_book_preface:
                listener.onBookPrefaceEditRequest(mBook);
                return true;

//            case R.id.books_options_menu_item_paste:
//                mListener.onNotesPasteRequest(mBookId, 0, null);
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

   /*
    * Actions
    */

    private void newNoteRelativeToSelection(Place place) {
        long targetNoteId = getTargetNoteIdFromSelection(place);
        listener.onNoteNewRequest(new NotePlace(mBookId, targetNoteId, place));
    }

    // TODO: Go through main activity and do it in background.
    private void moveNotes(int offset) {
        /* Sanity check. Should not ever happen. */
        if (getSelection().getCount() == 0) {
            Log.e(TAG, "Trying to move notes up while there are no notes selected");
            return;
        }

        listener.onNotesMoveRequest(mBookId, getSelection().getIds().first(), offset);
    }

    /**
     * Paste notes.
     * @param place {@link Place}
     */
    private void pasteNotes(Place place) {
        long noteId = getTargetNoteIdFromSelection(place);

        /* Remove selection. */
        mSelection.clearSelection();

        listener.onNotesPasteRequest(mBookId, noteId, place);
    }

    private void scrollToNoteIfSet() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        long noteId = getArguments().getLong(ARG_NOTE_ID, 0);

        if (noteId > 0) {
            long t = System.currentTimeMillis();

            for (int i = 0; i < getListAdapter().getCount(); i++) {
                long id = getListAdapter().getItemId(i);

                if (id == noteId) {
                    scrollToCursorPosition(i);

                    /* Make sure we don't scroll again (for example after configuration change). */
                    new Handler().postDelayed(() -> getArguments().remove(ARG_NOTE_ID), 500);

                    break;
                }
            }

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG,
                    "Scrolling to note " + noteId +
                            " took " + (System.currentTimeMillis() - t) + "ms");
        }
    }

    /**
     * @param cursorPosition note to scroll to. 0 for first note, 1 for second etc.
     */
    private void scrollToCursorPosition(final int cursorPosition) {
        GesturedListView listView = getListView();
        listView.post(() -> {
            listView.setSelection(cursorPosition + listView.getHeaderViewsCount());
        });
    }

    public Book getBook() {
        return mBook;
    }

    @Override
    public ActionMode.Callback getNewActionMode() {
        return new MyActionMode();
    }

    /*
     * Loading ...
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, bundle);

        switch (id) {
            case Loaders.BOOK_FRAGMENT_BOOK:
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ProviderContract.Books.ContentUri.booksId(mBookId),
                        null,
                        null,
                        null,
                        null
                );

            case Loaders.BOOK_FRAGMENT_NOTES:
                /* Get all non-cut notes for book id. Order by position. */
                return new CursorLoader(
                        getActivity().getApplicationContext(),
                        ProviderContract.Books.ContentUri.booksIdNotes(mBookId),
                        null,
                        null,
                        null,
                        DbNoteView.LFT);

            default:
                throw new IllegalArgumentException("Unknown loader id " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader);

        if (mIsViewCreated) {
            if (loader.getId() == Loaders.BOOK_FRAGMENT_BOOK) {
                Book book = null;
                if (cursor.moveToFirst()) {
                    book = BooksClient.fromCursor(cursor);
                }
                bookLoaded(book);
                mLastBookId = mBookId;

            } else if (loader.getId() == Loaders.BOOK_FRAGMENT_NOTES) {
                notesLoaded(cursor);
            }

            /* Refresh action bar items (hide or display, depending on if the book has been loaded. */
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, cursorLoader);

        if (mIsViewCreated) {
            if (cursorLoader.getId() == Loaders.BOOK_FRAGMENT_NOTES) {
                mListAdapter.changeCursor(null);

            } else if (cursorLoader.getId() == Loaders.BOOK_FRAGMENT_BOOK) {
                /* Nothing holds the cursor. */
            }
        }
    }

    private void bookLoaded(Book book) {
        /* Set the current book.
         * Book can be null. That can happen when this fragment is in back stack and its book
         * gets deleted. When fragment is popped from the back stack it will try to fetch the
         * non-existent book id.
         */
        mBook = book;

        if (mBook != null) {
            updatePreface();
        }

        mViewFlipper.setDisplayedChild(book != null ? 0 : 1);

        announceChangesToActivity();
    }

    private void announceChangesToActivity() {
        if (listener != null) {
            listener.announceChanges(
                    BookFragment.FRAGMENT_TAG,
                    BookUtils.getFragmentTitleForBook(mBook),
                    BookUtils.getFragmentSubtitleForBook(getContext(), mBook),
                    mSelection.getCount());
        }
    }

    /**
     * Update book's preface.
     */
    private void updatePreface() {
        if (isPrefaceDisplayed()) {
            // Add header
            if (getListView().getHeaderViewsCount() == 0) {
                getListView().addHeaderView(mHeader);
            }

            if (getString(R.string.pref_value_preface_in_book_few_lines)
                    .equals(AppPreferences.prefaceDisplay(getContext()))) {
                mPrefaceText.setMaxLines(3);
                mPrefaceText.setEllipsize(TextUtils.TruncateAt.END);

            } else {
                mPrefaceText.setMaxLines(Integer.MAX_VALUE);
                mPrefaceText.setEllipsize(null);
            }

            mPrefaceText.setRawText(mBook.getPreface());

        } else {
            // Remove header
            if (getListView().getHeaderViewsCount() > 0) {
                getListView().removeHeaderView(mHeader);
            }
        }
    }

    private boolean isPrefaceDisplayed() {
        boolean displayPreface = ! getString(R.string.pref_value_preface_in_book_hide)
                .equals(AppPreferences.prefaceDisplay(getContext()));

        return displayPreface && mBook != null && !TextUtils.isEmpty(mBook.getPreface());
    }

    private void notesLoaded(Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, cursor);

        SelectionUtils.removeNonExistingIdsFromSelection(mSelection, cursor);

        mListAdapter.swapCursor(cursor);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "after swap: cursor/adapter count: " + cursor.getCount() + "/" + mListAdapter.getCount());

        /* Display "No notes" text, unless notes or preface are displayed. */
        if (mListAdapter.getCount() > 0 || isPrefaceDisplayed()) {
            mNoNotesText.setVisibility(View.GONE);
        } else {
            mNoNotesText.setVisibility(View.VISIBLE);
        }

        if (mActionModeListener != null) {
            mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());

            ActionMode actionMode = mActionModeListener.getActionMode();
            if (actionMode != null && mActionModeTag != null) {
                actionMode.setTag("M"); // TODO: Ugh.
                actionMode.invalidate();
                mActionModeTag = null;
            }
        }

        /* Scroll to note if note id argument is set. */
        scrollToNoteIfSet();
    }

    @Override
    public Runnable getFabAction() {
        return mBook != null ? () -> {
            if (listener != null) {
                listener.onNoteNewRequest(new NotePlace(mBookId));
            }
        } : null;
    }

    private void delete(final Set<Long> ids) {
        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_notes)
                .setMessage(R.string.delete_notes_and_all_subnotes)
                .setPositiveButton(R.string.delete, (dialog, which) -> listener.onNotesDeleteRequest(mBookId, ids))
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .create();

        dialog.show();
    }

    @Override
    public String getCurrentDrawerItemId() {
        return getDrawerItemId(mBookId);
    }

    public static String getDrawerItemId(long bookId) {
        return TAG + " " + bookId;
    }

    public class MyActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menu);

            /* Inflate a menu resource providing context menu items. */
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.book_cab, menu);

            return true;
        }

        /**
         * Called each time the action mode is shown. Always called after onCreateActionMode,
         * but may be called multiple times if the mode is invalidated.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menu);

            /* Update action mode with number of selected items. */
            actionMode.setTitle(String.valueOf(mSelection.getCount()));

            /* Movement menu. */
            if ("M".equals(actionMode.getTag())) { /* Tag could be null, which is fine here. */
                menu.clear();
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.book_cab_moving, menu);
            }

            /* Hide some items if multiple notes are selected. */
            for (int id: ITEMS_HIDDEN_ON_MULTIPLE_SELECTED_NOTES) {
                MenuItem item = menu.findItem(id);
                if (item != null) {
                    item.setVisible(mSelection.getCount() == 1);
                }
            }

            if (actionMode.getTag() != null && mSelection.getCount() > 1) {
                menu.clear();
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.book_cab, menu);
                actionMode.setTag(null);
            }

            announceChangesToActivity();

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menuItem);

            switch (menuItem.getItemId()) {
                case R.id.book_cab_new_above:
                    newNoteRelativeToSelection(Place.ABOVE);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.book_cab_new_under:
                    newNoteRelativeToSelection(Place.UNDER);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.book_cab_new_below:
                    newNoteRelativeToSelection(Place.BELOW);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.book_cab_move:
                    /* TODO Select all descendants of selected notes. */

                    /* Request different menu for this action mode. */
                    actionMode.setTag("M");
                    actionMode.invalidate();
                    break;

                case R.id.book_cab_schedule:
                case R.id.book_cab_deadline:
                    displayTimestampDialog(menuItem.getItemId(), mSelection.getIds());
                    break;

                case R.id.book_cab_cut:
                case R.id.book_cab_delete_note:
                    /* Get currently selected notes' IDs. */
                    TreeSet<Long> ids = new TreeSet<>(mSelection.getIds());

                    if (menuItem.getItemId() == R.id.book_cab_cut) {
                        listener.onNotesCutRequest(mBookId, ids);
                    } else {
                        delete(ids);
                    }

                    /* Remove selection. */
                    mSelection.clearSelection();

                    actionMode.finish(); /* Close action mode. */

                    break;

//                case R.id.book_cab_undo:
//                    undoCut();
//                    actionMode.finish(); /* Close action mode. */
//                    break;

                case R.id.book_cab_paste_above:
                    pasteNotes(Place.ABOVE);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.book_cab_refile:
                    openNoteRefileDialog(listener, mBookId, mSelection.getIds());
                    break;

                case R.id.book_cab_paste_under:
                    pasteNotes(Place.UNDER);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.book_cab_paste_below:
                    pasteNotes(Place.BELOW);
                    actionMode.finish(); /* Close action mode. */
                    break;

                case R.id.notes_action_move_up:
                    moveNotes(-1);
                    break;

                case R.id.notes_action_move_down:
                    moveNotes(1);
                    break;

                case R.id.notes_action_move_left:
                    listener.onNotesPromoteRequest(mBookId, mSelection.getIds());
                    break;

                case R.id.notes_action_move_right:
                    listener.onNotesDemoteRequest(mBookId, mSelection.getIds());
                    break;

                case R.id.book_cab_state:
                    openNoteStateDialog(listener, mSelection.getIds(), null);
                    break;
            }

            return true; // Handled.
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mSelection.clearSelection();

            /* List adapter could be null, as we could be destroying the action mode of a fragment
             * which is in back stack. That fragment had its onDestroyView called, where list
             * adapter is set to null.
             */
            if (getListAdapter() != null) {
                getListAdapter().notifyDataSetChanged();
            }

            if (mActionModeListener != null) {
                mActionModeListener.actionModeDestroyed();
            }

            announceChangesToActivity();
        }
    }

    public interface BookFragmentListener extends NoteListFragmentListener {
        void onBookPrefaceEditRequest(Book book);

        void onNotesDeleteRequest(long bookId, Set<Long> noteIds);
        void onNotesCutRequest(long bookId, Set<Long> noteIds);
        void onNotesPasteRequest(long bookId, long noteId, Place place);
        void onNotesPromoteRequest(long bookId, Set<Long> noteIds);
        void onNotesDemoteRequest(long bookId, Set<Long> noteIds);
        void onNotesMoveRequest(long bookId, long noteId, int offset);
        void onNotesRefileRequest(long sourceBookId, Set<Long> noteIds, long targetBookId);

        void onCycleVisibilityRequest(Book book);
    }
}

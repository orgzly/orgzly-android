package com.orgzly.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.Filter;
import com.orgzly.android.Note;
import com.orgzly.android.NotesBatch;
import com.orgzly.android.Notifications;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.ContentRepo;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.ui.dialogs.SimpleOneLinerDialog;
import com.orgzly.android.ui.dialogs.WhatsNewDialog;
import com.orgzly.android.ui.fragments.BookFragment;
import com.orgzly.android.ui.fragments.BookPrefaceFragment;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.fragments.DrawerFragment;
import com.orgzly.android.ui.fragments.FilterFragment;
import com.orgzly.android.ui.fragments.FiltersFragment;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.NoteListFragment;
import com.orgzly.android.ui.fragments.SettingsFragment;
import com.orgzly.android.ui.fragments.SyncFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.datetime.OrgDateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends CommonActivity
        implements
        ActionModeListener,
        FilterFragment.FilterFragmentListener,
        FiltersFragment.FiltersFragmentListener,
        BooksFragment.BooksFragmentListener,
        BookFragment.BookFragmentListener,
        NoteFragment.NoteFragmentListener,
        SyncFragment.SyncFragmentListener,
        SimpleOneLinerDialog.SimpleOneLinerDialogListener,
        BookPrefaceFragment.EditorListener,
        NoteListFragment.NoteListFragmentListener,
        SettingsFragment.SettingsFragmentListener,
        DrawerFragment.DrawerFragmentListener {

    public static final String TAG = MainActivity.class.getName();

    public static final int ACTIVITY_REQUEST_CODE_FOR_FILE_CHOOSER = 0;

    private static final String GETTING_STARTED_NOTEBOOK_NAME = "Getting Started with Orgzly";
    private static final int GETTING_STARTED_NOTEBOOK_RESOURCE_ID = R.raw.orgzly_getting_started;

    private static final int DIALOG_NEW_BOOK = 1;
    private static final int DIALOG_IMPORT_BOOK = 5;

    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_QUERY_STRING = "query_string";

    public SyncFragment mSyncFragment;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    /** isDrawerOpen() is not reliable - there was a race condition - closing drawer VS fragment resume. */
    private boolean mIsDrawerOpen = false;


    private CharSequence mSavedTitle;
    private CharSequence mSavedSubtitle;

    /** Original title used when book is not being displayed. */
    private CharSequence mDefaultTitle;

    private DisplayManager mDisplayManager;
    private ActionMode mActionMode;

    // private Dialog mTooltip;
    private AlertDialog mWhatsNewDialog;

    private BroadcastReceiver dbUpgradeStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mWhatsNewDialog != null) {
                mWhatsNewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.running_database_update);
                mWhatsNewDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                mWhatsNewDialog.setCancelable(false);
            }
        }
    };

    private BroadcastReceiver dbUpgradeEndedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mWhatsNewDialog != null) {
                mWhatsNewDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.ok);
                mWhatsNewDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                mWhatsNewDialog.setCancelable(true);
            }
        }
    };

    private Bundle mImportChosenBook = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(dbUpgradeStartedReceiver, new IntentFilter(AppIntent.ACTION_DB_UPGRADE_STARTED));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(dbUpgradeEndedReceiver, new IntentFilter(AppIntent.ACTION_DB_UPGRADE_ENDED));

        /*
         * Set defaults.
         * Attempting to set defaults every time (true parameter below), in case some preference's
         * key has been changed. If false is used, opening Settings fragment can trigger preference
         * change listener, even though nothing has been changed by user.
         */
        android.preference.PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        setContentView(R.layout.activity_main);

        mSavedTitle = mDefaultTitle = getTitle();
        mSavedSubtitle = null;

        setupDrawer();

        setupDisplay(savedInstanceState);

        if (AppPreferences.newNoteNotification(this)) {
            Notifications.createNewNoteNotification(this);
        }
    }

    /**
     * Adds initial set of fragments, depending on intent extras
     */
    private void setupDisplay(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getIntent().getExtras());

        mDisplayManager = new DisplayManager(getSupportFragmentManager());

        if (savedInstanceState == null) { // Not a configuration change.
            long bookId = getIntent().getLongExtra(EXTRA_BOOK_ID, 0L);
            long noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, 0L);
            String queryString = getIntent().getStringExtra(EXTRA_QUERY_STRING);

            mDisplayManager.displayBooks(false);

            /* Display requested book and note. */
            if (bookId > 0) {
                mDisplayManager.displayBook(bookId, noteId);
                if (noteId > 0) {
                    mDisplayManager.displayNote(bookId, noteId);
                }
            } else if (queryString != null) {
                mDisplayManager.displayQuery(queryString);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void setupDrawer() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (mDrawerLayout != null) {
            // Set the drawer toggle as the DrawerListener
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
                private int state = -1;

                /*
                 * onDrawerOpened and onDrawerClosed are not called fast enough.
                 * So state is determined using onDrawerSlide callback and checking the slide offset.
                 */
                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, slideOffset);

                    switch (state) {
                        case -1: // Unknown
                            if (slideOffset == 0) {
                                state = 0;
                                drawerClosed();

                            } else if (slideOffset > 0) {
                                state = 1;
                                drawerOpened();
                            }
                            break;

                        case 0: // Closed
                            if (slideOffset > 0) {
                                state = 1;
                                drawerOpened();
                            }
                            break;

                        case 1: // Opened
                            if (slideOffset == 0) {
                                state = 0;
                                drawerClosed();
                            }
                            break;
                    }
                }
            };

            mDrawerLayout.addDrawerListener(mDrawerToggle);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mSyncFragment = addSyncFragment();

        addDrawerFragment();
    }

    private void drawerOpened() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /* Causes onPrepareOptionsMenu to be called. */
        supportInvalidateOptionsMenu();

        getSupportActionBar().setTitle(mDefaultTitle);
        getSupportActionBar().setSubtitle(null);

        mIsDrawerOpen = true;

        ActivityUtils.closeSoftKeyboard(this);
    }

    private void drawerClosed() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /* Causes onPrepareOptionsMenu to be called. */
        supportInvalidateOptionsMenu();

        getSupportActionBar().setTitle(mSavedTitle);
        getSupportActionBar().setSubtitle(mSavedSubtitle);

        mIsDrawerOpen = false;
    }

    private SyncFragment addSyncFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        SyncFragment fragment = (SyncFragment) getSupportFragmentManager()
                .findFragmentByTag(SyncFragment.FRAGMENT_TAG);

        /* If the Fragment is non-null, then it is currently being
         * retained across a configuration change.
         */
        if (fragment == null) {
            fragment = SyncFragment.getInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.drawer_sync_container, fragment, SyncFragment.FRAGMENT_TAG)
                    .commit();
        }

        return fragment;
    }

    private void addDrawerFragment() {
        if (getSupportFragmentManager().findFragmentByTag(DrawerFragment.FRAGMENT_TAG) == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.drawer_list_container, DrawerFragment.getInstance(), DrawerFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, newConfig);
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        performIntros();
    }

    private void performIntros() {
        int currentVersion = AppPreferences.lastUsedVersionCode(this);
        boolean isNewVersion = checkIfNewAndUpdateVersion();

        if (isNewVersion) {
            /* Import Getting Started notebook. */
            if (! AppPreferences.isGettingStartedNotebookLoaded(this)) {
                importGettingStartedNotebook();
                /* This will be marked as done after book has been loaded in onBookLoaded(). */
            }

            /* Open drawer for the first time user. */
            if (currentVersion == 0 && mDrawerLayout != null) {
                mDrawerLayout.openDrawer(GravityCompat.START);
                mIsDrawerOpen = true;
            }

            onWhatsNewDisplayRequest();
        }
    }

    private void importGettingStartedNotebook() {
        mSyncFragment.loadBook(
                GETTING_STARTED_NOTEBOOK_NAME,
                getResources(),
                GETTING_STARTED_NOTEBOOK_RESOURCE_ID);
    }

    private boolean checkIfNewAndUpdateVersion() {
        boolean isNewVersion = false;


        if (BuildConfig.VERSION_CODE > AppPreferences.lastUsedVersionCode(this)) {
            isNewVersion = true;
        }

        AppPreferences.lastUsedVersionCode(this, BuildConfig.VERSION_CODE);

        return isNewVersion;
    }

    @Override
    protected void onPause() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onPause();

        /* Dismiss What's new dialog. */
        if (mWhatsNewDialog != null) {
            mWhatsNewDialog.dismiss();
            mWhatsNewDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(dbUpgradeStartedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dbUpgradeEndedReceiver);

        if (mDrawerLayout != null && mDrawerToggle != null) {
            mDrawerLayout.removeDrawerListener(mDrawerToggle);
        }
    }

    @Override
    public void onBackPressed() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /* Close drawer if opened. */
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
                mIsDrawerOpen = false;
                return;
            }
        }

        /* Handle back press when editing note - check for changes */
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(NoteFragment.FRAGMENT_TAG);
        if (fragment != null && fragment instanceof NoteFragment && fragment.isVisible()) {
            final NoteFragment noteFragment = (NoteFragment) fragment;
            if (noteFragment.isAskingForConfirmationForModifiedNote()) {
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fragment);

        super.onAttachFragment(fragment);
    }

    @Override
    protected void onResumeFragments() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResumeFragments();

        /* Showing dialog in onResume() fails with:
         *   Can not perform this action after onSaveInstanceState
         */
        if (mImportChosenBook != null) {
            importChosenBook(mImportChosenBook);
            mImportChosenBook = null;
        }
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu);
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_actions, menu);

        setupSearchView(menu);

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                tryDisplayingTooltip();
//            }
//        }, 200);

        return true;
    }

    /**
     * SearchView setup and query text listeners.
     * TODO: http://developer.android.com/training/search/setup.html
     */
    private void setupSearchView(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.activity_action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setQueryHint(getString(R.string.search_hint));

        /* When user starts the search, fill the search box with text depending on current fragment. */
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Make search as wide as possible. */
                ViewGroup.LayoutParams layoutParams = searchView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;

                /* For Query fragment, fill the box with full query. */
                SearchQuery q = mDisplayManager.getDisplayedQuery();
                if (q != null) {
                    searchView.setQuery(q.toString() + " ", false);

                } else {
                    /* If searching from book, add book name to query. */
                    Book book = getActiveFragmentBook();
                    if (book != null) {
                        SearchQuery query = new SearchQuery();
                        query.setBookName(book.getName());
                        searchView.setQuery(query.toString() + " ", false);
                    }
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String str) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String str) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, str);

                /* Close search. */
                MenuItemCompat.collapseActionView(searchItem);

                mDisplayManager.displayQuery(str);

                return true;
            }
        });
    }

    /**
     * Callback for options menu.
     * Called after each invalidateOptionsMenu().
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /* Hide all menu items if drawer is visible. */
        if (mDrawerLayout != null) {
            menuItemsSetVisible(menu, !mDrawerLayout.isDrawerVisible(GravityCompat.START));
        }

        return true;
    }

    private void menuItemsSetVisible(Menu menu, boolean visible) {
        for(int i = 0; i < menu.size(); i++){
            menu.getItem(i).setVisible(visible);
        }
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.activity_action_sync:
                mSyncFragment.onSyncButton();
                return true;

            case R.id.activity_action_settings:
                mDisplayManager.displaySettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            /* Open selected file. */
            case ACTIVITY_REQUEST_CODE_FOR_FILE_CHOOSER:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    Bundle bundle = new Bundle();
                    bundle.putString("uri", uri.toString());

                    /* This will get picked up in onResume(). */
                    mImportChosenBook = bundle;
                }
                break;
        }
    }

    /**
     * Display a dialog for user to enter notebook's name.
     */
    private void importChosenBook(final Bundle bundle) {
        Uri uri = Uri.parse(bundle.getString("uri"));
        String guessedBookName = guessBookNameFromUri(uri);

        SimpleOneLinerDialog
                .getInstance(DIALOG_IMPORT_BOOK, R.string.import_as, R.string.name, R.string.import_, R.string.cancel, guessedBookName, bundle)
                .show(getSupportFragmentManager(), SimpleOneLinerDialog.FRAGMENT_TAG);
    }

    /**
     * @return Guessed book name or {@code null} if it couldn't be guessed
     */
    private String guessBookNameFromUri(Uri uri) {
        String fileName = BookName.getFileName(this, uri);

        if (BookName.isSupportedFormatFileName(fileName)) {
            BookName bookName = BookName.fromFileName(fileName);
            return bookName.getName();
        } else {
            return null;
        }
    }

    /**
     * Import notebook from URI saving it under specified name.
     */
    private void importBookFromUri(Uri uri, String bookName, BookName.Format format) {
        /* Check if book name already exists in database. */
        if (BooksClient.doesExist(this, bookName)) {
            showSimpleSnackbarLong(getString(R.string.book_name_already_exists, bookName));
            return;
        }

        mSyncFragment.importBookFromUri(bookName, format, uri);
    }

    /**
     * Note has been clicked in list view.
     *
     * @param fragment Fragment from which the action came.
     * @param view view
     * @param position note position in list
     * @param noteId note ID
     */
    @Override
    public void onNoteClick(NoteListFragment fragment, View view, int position, long noteId) {
        if (AppPreferences.isReverseNoteClickAction(this)) {
            toggleNoteSelection(fragment, view, noteId);

        } else {
            /* If there are already selected note, toggle the selection of this one.
             * If there are no notes selected, open note
             */
            if (fragment.getSelection().getCount() > 0) {
                toggleNoteSelection(fragment, view, noteId);
            } else {
                openNote(fragment.getFragmentTag(), noteId);
            }
        }
    }

    /**
     * Note has been long-clicked in list view.
     *
     * @param fragment Fragment from which the action came.
     * @param view view
     * @param position note position in list
     * @param noteId note ID
     */
    @Override
    public void onNoteLongClick(NoteListFragment fragment, View view, int position, long noteId) {
        if (AppPreferences.isReverseNoteClickAction(this)) {
            openNote(fragment.getFragmentTag(), noteId);
        } else {
            toggleNoteSelection(fragment, view, noteId);
        }
    }

    private void openNote(String fragmentTag, long noteId) {
        finishActionMode();

        /* Get book ID from note ID. */
        // TODO: Avoid using Shelf from activity directly
        Shelf shelf = new Shelf(this);
        Note note = shelf.getNote(noteId);
        long bookId = note.getPosition().getBookId();

        mDisplayManager.displayNote(bookId, noteId);
    }

    @Override
    public void onNoteScrollToRequest(long noteId) {
        /* Get book ID from note ID. */
        // TODO: Avoid using Shelf from activity directly
        Shelf shelf = new Shelf(this);
        Note note = shelf.getNote(noteId);
        long bookId = note.getPosition().getBookId();

        mSyncFragment.sparseTree(bookId, noteId);
        mDisplayManager.displayBook(bookId, noteId);
    }

    /**
     * Toggle selection of a note (or notes, if selected note is folded).
     */
    private void toggleNoteSelection(NoteListFragment fragment, View view, long noteId) {
        Selection selection = fragment.getSelection();

        selection.toggle(view, noteId);

        updateActionModeForSelection(fragment.getSelection(), fragment.getNewActionMode());
    }

    /* Open note fragment to create a new note. */
    @Override
    public void onNoteNewRequest(NotePlace target) {
        finishActionMode();

        mDisplayManager.displayNewNote(target);
    }

    /* Save note. */
    @Override
    public void onNoteCreateRequest(Note note, NotePlace notePlace) {
        finishActionMode();

        popBackStackAndCloseKeyboard();

        mSyncFragment.createNote(note, notePlace);
    }

    @Override
    public void onNoteCreated(final Note note) {
        /*
         * Display Snackbar with an action - create new note below just created one.
         */
        View view = findViewById(R.id.main_content);
        if (view != null) {
            showSnackbar(Snackbar
                    .make(view, R.string.message_note_created, MiscUtils.SNACKBAR_WITH_ACTION_DURATION)
                    .setAction(R.string.new_below, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            NotePlace notePlace = new NotePlace(
                                    note.getPosition().getBookId(),
                                    note.getId(),
                                    Place.BELOW);

                            mDisplayManager.displayNewNote(notePlace);
                        }
                    }));
        }


        /* Animate updated note. */
//        Set<Long> set = new HashSet<>();
//        set.add(note.getId());
//        animateNotesAfterEdit(set);
    }

    @Override
    public void onNoteCreatingFailed() {
        showSimpleSnackbarLong(R.string.message_failed_creating_note);
    }

    @Override
    public void onNoteUpdateRequest(Note note) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.updateNote(note);
    }

    @Override
    public void onNoteUpdated(Note note) {
    }

    @Override
    public void onNoteUpdatingFailed(Note note) {
        showSimpleSnackbarLong(R.string.message_failed_updating_note);
    }

    @Override
    public void onNoteCancelRequest(Note note) {
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onNoteDeleteRequest(Note note) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.deleteNotes(note.getPosition().getBookId(), note.getId());
    }

    @Override
    public void onStateChangeRequest(Set<Long> noteIds, String state) {
        mSyncFragment.updateNoteState(noteIds, state);
    }

    @Override
    public void onStateCycleRequest(long id, int direction) {
        mSyncFragment.shiftNoteState(id, direction);
    }

    @Override
    public void onStateToDoneRequest(long noteId) {
        mSyncFragment.setStateToDone(noteId);
    }

    @Override
    public void onStateChanged(Set<Long> noteIds, String state) {
        // animateNotesAfterEdit(noteIds);
    }

    @Override
    public void onScheduledTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time) {
        mSyncFragment.updateScheduledTime(noteIds, time);
    }

    @Override
    public void onCycleVisibilityRequest(Book book) {
        mSyncFragment.cycleVisibility(book);
    }

    @Override
    public void onScheduledTimeUpdated(Set<Long> noteIds, OrgDateTime time) {
        // animateNotesAfterEdit(noteIds);
    }

    @Override /* BookFragment */
    public void onBookPrefaceEditRequest(Book book) {
        finishActionMode();

        mDisplayManager.displayEditor(book);
    }

    @Override
    public void onNotesDeleteRequest(final long bookId, final TreeSet<Long> noteIds) {
        mSyncFragment.deleteNotes(bookId, noteIds);
    }

    @Override
    public void onNotesDeleted(int count) {
        String message;

        if (count == 0) {
            message = getResources().getString(R.string.no_notes_deleted);
        } else {
            message = getResources().getQuantityString(R.plurals.notes_deleted, count, count);
        }

        showSimpleSnackbarLong(message);
    }

    @Override
    public void onNotesCutRequest(long bookId, TreeSet<Long> noteIds) {
        mSyncFragment.cutNotes(bookId, noteIds);
    }

    @Override
    public void onNotesCut(int count) {
        String message;

        if (count == 0) {
            message = getResources().getString(R.string.no_notes_cut);
        } else {
            message = getResources().getQuantityString(R.plurals.notes_cut, count, count);
        }

        showSimpleSnackbarLong(message);
    }

    @Override
    public void onStateKeywordsPreferenceChanged() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.todo_keywords_configuration_changed_dialog_title)
                .setMessage(R.string.todo_keywords_configuration_changed_dialog_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSyncFragment.reParseNotes();
                    }
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    @Override
    public void onCreatedKeywordPreferenceChanged() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.created_keyword_configuration_changed_dialog_title)
                .setMessage(R.string.todo_keywords_configuration_changed_dialog_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSyncFragment.reParseNotes();
                    }
                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    /**
     * Ask user to confirm, then delete book.
     */
    @Override
    public void onBookDeleteRequest(final long bookId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Get name for the book " + bookId + "...");

        final Book book = BooksClient.get(this, bookId);

        View view = View.inflate(this, R.layout.dialog_book_delete, null);
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        boolean deleteLinked = checkBox.isChecked();

                        /* Delete book. */
                        mSyncFragment.deleteBook(book, deleteLinked);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this)
                        .setTitle("Delete " + MiscUtils.quotedString(book.getName()))
                        .setMessage("Delete this notebook?")
                        .setPositiveButton(R.string.ok, dialogClickListener)
                        .setNegativeButton(R.string.cancel, dialogClickListener);

        if (book.getLink() != null) {
            builder.setView(view);
        }

        builder.show();

    }

    @Override
    public void onBookRenameRequest(final long bookId) {
        final Book book = BooksClient.get(this, bookId);

        if (book == null) {
            return;
        }

        final View dialogView = View.inflate(this, R.layout.dialog_book_rename, null);
        final TextInputLayout nameInputLayout = (TextInputLayout) dialogView.findViewById(R.id.name_input_layout);
        final EditText name = (EditText) dialogView.findViewById(R.id.name);

        Uri originalLinkUri = book.getLink() != null ? book.getLink().getUri() : null;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        doRenameBook(book, name.getText().toString(), nameInputLayout);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.rename_book, MiscUtils.quotedString(book.getName())))
                        .setPositiveButton(R.string.rename, dialogClickListener)
                        .setNegativeButton(R.string.cancel, dialogClickListener)
                        .setView(dialogView);

        name.setText(book.getName());

        final AlertDialog dialog = builder.create();

        /* Finish on keyboard action press. */
        name.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });


        final Activity activity = this;

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ActivityUtils.openSoftKeyboard(activity, name);
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActivityUtils.closeSoftKeyboard(activity);
            }
        });

        if (originalLinkUri != null) {
            name.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable str) {
                    /* Disable the button is nothing is entered. */
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(str));
                }
            });
        }

        dialog.show();
    }

    private void doRenameBook(Book book, String name, TextInputLayout inputLayout) {
        if (! TextUtils.isEmpty(name)) {
            inputLayout.setError(null);
            mSyncFragment.renameBook(book, name);

        } else {
            inputLayout.setError(getString(R.string.can_not_be_empty));
        }
    }

    @Override
    public void onBookLinkSetRequest(final long bookId) {
        final Book book = BooksClient.get(this, bookId);

        Map<String, Repo> repos = ReposClient.getAll(this);

        if (repos.size() == 0) {
            showSnackbarWithReposLink(getString(R.string.no_repos));
            return;
        }

        LinkedHashMap<String, Integer> items = new LinkedHashMap<>();
        int itemIndex = 0;

        /* Add "no link" item. */
        items.put(getString(R.string.no_link), itemIndex++);

        /* Add repositories.
         * FIXME: Skipping ContentRepo for now, as we can't construct Uri for a non-existent document.
         * Repo might need changing to be repo uri + path
         */
        for (String repoUri: repos.keySet()) {
            Repo repo = repos.get(repoUri);
            if (! (repo instanceof ContentRepo)) {
                items.put(repoUri, itemIndex++);
            }
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_spinner, null, false);
        final Spinner spinner = (Spinner) view.findViewById(R.id.dialog_spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(), R.layout.spinner_item, new ArrayList<>(items.keySet()));
        adapter.setDropDownViewResource(R.layout.dropdown_item);
        spinner.setAdapter(adapter);

        /* Set spinner to current book's link. */
        if (book.getLink() != null) {
            Integer pos = items.get(book.getLink().getRepoUri().toString());
            if (pos != null) {
                spinner.setSelection(pos);
            }
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Shelf shelf = new Shelf(MainActivity.this);
                        String repoUrl = (String) spinner.getSelectedItem();

                        if (getString(R.string.no_link).equals(repoUrl)) {
                            shelf.setLink(book, null);
                        } else {
                            shelf.setLink(book, repoUrl);
                        }

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("Link " + MiscUtils.quotedString(book.getName()) + " to repository")
                .setView(view)
                .setPositiveButton(R.string.set, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show();
    }

    @Override
    public void onForceSaveRequest(long bookId) {
        mSyncFragment.forceSaveBook(bookId);
    }

    @Override
    public void onBookSaved(Book book) {
    }

    @Override
    public void onBookForceSavingFailed(Exception exception) {
    }

    @Override
    public void onForceLoadRequest(long bookId) {
        mSyncFragment.loadBook(bookId);
    }

    /**
     * Callback from {@link com.orgzly.android.ui.fragments.BooksFragment}.
     */
    @Override
    public void onBookExportRequest(final long bookId) {
        actionAfterPermissionGrant = new Runnable() {
            @Override
            public void run() {
                mSyncFragment.exportBook(bookId);
            }
        };

        /* Check for permissions. */
        boolean isGranted = AppPermissions.isGrantedOrRequest(this, AppPermissions.FOR_BOOK_EXPORT);

        if (isGranted) {
            actionAfterPermissionGrant.run();
            actionAfterPermissionGrant = null;
        }
    }

    @Override
    public void onBookLoadRequest() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        /* Allow choosing multiple files. */
        // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(
                Intent.createChooser(intent, getString(R.string.import_org_file)),
                ACTIVITY_REQUEST_CODE_FOR_FILE_CHOOSER);
    }

    @Override
    public void onGettingStartedNotebookReloadRequest() {
        importGettingStartedNotebook();
    }

    @Override
    public void onWhatsNewDisplayRequest() {
        if (mWhatsNewDialog != null) {
            mWhatsNewDialog.dismiss();
        }

        mWhatsNewDialog = WhatsNewDialog.create(this);

        mWhatsNewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mWhatsNewDialog = null;
            }
        });

        mWhatsNewDialog.show();
    }

    /**
     * Wipe database, after prompting user to confirm.
     */
    @Override
    public void onDatabaseClearRequest() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        mDisplayManager.reset();
                        mSyncFragment.clearDatabase();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        new AlertDialog.Builder(this)
                .setTitle("Database")
                .setMessage(R.string.clear_database_dialog_message)
                .setPositiveButton(R.string.ok, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show();
    }

//    @Override
//    public void onRepoSettingsRequest() {
//        mDisplayManager.reposSettingsRequest();
//    }

    /**
     * Prompt user for book name and then create it.
     */
    @Override
    public void onBookCreateRequest() {
        SimpleOneLinerDialog
                .getInstance(DIALOG_NEW_BOOK, R.string.new_notebook, R.string.name, R.string.create, R.string.cancel, null, null)
                .show(getSupportFragmentManager(), SimpleOneLinerDialog.FRAGMENT_TAG);
    }

    @Override /* SyncFragment */
    public void onBookCreated(Book book) {
    }

    @Override /* SyncFragment */
    public void onBookCreationFailed(Exception exception) {
        showSimpleSnackbarLong(exception.getMessage());
    }

    @Override /* SyncFragment */
    public void onBookLoaded(Book book) {
        /* If it's Getting Started notebook, mark that it has been loaded in preferences,
         * so we don't try to load it again.
         */
        if (book.getName().equals(GETTING_STARTED_NOTEBOOK_NAME)) {
            /* If notebook was already previously loaded, user probably requested reload.
             * Display a message in that case.
             */
            if (AppPreferences.isGettingStartedNotebookLoaded(this)) {
                showSimpleSnackbarLong(R.string.getting_started_loaded);
            } else {
                AppPreferences.isGettingStartedNotebookLoaded(this, true);
            }
        }
    }

    @Override
    public void onBookLoadFailed(Exception exception) {
    }

    /**
     * Sync finished.
     *
     * Display snackbar with a message.  If it makes sense also set action to open a repository..
     *
     * @param msg Error message if syncing failed, null if it was successful
     */
    @Override
    public void onSyncFinished(String msg) {
        if (msg != null) {
            showSnackbarWithReposLink(getString(R.string.syncing_failed, msg));
        }
    }

    @Override /* SyncFragment */
    public void onBookExported(File file) {
        showSimpleSnackbarLong(getString(R.string.book_exported, file.getAbsolutePath()));
    }

    /**
     * Display snackbar and include link to repositories.
     */
    private void showSnackbarWithReposLink(String msg) {
        View view = findViewById(R.id.main_content);

        if (view != null) {
            showSnackbar(Snackbar.make(view, msg, MiscUtils.SNACKBAR_WITH_ACTION_DURATION)
                    .setAction(R.string.repositories, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setClass(MainActivity.this, ReposActivity.class);
                            startActivity(intent);
                        }
                    }));
        }
    }

    @Override /* SyncFragment */
    public void onBookExportFailed(Exception e) {
        showSimpleSnackbarLong(getString(R.string.failed_exporting_book, e.getLocalizedMessage()));
    }

    @Override
    public void onNotesPasteRequest(long bookId, long noteId, Place place) {
        mSyncFragment.pasteNotes(bookId, noteId, place);
    }

    @Override
    public void onNotesPromoteRequest(long bookId, Set<Long> noteIds) {
        mSyncFragment.promoteNotes(bookId, noteIds);
    }

    @Override
    public void onNotesDemoteRequest(long bookId, Set<Long> noteIds) {
        mSyncFragment.demoteNotes(bookId, noteIds);
    }

    @Override
    public void onNotesPasted(NotesBatch batch) {
        String message = getResources().getQuantityString(
                R.plurals.notes_pasted,
                batch.getCount(),
                batch.getCount());

        showSimpleSnackbarLong(message);
    }

    @Override
    public void onNotesNotPasted() {
        showSimpleSnackbarLong(getResources().getString(R.string.no_notes_pasted));
    }

    @Override /* SyncFragment */
    public void onBookDeleted(Book book) {
        showSimpleSnackbarLong(R.string.message_book_deleted);
    }

    @Override
    public void onBookDeletingFailed(Book book, IOException exception) {
        String message = getResources().getString(
                R.string.message_deleting_book_failed,
                exception.toString());

        showSimpleSnackbarLong(message);
    }

    @Override /* SyncFragment */
    public void onDatabaseCleared() {
    }

    @Override
    public void onBookClicked(long bookId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId);

        mDisplayManager.displayBook(bookId, 0);
    }

//    private void animateNotesAfterEdit(Set<Long> noteIds) {
//        Fragment f;
//
//        f = getSupportFragmentManager().findFragmentByTag(BookFragment.FRAGMENT_TAG);
//        if (f != null && f.isVisible()) {
//            BookFragment heads = (BookFragment) f;
//            heads.animateNotes(noteIds, HeadAnimator.ANIMATE_FOR_HEAD_MODIFIED);
//        }
//
//        f = getSupportFragmentManager().findFragmentByTag(QueryFragment.FRAGMENT_TAG);
//        if (f != null && f.isVisible()) {
//            QueryFragment heads = (QueryFragment) f;
//            heads.animateNotes(noteIds, HeadAnimator.ANIMATE_FOR_HEAD_MODIFIED);
//        }
//    }

    @Override /* EditorFragment */
    public void onBookPrefaceEditSaveRequest(Book book) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.updateBookSettings(book);
    }

    @Override
    public void onBookPrefaceEditCancelRequest() {
        popBackStackAndCloseKeyboard();
    }

    // TODO: Implement handlers when dialog is created
    @Override
    public void onSimpleOneLinerDialogValue(int id, String value, Bundle userData) {
        switch (id) {
            case DIALOG_NEW_BOOK:
                mSyncFragment.createNewBook(value);
                break;

            case DIALOG_IMPORT_BOOK:
                /* We are assuming it's an Org file. */
                Uri uri = Uri.parse(userData.getString("uri"));
                importBookFromUri(uri, value, BookName.Format.ORG);

                break;
        }
    }

    private Book getActiveFragmentBook() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(BookFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            BookFragment bookFragment = (BookFragment) f;
            return bookFragment.getBook();
        }

        return null;
    }

    /*
     * Action mode
     */

    @Override
    public void updateActionModeForSelection(Selection selection, ActionMode.Callback actionMode) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, selection, actionMode);

        if (mActionMode != null) { /* Action menu is already activated. */
            /* Finish action mode if there are no more selected items. */
            if (selection.getCount() == 0) {
                mActionMode.finish();
            } else {
                mActionMode.invalidate();
            }

        } else { /* No action menu activated - started it. */
            if (selection.getCount() > 0) {
                /* Start new action mode. */
                mActionMode = startSupportActionMode(actionMode);

                /* onPrepareActionMode is not being called (any more?) when action mode
                 * is first created. This causes title to be left empty. This forces
                 * onPrepareActionMode to be called and title updated.
                 *
                 * TODO: Update: Might not be the case any more
                 * We now get two calls to onPrepareActionMode when action mode is first created.
                 */
                mActionMode.invalidate();
            }
        }
    }

    @Override
    public ActionMode getActionMode() {
        return mActionMode;
    }

    @Override
    public void actionModeDestroyed() {
        mActionMode = null;
    }

    private void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /**
     * Updates colors, FAB, title and subtitle, all depending on displayed fragment.
     */
    @Override
    public void announceChanges(String fragmentTag, CharSequence title, CharSequence subTitle, int selectionCount) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fragmentTag, title, subTitle, selectionCount);

        /* Save titles so they can be restored after drawer open/close. */
        mSavedTitle = title != null ? title : mDefaultTitle;
        mSavedSubtitle = subTitle;

        /* Do not set titles if drawer is opened.
         * This check is only needed for when drawer is opened for the first time
         * programmatically, before fragment got to its onResume().
         */
        if (! mIsDrawerOpen) {
            /* Change titles. */
            getSupportActionBar().setTitle(mSavedTitle);
            getSupportActionBar().setSubtitle(mSavedSubtitle);
        }

        /* Set status and action bar colors depending on the fragment. */
        ActivityUtils.setColorsForFragment(this, fragmentTag);

        /* Notify drawer about currently active fragment. */
        Fragment drawerFragment = getSupportFragmentManager()
                .findFragmentByTag(DrawerFragment.FRAGMENT_TAG);
        if (drawerFragment != null) {
            ((DrawerFragment) drawerFragment).setActiveFragment(fragmentTag);
        }

        /* Update floating action button. */
        MainFab.updateFab(this, fragmentTag, selectionCount);
    }

    @Override
    public void onFilterNewRequest() {
        mDisplayManager.onFilterNewRequest();
    }

    @Override
    public void onFilterDeleteRequest(Set<Long> ids) {
        mSyncFragment.deleteFilters(ids);
    }

    @Override
    public void onFilterEditRequest(long id) {
        mDisplayManager.onFilterEditRequest(id);
    }

    @Override
    public void onFilterMoveUpRequest(long id) {
        mSyncFragment.moveFilterUp(id);
    }

    @Override
    public void onFilterMoveDownRequest(long id) {
        mSyncFragment.moveFilterDown(id);
    }

    @Override
    public void onFilterCreateRequest(Filter filter) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.createFilter(filter);
    }

    @Override
    public void onFilterUpdateRequest(long id, Filter filter) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.updateFilter(id, filter);
    }

    @Override
    public void onFilterCancelRequest() {
        popBackStackAndCloseKeyboard();
    }

//    private void tryDisplayingTooltip() {
//        if (mTooltip == null) {
//            mTooltip = Tooltips.display(this, mIsDrawerOpen);
//
//            if (mTooltip != null) {
//                mTooltip.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        mTooltip = null;
//                    }
//                });
//            }
//        }
//    }

    @Override
    public void onDrawerItemClicked(final DrawerFragment.DrawerItem item) {
        /* Don't end action mode if the click was on book - it could be the same book. */
        if (! (item instanceof DrawerFragment.BookItem)) {
            finishActionMode();
        }

        /* Close drawer. */
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }

        runDelayedAfterDrawerClose(new Runnable() {
            @Override
            public void run() {
                if (item instanceof DrawerFragment.BooksItem) {
                    mDisplayManager.displayBooks(true);

                } else if (item instanceof DrawerFragment.FiltersItem) {
                    mDisplayManager.displayFilters();

                } else if (item instanceof DrawerFragment.SettingsItem) {
                    mDisplayManager.displaySettings();

                } else if (item instanceof DrawerFragment.BookItem) {
                    long bookId = ((DrawerFragment.BookItem) item).id;
                    mDisplayManager.displayBook(bookId, 0);

                } else if (item instanceof DrawerFragment.FilterItem) {
                    String query = ((DrawerFragment.FilterItem) item).query;
                    mDisplayManager.displayQuery(query);
                }
            }
        });
    }

    /* Avoid jerky drawer close by displaying new fragment with a delay.
     * Previous title might be displayed if loading of the new fragment takes too long and it
     * might look ugly, showing for only a fraction of a second before being replaced with new one.
     * FIXME: Maybe move drawer handling here and add a flag for *not* changing the title.
     */
    private void runDelayedAfterDrawerClose(Runnable runnable) {
        new Handler().postDelayed(runnable, 300);
    }
}

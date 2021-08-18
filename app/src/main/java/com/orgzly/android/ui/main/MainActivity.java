package com.orgzly.android.ui.main;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.BookFormat;
import com.orgzly.android.BookName;
import com.orgzly.android.db.NotesClipboard;
import com.orgzly.android.db.dao.NoteDao;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.Note;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.query.Condition;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.user.DottedQueryBuilder;
import com.orgzly.android.sync.AutoSync;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.BottomActionBar;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.DisplayManager;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.books.BooksFragment;
import com.orgzly.android.ui.dialogs.SimpleOneLinerDialog;
import com.orgzly.android.ui.drawer.DrawerNavigationView;
import com.orgzly.android.ui.note.NoteFragment;
import com.orgzly.android.ui.notes.book.BookFragment;
import com.orgzly.android.ui.notes.book.BookPrefaceFragment;
import com.orgzly.android.ui.notifications.Notifications;
import com.orgzly.android.ui.repos.ReposActivity;
import com.orgzly.android.ui.savedsearch.SavedSearchFragment;
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment;
import com.orgzly.android.ui.settings.SettingsActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.usecase.BookCreate;
import com.orgzly.android.usecase.BookExport;
import com.orgzly.android.usecase.BookExportToUri;
import com.orgzly.android.usecase.BookForceLoad;
import com.orgzly.android.usecase.BookForceSave;
import com.orgzly.android.usecase.BookImportFromUri;
import com.orgzly.android.usecase.BookImportGettingStarted;
import com.orgzly.android.usecase.BookLinkUpdate;
import com.orgzly.android.usecase.BookSparseTreeForNote;
import com.orgzly.android.usecase.BookUpdatePreface;
import com.orgzly.android.usecase.NoteCopy;
import com.orgzly.android.usecase.NoteCut;
import com.orgzly.android.usecase.NoteDelete;
import com.orgzly.android.usecase.NoteDemote;
import com.orgzly.android.usecase.NoteFindWithProperty;
import com.orgzly.android.usecase.NoteMove;
import com.orgzly.android.usecase.NotePaste;
import com.orgzly.android.usecase.NotePromote;
import com.orgzly.android.usecase.NoteUpdateDeadlineTime;
import com.orgzly.android.usecase.NoteUpdateScheduledTime;
import com.orgzly.android.usecase.NoteUpdateState;
import com.orgzly.android.usecase.NoteUpdateStateToggle;
import com.orgzly.android.usecase.NoteUpdateClockingState;
import com.orgzly.android.usecase.SavedSearchCreate;
import com.orgzly.android.usecase.SavedSearchDelete;
import com.orgzly.android.usecase.SavedSearchExport;
import com.orgzly.android.usecase.SavedSearchMoveDown;
import com.orgzly.android.usecase.SavedSearchMoveUp;
import com.orgzly.android.usecase.SavedSearchUpdate;
import com.orgzly.android.usecase.UseCase;
import com.orgzly.android.usecase.UseCaseResult;
import com.orgzly.android.usecase.UseCaseRunner;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.org.datetime.OrgDateTime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

public class MainActivity extends CommonActivity
        implements
        ActionModeListener,
        SavedSearchFragment.Listener,
        SavedSearchesFragment.Listener,
        BooksFragment.Listener,
        BookFragment.Listener,
        NoteFragment.Listener,
        SyncFragment.Listener,
        SimpleOneLinerDialog.Listener,
        BookPrefaceFragment.Listener {

    public static final String TAG = MainActivity.class.getName();

    private static final int DIALOG_NEW_BOOK = 1;
    private static final int DIALOG_IMPORT_BOOK = 2;

    // TODO: Stop using SyncFragment, use ViewModel
    public SyncFragment mSyncFragment;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    protected DrawerNavigationView drawerNavigationView;

    private LocalBroadcastManager broadcastManager;

    private ActionMode mActionMode;
    private boolean mPromoteDemoteOrMoveRequested = false;

    private Runnable runnableOnResumeFragments;

    private BroadcastReceiver receiver = new LocalBroadcastReceiver();

    private AlertDialog dialog;

    private SharedMainActivityViewModel sharedMainActivityViewModel;

    private MainActivityViewModel viewModel;

    private ActivityForResult activityForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.appComponent.inject(this);

        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setContentView(R.layout.activity_main);

        sharedMainActivityViewModel = new ViewModelProvider(this)
                .get(SharedMainActivityViewModel.class);

        ViewModelProvider.Factory factory =
                MainActivityViewModelFactory.Companion.getInstance(dataRepository);

        viewModel = new ViewModelProvider(this, factory).get(MainActivityViewModel.class);

        setupActionBar();

        broadcastManager = LocalBroadcastManager.getInstance(this);

        setupDrawer();

        setupViewModel();

        setupDisplay(savedInstanceState);

        if (AppPreferences.newNoteNotification(this)) {
            Notifications.createNewNoteNotification(this);
        }

        activityForResult = new ActivityForResult(this) {
            @Override
            public void onBookExport(@NotNull Uri uri, long bookId) {
                runnableOnResumeFragments = () ->
                        mSyncFragment.run(new BookExportToUri(uri, bookId) {
                            @NotNull
                            @Override
                            public OutputStream getStream(@NotNull Uri uri) throws IOException {
                                return getOutputStream(uri);
                            }
                        });
            }

            @Override
            public void onBookImport(@NotNull Uri uri) {
                runnableOnResumeFragments = () ->
                        importChosenBook(uri);
            }

            @Override
            public void onSearchQueriesImport(@NotNull Uri uri) {
                viewModel.importSavedSearches(uri);
            }

            @Override
            public void onSearchQueriesExport(@NotNull Uri uri) {
                viewModel.exportSavedSearches(uri);
            }
        };
    }

    @NotNull
    private OutputStream getOutputStream(@NotNull Uri uri) throws IOException {
        OutputStream stream = getContentResolver().openOutputStream(uri);

        if (stream == null) {
            throw new IOException("Failed opening output stream for " + uri);
        }

        return stream;
    }

    /**
     * Adds initial set of fragments, depending on intent extras
     */
    private void setupDisplay(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getIntent().getExtras());

        if (savedInstanceState == null) { // Not a configuration change.
            long bookId = getIntent().getLongExtra(AppIntent.EXTRA_BOOK_ID, 0L);
            long noteId = getIntent().getLongExtra(AppIntent.EXTRA_NOTE_ID, 0L);
            String queryString = getIntent().getStringExtra(AppIntent.EXTRA_QUERY_STRING);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId, queryString);

            DisplayManager.displayBooks(getSupportFragmentManager(), false);

            /* Display requested book and note. */
            if (bookId > 0) {
                DisplayManager.displayBook(getSupportFragmentManager(), bookId, noteId);
                if (noteId > 0) {
                    DisplayManager.displayExistingNote(getSupportFragmentManager(), bookId, noteId);
                }
            } else if (queryString != null) {
                DisplayManager.displayQuery(getSupportFragmentManager(), queryString);
            }
        }
    }

    private void setupDrawer() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = mDrawerLayout.findViewById(R.id.drawer_navigation_view);

        navigationView.setNavigationItemSelectedListener(item -> {
            Intent intent = item.getIntent();

            if (intent != null) {
                mDrawerLayout.closeDrawer(GravityCompat.START);

                // Avoid jerky drawer close by displaying new fragment with a delay
                new Handler().postDelayed(() -> broadcastManager.sendBroadcast(intent), 200);
            }

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item, intent);

            return true;
        });

        drawerNavigationView = new DrawerNavigationView(this, viewModel, navigationView);

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

                        case 0: // Starting to open the drawer
                            if (slideOffset > 0) {
                                state = 1;
                                drawerOpened();
                            }
                            break;

                        case 1: // Starting to close the drawer
                            if (slideOffset == 0) {
                                state = 0;
                                drawerClosed();
                            }
                            break;
                    }
                }
            };

            // No flipping burgers
            mDrawerToggle.setDrawerSlideAnimationEnabled(false);

            mDrawerLayout.addDrawerListener(mDrawerToggle);
        }

        mSyncFragment = addSyncFragment();
    }

    private void setupViewModel() {
        sharedMainActivityViewModel.getDrawerLockState().observe(this, isLocked -> {
            DrawerLayout layout = mDrawerLayout;
            if (layout != null) {
                if (isLocked == null || !isLocked) {
                    layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                } else {
                    layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
            }
        });

        sharedMainActivityViewModel.getFragmentState().observe(this, state -> {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed fragment state: " + state);

            if (state != null) {
                getSupportActionBar().setTitle(state.getTitle());

                // Clean up whitespace for multi-line query
                CharSequence subTitle = state.getSubTitle();
                if (subTitle != null) {
                    subTitle = subTitle.toString().replaceAll("\\s{2,}", " ");
                }

                getSupportActionBar().setSubtitle(subTitle);

                drawerNavigationView.updateActiveFragment(state.getTag());

                /* Update floating action button. */
                MainFab.updateFab(this, state.getTag(), state.getSelectionCount());
            }
        });

        viewModel.getOpenNoteWithPropertyRequestEvent().observeSingle(this, pair -> {
            if (pair != null) {
                UseCase action = pair.getFirst();
                UseCaseResult result = pair.getSecond();

                if (action instanceof NoteFindWithProperty) {
                    NoteFindWithProperty thisAction = (NoteFindWithProperty) action;

                    if (result.getUserData() != null) {
                        NoteDao.NoteIdBookId note = (NoteDao.NoteIdBookId) result.getUserData();
                        DisplayManager.displayExistingNote(
                                getSupportFragmentManager(), note.getBookId(), note.getNoteId());

                    } else {
                        showSnackbar(getString(
                                R.string.no_such_link_target,
                                thisAction.getName(),
                                thisAction.getValue()));
                    }
                }
            }
        });

        viewModel.getOpenFileLinkRequestEvent().observeSingle(this, result -> {
            if (result != null && result.getUserData() != null) {
                Object userData = result.getUserData();

                if (userData instanceof Book) {
                    Book book = (Book) userData;
                    Intent intent = new Intent(AppIntent.ACTION_OPEN_BOOK);
                    intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.getId());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "sending intent", intent);

                } else if (userData instanceof File) {
                    File file = (File) userData;
                    openFileIfExists(file);
                }
            }
        });

        viewModel.getOpenNoteRequestEvent().observeSingle(this, note ->
                MainActivity.openSpecificNote(note.getPosition().getBookId(), note.getId()));

        viewModel.getSetBookLinkRequestEvent().observeSingle(this, result -> {
            Book book = result.getBook();
            List<Repo> links = result.getLinks();
            CharSequence[] urls = result.getUrls();
            int checked = result.getSelected();

            if (links.isEmpty()) {
                showSnackbarWithReposLink(getString(R.string.no_repos));

            } else {
                ArrayAdapter<Repo> adapter = new ArrayAdapter<>(
                        this, R.layout.item_repo, R.id.item_repo_url);
                adapter.addAll(links);



                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.book_link)
                        .setSingleChoiceItems(
                                urls, checked, (d, which) -> {
                                    mSyncFragment.run(new BookLinkUpdate(book.getId(), links.get(which)));
                                    dialog.dismiss();
                                    dialog = null;
                                })

                        .setNeutralButton(R.string.remove_notebook_link, (dialog, which) -> {
                            mSyncFragment.run(new BookLinkUpdate(book.getId()));
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        viewModel.getSavedSearchedExportEvent().observeSingle(this, count -> {
            showSnackbar(getResources().getQuantityString(R.plurals.exported_searches, count, count));
        });

        viewModel.getSavedSearchedImportEvent().observeSingle(this, count -> {
            showSnackbar(getResources().getQuantityString(R.plurals.imported_searches, count, count));

        });
        viewModel.getErrorEvent().observeSingle(this, error -> {
            if (error != null) {
                showSnackbar(error.getLocalizedMessage());
            }
        });
    }

    private void drawerOpened() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        ActivityUtils.closeSoftKeyboard(this);
    }

    private void drawerClosed() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        if (clearFragmentBackstack) {
            DisplayManager.clear(getSupportFragmentManager());
            clearFragmentBackstack = false;
        }

        performIntros();

        viewModel.refresh(AppPreferences.notebooksSortOrder(this));

        autoSync.trigger(AutoSync.Type.APP_RESUMED);
    }

    private void performIntros() {
        int currentVersion = AppPreferences.lastUsedVersionCode(this);
        boolean isNewVersion = checkIfNewAndUpdateVersion();

        if (isNewVersion) {
            /* Import Getting Started notebook. */
            if (!AppPreferences.isGettingStartedNotebookLoaded(this)) {
                UseCaseRunner.enqueue(new BookImportGettingStarted());
            }

            /* Open drawer for the first time user. */
            if (currentVersion == 0 && mDrawerLayout != null) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }

            // Display what's new
            displayWhatsNewDialog();

            // Clear clipboard (due to possible internal format changes across app versions)
            NotesClipboard.clear();
        }
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
        super.onPause();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.unregisterReceiver(receiver);

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

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
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fragment);

//        if (BuildConfig.LOG_DEBUG) {
//            fragment.getLifecycle().addObserver((LifecycleEventObserver) this::logLifecycleEvent);
//        }
    }

    private void logLifecycleEvent(LifecycleOwner source, Lifecycle.Event event) {
        LogUtils.d(TAG, source.getClass().getSimpleName(), event);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /* Showing dialog in onResume() fails with:
         *   Can not perform this action after onSaveInstanceState
         */
        if (runnableOnResumeFragments != null) {
            runnableOnResumeFragments.run();
            runnableOnResumeFragments = null;
        }

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_NOTE));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_FOLLOW_LINK_TO_NOTE_WITH_PROPERTY));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_FOLLOW_LINK_TO_FILE));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_SAVED_SEARCHES));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_QUERY));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_BOOKS));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_BOOK));
        bm.registerReceiver(receiver, new IntentFilter(AppIntent.ACTION_OPEN_SETTINGS));
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_actions, menu);

        setupSearchView(menu);

        return true;
    }

    /**
     * SearchView setup and query text listeners.
     * TODO: http://developer.android.com/training/search/setup.html
     */
    private void setupSearchView(Menu menu) {
        final MenuItem searchItem = menu.findItem(R.id.activity_action_search);

        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint(getString(R.string.search_hint));

        /* When user starts the search, fill the search box with text depending on current fragment. */
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Make search as wide as possible. */
                ViewGroup.LayoutParams layoutParams = searchView.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;

                /* For Query fragment, fill the box with full query. */
                String q = DisplayManager.getDisplayedQuery(getSupportFragmentManager());
                if (q != null) {
                    searchView.setQuery(q + " ", false);

                } else {
                    /* If searching from book, add book name to query. */
                    Book book = getActiveFragmentBook();
                    if (book != null) {
                        DottedQueryBuilder builder = new DottedQueryBuilder();
                        String query = builder.build(new Query(new Condition.InBook(book.getName())));
                        searchView.setQuery(query + " ", false);
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
                searchItem.collapseActionView();

                DisplayManager.displayQuery(getSupportFragmentManager(), str.trim());

                return true;
            }
        });
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
                SyncService.start(this, new Intent(this, SyncService.class));
                return true;

            case R.id.activity_action_settings:
                openSettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        activityForResult.onResult(requestCode, resultCode, data);
    }

    /**
     * Display a dialog for user to enter notebook's name.
     */
    private void importChosenBook(Uri uri) {
        String guessedBookName = guessBookNameFromUri(uri);

        Bundle bundle = new Bundle();
        bundle.putString("uri", uri.toString());

        SimpleOneLinerDialog
                .getInstance(DIALOG_IMPORT_BOOK, R.string.import_as, R.string.name, R.string.import_, R.string.cancel, guessedBookName, bundle)
                .show(getSupportFragmentManager(), SimpleOneLinerDialog.FRAGMENT_TAG);
    }

    /**
     * @return Guessed book name or {@code null} if it couldn't be guessed
     */
    private String guessBookNameFromUri(Uri uri) {
        String fileName = BookName.getFileName(this, uri);

        if (fileName != null && BookName.isSupportedFormatFileName(fileName)) {
            BookName bookName = BookName.fromFileName(fileName);
            return bookName.getName();

        } else {
            return null;
        }
    }

    @Override
    public void onNoteFocusInBookRequest(long noteId) {
        mSyncFragment.run(new BookSparseTreeForNote(noteId));
    }

    /* Open note fragment to create a new note. */
    @Override
    public void onNoteNewRequest(NotePlace target) {
        finishActionMode();

        DisplayManager.displayNewNote(getSupportFragmentManager(), target);
    }

    @Override
    public void onNoteCreated(Note note) {
        finishActionMode();
        popBackStackAndCloseKeyboard();

        // Display Snackbar with an action (create new note below just created one)
        View view = findViewById(R.id.main_content);
        if (view != null) {
            showSnackbar(Snackbar
                    .make(view, R.string.message_note_created, Snackbar.LENGTH_LONG)
                    .setAction(R.string.new_below, v -> {
                        NotePlace notePlace = new NotePlace(
                                note.getPosition().getBookId(),
                                note.getId(),
                                Place.BELOW);

                        DisplayManager.displayNewNote(getSupportFragmentManager(), notePlace);
                    }));
        }
    }

    @Override
    public void onNoteUpdated(Note note) {
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onNoteCanceled() {
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onStateChangeRequest(Set<Long> noteIds, @Nullable String state) {
        mSyncFragment.run(new NoteUpdateState(noteIds, state));
    }

    @Override
    public void onStateToggleRequest(@NotNull Set<Long> noteIds) {
        mSyncFragment.run(new NoteUpdateStateToggle(noteIds));
    }

    @Override
    public void onScheduledTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time) {
        mSyncFragment.run(new NoteUpdateScheduledTime(noteIds, time));
    }

    @Override
    public void onDeadlineTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time) {
        mSyncFragment.run(new NoteUpdateDeadlineTime(noteIds, time));
    }

    @Override /* BookFragment */
    public void onBookPrefaceEditRequest(Book book) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        finishActionMode();

        DisplayManager.displayEditor(getSupportFragmentManager(), book);
    }

    @Override
    public void onBookPrefaceUpdate(long bookId, String preface) {
        mSyncFragment.run(new BookUpdatePreface(bookId, preface));
    }

    @Override
    public void onNotesDeleteRequest(final long bookId, final Set<Long> noteIds) {
        mSyncFragment.run(new NoteDelete(bookId, noteIds));
    }

    @Override
    public void onNotesCutRequest(long bookId, Set<Long> noteIds) {
        mSyncFragment.run(new NoteCut(bookId, noteIds));
    }

    @Override
    public void onNotesCopyRequest(long bookId, Set<Long> noteIds) {
        mSyncFragment.run(new NoteCopy(bookId, noteIds));
    }

    @Override
    public void onNotesPasteRequest(long bookId, long noteId, Place place) {
        mSyncFragment.run(new NotePaste(bookId, noteId, place));
    }

    @Override
    public void onBookLinkSetRequest(final long bookId) {
        viewModel.setBookLink(bookId);
    }

    @Override
    public void onForceSaveRequest(long bookId) {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.books_context_menu_item_force_save)
                .setMessage(R.string.overwrite_remote_notebook_question)
                .setPositiveButton(R.string.overwrite, (dialog, which) ->
                        mSyncFragment.run(new BookForceSave(bookId)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onForceLoadRequest(long bookId) {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.books_context_menu_item_force_load)
                .setMessage(R.string.overwrite_local_notebook_question)
                .setPositiveButton(R.string.overwrite, (dialog, which) ->
                        mSyncFragment.run(new BookForceLoad(bookId)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Callback from {@link BooksFragment}.
     */
    @Override
    public void onBookExportRequest(@NotNull Book book, @NotNull BookFormat format) {
        // For scoped storage
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//            String defaultFileName = BookName.fileName(book.getName(), format);
//            activityForResult.startBookExportFileChooser(book.getId(), defaultFileName);
//
//        } else {
            runWithPermission(
                    AppPermissions.Usage.BOOK_EXPORT,
                    () -> mSyncFragment.run(new BookExport(book.getId())));
//        }
    }

    @Override
    public void onBookImportRequest() {
        activityForResult.startBookImportFileChooser();
    }

    /**
     * Prompt user for book name and then create it.
     */
    @Override
    public void onBookCreateRequest() {
        SimpleOneLinerDialog
                .getInstance(DIALOG_NEW_BOOK, R.string.new_notebook, R.string.name, R.string.create, R.string.cancel, null, null)
                .show(getSupportFragmentManager(), SimpleOneLinerDialog.FRAGMENT_TAG);
    }

    /**
     * Sync finished.
     *
     * Display Snackbar with a message.  If it makes sense also set action to open a repository.
     *
     * @param msg Error message if syncing failed, null if it was successful
     */
    @Override
    public void onSyncFinished(String msg) {
        if (msg != null) {
            showSnackbarWithReposLink(getString(R.string.sync_with_argument, msg));
        }
    }

    /**
     * Display snackbar and include link to repositories.
     */
    private void showSnackbarWithReposLink(String msg) {
        View view = findViewById(R.id.main_content);

        if (view != null) {
            showSnackbar(Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.repositories, v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setClass(MainActivity.this, ReposActivity.class);
                        startActivity(intent);
                    }));
        }
    }

    @Override
    public void onNotesPromoteRequest(Set<Long> noteIds) {
        mPromoteDemoteOrMoveRequested = true;
        mSyncFragment.run(new NotePromote(noteIds));
    }

    @Override
    public void onNotesDemoteRequest(Set<Long> noteIds) {
        mPromoteDemoteOrMoveRequested = true;
        mSyncFragment.run(new NoteDemote(noteIds));
    }

    @Override
    public void onNotesMoveRequest(long bookId, Set<Long> noteIds, int offset) {
        mPromoteDemoteOrMoveRequested = true;
        mSyncFragment.run(new NoteMove(bookId, noteIds, offset));
    }

    @Override
    public void onBookClicked(long bookId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId);

        /* Attempt to avoid occasional rare IllegalStateException (state loss related).
         * Consider removing BooksFragment.Listener and using broadcasts for all actions instead.
         */
        // DisplayManager.displayBook(bookId, 0);
        Intent intent = new Intent(AppIntent.ACTION_OPEN_BOOK);
        intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
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
    public void onBookPrefaceEditSaveRequest(@NotNull Book book, @NotNull String preface) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.run(new BookUpdatePreface(book.getId(), preface));
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
                mSyncFragment.run(new BookCreate(value));
                break;

            case DIALOG_IMPORT_BOOK:
                Uri uri = Uri.parse(userData.getString("uri"));
                /* We are assuming it's an Org file. */
                mSyncFragment.run(new BookImportFromUri(value, BookFormat.ORG, uri));
                break;
        }
    }

    private Book getActiveFragmentBook() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(BookFragment.FRAGMENT_TAG);

        if (f != null && f.isVisible()) {
            BookFragment bookFragment = (BookFragment) f;
            return bookFragment.getCurrentBook();
        }

        return null;
    }

    /*
     * Action mode
     */

    @Override
    public void updateActionModeForSelection(int selectedCount, Fragment fragment) {

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, selectedCount, fragment);

        if (mActionMode != null) { /* Action menu is already activated. */
            /* Finish action mode if there are no more selected items. */
            if (selectedCount == 0) {
                mActionMode.finish();
            } else {
                mActionMode.invalidate();
            }

        } else { /* No action menu activated - started it. */
            if (selectedCount > 0) {
                /* Start new action mode. */
                mActionMode = startSupportActionMode((ActionMode.Callback) fragment);
            }
        }

        Toolbar bottomToolBar = findViewById(R.id.bottom_action_bar);

        if (bottomToolBar != null) {
            if (fragment instanceof BottomActionBar.Callback) {
                BottomActionBar.Callback callback = (BottomActionBar.Callback) fragment;

                if (selectedCount == 0) {
                    BottomActionBar.hideBottomBar(bottomToolBar);
                } else {
                    BottomActionBar.showBottomBar(bottomToolBar, callback);
                }

            } else {
                BottomActionBar.hideBottomBar(bottomToolBar);
            }
        }

    }

    @Override
    public ActionMode getActionMode() {
        return mActionMode;
    }

    @Override
    public void actionModeDestroyed() {
        if (mActionMode != null) {
            if ("M".equals(mActionMode.getTag()) && mPromoteDemoteOrMoveRequested) {
                // TODO: Remove this from here if possible
                autoSync.trigger(AutoSync.Type.DATA_MODIFIED);
            }
        }
        mPromoteDemoteOrMoveRequested = false;
        mActionMode = null;

        BottomActionBar.hideBottomBar(findViewById(R.id.bottom_action_bar));
    }

    private void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onSavedSearchNewRequest() {
        DisplayManager.onSavedSearchNewRequest(getSupportFragmentManager());
    }

    @Override
    public void onSavedSearchEditRequest(long id) {
        DisplayManager.onSavedSearchEditRequest(getSupportFragmentManager(), id);
    }

    @Override
    public void onSavedSearchDeleteRequest(@NotNull Set<Long> ids) {
        mSyncFragment.run(new SavedSearchDelete(ids));
    }

    @Override
    public void onSavedSearchMoveUpRequest(long id) {
        mSyncFragment.run(new SavedSearchMoveUp(id));
    }

    @Override
    public void onSavedSearchMoveDownRequest(long id) {
        mSyncFragment.run(new SavedSearchMoveDown(id));
    }

    @Override
    public void onSavedSearchCreateRequest(SavedSearch savedSearch) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.run(new SavedSearchCreate(savedSearch));
    }

    @Override
    public void onSavedSearchUpdateRequest(SavedSearch savedSearch) {
        popBackStackAndCloseKeyboard();
        mSyncFragment.run(new SavedSearchUpdate(savedSearch));
    }

    @Override
    public void onSavedSearchCancelRequest() {
        popBackStackAndCloseKeyboard();
    }

    @Override
    public void onSavedSearchesExportRequest(int title, @NonNull String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            activityForResult.startSavedSearchesExportFileChooser();

        } else {
            runWithPermission(
                    AppPermissions.Usage.SAVED_SEARCHES_EXPORT_IMPORT,
                    () -> mSyncFragment.run(new SavedSearchExport()));
        }
    }

    @Override
    public void onSavedSearchesImportRequest(int title, @NonNull String message) {
        activityForResult.startSavedSearchesImportFileChooser();
    }

    private void openSettings() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    public void popBackStackAndCloseKeyboard() {
        getSupportFragmentManager().popBackStack();
        ActivityUtils.closeSoftKeyboard(this);
    }

    /**
     * User action succeeded.
     */
    @Override
    public void onSuccess(UseCase action, UseCaseResult result) {
        if (action instanceof BookExport) {
            showSnackbar(getString(R.string.book_exported, (File) result.getUserData()));

        } else if (action instanceof BookExportToUri) {
            showSnackbar(getString(R.string.book_exported, (Uri) result.getUserData()));

        } else if (action instanceof NoteCut) {
            NotesClipboard clipboard = (NotesClipboard) result.getUserData();

            if (clipboard != null) {
                int count = clipboard.getCount();

                String message;

                if (count == 0) {
                    message = getResources().getString(R.string.no_notes_cut);
                } else {
                    message = getResources().getQuantityString(R.plurals.notes_cut, count, count);
                }

                showSnackbar(message);
            }

        } else if (action instanceof NoteCopy) {
            NotesClipboard clipboard = (NotesClipboard) result.getUserData();

            if (clipboard != null) {
                int count = clipboard.getCount();

                if (count > 0) {
                    String message = getResources().getQuantityString(R.plurals.notes_copied, count, count);
                    showSnackbar(message);
                }
            }

        } else if (action instanceof NotePaste) {
            int count = (int) result.getUserData();

            String message;
            if (count > 0) {
                message = getResources().getQuantityString(R.plurals.notes_pasted, count, count);
            } else {
                message = getResources().getString(R.string.no_notes_pasted);
            }

            showSnackbar(message);
        }
    }

    /**
     * User action failed.
     */
    @Override
    public void onError(UseCase action, Throwable throwable) {
        if (action instanceof BookExport) {
            showSnackbar(getString(
                    R.string.failed_exporting_book, throwable.getLocalizedMessage()));

        } else {
            if (throwable.getCause() != null) {
                showSnackbar(throwable.getCause().getLocalizedMessage());
            } else {
                showSnackbar(throwable.getLocalizedMessage());
            }
        }
    }

    @Override
    public void onNoteOpen(long noteId) {
        finishActionMode();

        viewModel.openNote(noteId);
    }

    // TODO: Consider creating NavigationBroadcastReceiver
    public static void openSpecificNote(long bookId, long noteId) {
        Intent intent = new Intent(AppIntent.ACTION_OPEN_NOTE);
        intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId);
        intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
        LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent);
    }

    public static void followLinkToFile(String path) {
        Intent intent = new Intent(AppIntent.ACTION_FOLLOW_LINK_TO_FILE);
        intent.putExtra(AppIntent.EXTRA_PATH, path);
        LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent);
    }

    public static void followLinkToNoteWithProperty(String name, String value) {
        Intent intent = new Intent(AppIntent.ACTION_FOLLOW_LINK_TO_NOTE_WITH_PROPERTY);
        intent.putExtra(AppIntent.EXTRA_PROPERTY_NAME, name);
        intent.putExtra(AppIntent.EXTRA_PROPERTY_VALUE, value);
        LocalBroadcastManager.getInstance(App.getAppContext()).sendBroadcast(intent);
    }

    private class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

            if (intent != null && intent.getAction() != null) {
                handleIntent(intent, intent.getAction());
            }
        }

        private void handleIntent(@NonNull Intent intent, @NonNull String action) {
            switch (action) {
                case AppIntent.ACTION_OPEN_NOTE: {
                    long bookId = intent.getLongExtra(AppIntent.EXTRA_BOOK_ID, 0);
                    long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0);
                    DisplayManager.displayExistingNote(getSupportFragmentManager(), bookId, noteId);
                    break;
                }

                case AppIntent.ACTION_OPEN_SAVED_SEARCHES: {
                    DisplayManager.displaySavedSearches(getSupportFragmentManager());
                    break;
                }

                case AppIntent.ACTION_OPEN_QUERY: {
                    String query = intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING);
                    DisplayManager.displayQuery(getSupportFragmentManager(), query);
                    break;
                }

                case AppIntent.ACTION_OPEN_BOOKS: {
                    DisplayManager.displayBooks(getSupportFragmentManager(), true);
                    break;
                }

                case AppIntent.ACTION_OPEN_BOOK: {
                    long bookId = intent.getLongExtra(AppIntent.EXTRA_BOOK_ID, 0);
                    long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0);
                    DisplayManager.displayBook(getSupportFragmentManager(), bookId, noteId);
                    break;
                }

                case AppIntent.ACTION_OPEN_SETTINGS: {
                    openSettings();
                    break;
                }

                case AppIntent.ACTION_FOLLOW_LINK_TO_NOTE_WITH_PROPERTY: {
                    String name = intent.getStringExtra(AppIntent.EXTRA_PROPERTY_NAME);
                    String value = intent.getStringExtra(AppIntent.EXTRA_PROPERTY_VALUE);
                    viewModel.followLinkToNoteWithProperty(name, value);
                    break;
                }

                case AppIntent.ACTION_FOLLOW_LINK_TO_FILE: {
                    String path = intent.getStringExtra(AppIntent.EXTRA_PATH);
                    viewModel.followLinkToFile(path);
                    break;
                }
            }
        }
    }
}

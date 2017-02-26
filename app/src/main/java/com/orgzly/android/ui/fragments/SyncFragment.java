package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.BookAction;
import com.orgzly.android.BookName;
import com.orgzly.android.Broadcasts;
import com.orgzly.android.Filter;
import com.orgzly.android.Note;
import com.orgzly.android.NotesBatch;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.Rook;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.sync.SyncStatus;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.NotePlacement;
import com.orgzly.android.ui.Placement;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UriUtils;
import com.orgzly.org.datetime.OrgDateTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * Retained fragment for sync button. FIXME: Cleanup
 *
 * Misused over time and now includes most async tasks. Move them and don't use a single listener
 * (check {@link com.orgzly.android.ui.ShareActivity}, it has to implement tons of methods with no reason)
 */
public class SyncFragment extends Fragment {
    private static final String TAG = SyncFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SyncFragment.class.getName();


    private boolean isServiceBound = false;

    /** Activity which has this fragment attached. Used as a target for hooks. */
    private SyncFragmentListener mListener;

    private Shelf mShelf;

    /** Progress bar and button text. */
    private SyncButton mSyncButton;

    private BroadcastReceiver syncServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncStatus status = SyncStatus.fromIntent(intent);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent, status);

            /* Update sync button based on sync status. */
            mSyncButton.update(status);

            switch (status.type) {
                case FAILED:
                    if (mListener != null) {
                        mListener.onSyncFinished(status.message);
                    }
                    break;

                case NO_STORAGE_PERMISSION:
                    Activity activity = getActivity();
                    if (activity != null) {
                        AppPermissions.isGrantedOrRequest((CommonActivity) activity, AppPermissions.FOR_SYNC_START);
                    }
                    break;

                case CANCELED:
                    if (mListener != null) {
                        /* No error message when sync is canceled by the user. */
                        mListener.onSyncFinished(null);
                    }
                    break;
            }
        }
    };


    public static SyncFragment getInstance() {
        return new SyncFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SyncFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (SyncFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + SyncFragmentListener.class);
        }

        mShelf = new Shelf(context.getApplicationContext());
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(syncServiceReceiver, new IntentFilter(Broadcasts.SYNC));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_sync, container, false);

        /* Retained on configuration change. */
        mSyncButton = new SyncButton(view, mSyncButton);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        /*
         * Bind to sync service to request and receive the sync status.
         * We're doing this after button is initialized.
         */
        bindToSyncService();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroy();

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(syncServiceReceiver);
    }

    /**
     * Set the callback to null so we don't accidentally leak the Activity instance.
     */
    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();
        mListener = null;

    }

    /**
     * Load book from the Uri.
     */
    public void importBookFromUri(
            final String bookName,
            final BookName.Format format,
            final Uri uri) {

        new AsyncTask<Void , Object, Object>() {
            @Override
            protected Object doInBackground(Void ... params) { /* Executing on a different thread. */
                try {
                    Book book;
                    InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                    try {
                        book = mShelf.loadBookFromStream(bookName, format, inputStream);
                    } finally {
                        inputStream.close();
                    }

                    return book;

                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof Book) {
                        Book book = (Book) result;
                        mShelf.setBookStatus(book, null, new BookAction(BookAction.Type.INFO, getString(R.string.imported)));
                        mListener.onBookLoaded((Book) result);
                    } else {
                        mListener.onBookLoadFailed((IOException) result);
                    }
                }
            }
        }.execute();
    }

    /**
     * Load book from resource.
     *
     * FIXME: Only supports Org format (hardcoded below)
     */
    public void loadBook(final String name, final Resources resources, final int resourceId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, name, resources, resourceId);

        new AsyncTask<Void, Object, Object>() {
            @Override
            protected void onPreExecute() {
            }

            /* Executing on a different thread. */
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    return mShelf.loadBookFromResource(name, BookName.Format.ORG, resources, resourceId);
                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof Book) {
                        Book book = (Book) result;
                        // TODO: Do in bg
                        mShelf.setBookStatus(book, null, new BookAction(
                                BookAction.Type.INFO,
                                getString(R.string.loaded_from_resource, name)));
                        mListener.onBookLoaded(book);
                    } else {
                        // TODO: Why is status not updated here?
                        mListener.onBookLoadFailed((IOException) result);
                    }
                }
            }
        }.execute();
    }

    /**
     * Load book from repository.
     */
    public void loadBook(final long bookId) {
        new AsyncTask<Void, Object, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                Book book = BooksClient.get(getActivity(), bookId);

                try {
                    if (book == null) {
                        throw new IOException(getString(R.string.message_book_does_not_exist));
                    }

                    Rook rook = book.getLink();

                    if (rook == null) {
                        throw new IOException(getString(R.string.message_book_has_no_link));
                    }

                    mShelf.setBookStatus(
                            book,
                            null,
                            new BookAction(
                                    BookAction.Type.PROGRESS,
                                    getString(R.string.force_loading_from_uri, UriUtils.friendlyUri(rook.getUri()))));

                    return mShelf.loadBookFromRepo(rook);

                } catch (Exception e) {
                    e.printStackTrace();

                    mShelf.setBookStatus(
                            book,
                            null,
                            new BookAction(
                                    BookAction.Type.ERROR,
                                    getString(R.string.force_loading_failed, e.getLocalizedMessage())));

                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof Book) {
                        Book book = (Book) result;

                        // TODO: Do in bg
                        mShelf.setBookStatus(
                                book,
                                null,
                                new BookAction(
                                        BookAction.Type.INFO,
                                        getString(R.string.force_loaded_from_uri, UriUtils.friendlyUri(book.getLastSyncedToRook().getUri()))));

                        mListener.onBookLoaded((Book) result);

                    } else {
                        mListener.onBookLoadFailed((Exception) result);
                    }
                }
            }
        }.execute();
    }

    public void deleteFilters(final Set<Long> ids) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.deleteFilters(ids);
                return null;
            }
        }.execute();
    }

    public void createFilter(final Filter filter) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.createFilter(filter);
                return null;
            }
        }.execute();
    }

    public void updateFilter(final long id, final Filter filter) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.updateFilter(id, filter);
                return null;
            }
        }.execute();
    }

    public void moveFilterUp(final long id) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.moveFilterUp(id);
                return null;
            }
        }.execute();
    }

    public void moveFilterDown(final long id) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.moveFilterDown(id);
                return null;
            }
        }.execute();
    }

    public void cycleVisibility(final Book book) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.cycleVisibility(book);
                return null;
            }
        }.execute();
    }

    public void sparseTree(final long bookId, final long noteId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                BooksClient.sparseTree(getContext(), bookId, noteId);
                return null;
            }
        }.execute();
    }

    public void setStateToDone(final long noteId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.setStateToDone(noteId);
                return null;
            }
        }.execute();
    }

    public void promoteNotes(final long bookId, final Set<Long> noteIds) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.promoteNotes(bookId, noteIds);
                return null;
            }
        }.execute();
    }

    public void demoteNotes(final long bookId, final Set<Long> noteIds) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.demoteNotes(bookId, noteIds);
                return null;
            }
        }.execute();
    }

    /**
     * Saves book to its linked remote book, or to the one-and-only repository .
     */
    public void forceSaveBook(final long bookId) {
        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                Book book = BooksClient.get(getActivity(), bookId);

                if (book != null) {
                    try {
                        String repoUrl;
                        String fileName;

                        /* Prefer link. */
                        if (book.getLink() != null) {
                            Rook link = book.getLink();
                            repoUrl = link.getRepoUri().toString();
                            fileName = BookName.getFileName(getContext(), link.getUri());

                        } else {
                            repoUrl = repoForSavingBook();
                            fileName = BookName.fileName(book.getName(), BookName.Format.ORG);
                        }

                        mShelf.setBookStatus(book, null,
                                new BookAction(BookAction.Type.PROGRESS,
                                        getString(R.string.force_saving_to_uri, repoUrl)));

                        return mShelf.saveBookToRepo(repoUrl, fileName, book, BookName.Format.ORG);

                    } catch (Exception e) {
                        e.printStackTrace();
                        mShelf.setBookStatus(book, null,
                                new BookAction(BookAction.Type.ERROR,
                                        getString(R.string.force_saving_failed, e.getLocalizedMessage())));
                        return e;
                    }

                } else {
                    return new IOException(getString(R.string.message_book_does_not_exist));
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof Book) {
                        Book book = (Book) result;

                        mShelf.setBookStatus(
                                book,
                                null,
                                new BookAction(
                                        BookAction.Type.INFO,
                                        getString(R.string.force_saved_to_uri, UriUtils.friendlyUri(book.getLastSyncedToRook().getUri()))));

                        mListener.onBookSaved(book);

                    } else {
                        mListener.onBookForceSavingFailed((Exception) result);
                    }
                } else {
                    Log.w(TAG, "Listener not set, not handling saveBookToRepo result");
                }
            }
        }.execute();
    }

    /* If there is only one repository, return its URL.
     * If there are more, we don't know which one to use, so throw exception.
     */
    private String repoForSavingBook() throws IOException {

        Map<String, Repo> repos = mShelf.getAllRepos();

        /* Use repository if there is only one. */

        if (repos.size() == 0) {
            throw new IOException(getString(R.string.no_repos));

        } else if (repos.size() == 1) {
            return repos.keySet().iterator().next();

        } else {
            throw new IOException(getString(R.string.multiple_repos));
        }
    }

    /**
     * Exports book. Link is not updated, book stays linked to the same remote book.
     */
    public void exportBook(final long bookId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId);

        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    return mShelf.exportBook(bookId, BookName.Format.ORG);
                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof File) {
                        mListener.onBookExported((File) result);
                    } else {
                        mListener.onBookExportFailed((IOException) result);
                    }
                } else {
                    Log.w(TAG, "Listener not set, not handling exportBook result");
                }
            }
        }.execute();
    }

    public void clearDatabase() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.clearDatabase();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mListener != null) {
                    mListener.onDatabaseCleared();
                } else {
                    Log.w(TAG, "Listener not set, not calling onDatabaseCleared");
                }
            }
        }.execute();
    }

    public void deleteBook(final Book book, final boolean deleteLinked) {
        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    mShelf.deleteBook(book, deleteLinked);

                    return null; /* Success. */

                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result == null) {
                        mListener.onBookDeleted(book);
                    } else {
                        mListener.onBookDeletingFailed(book, (IOException) result);
                    }
                } else {
                    Log.w(TAG, "Listener not set, not calling onBookDeleted");
                }
            }
        }.execute();
    }

    public void createNewBook(final String name) {
        new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    Book book = mShelf.createBook(name);
                    mShelf.setBookStatus(book, null, new BookAction(BookAction.Type.INFO, getString(R.string.created)));
                    return book;
                } catch (IOException e) {
                    e.printStackTrace();
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                if (mListener != null) {
                    if (result instanceof Book) {
                        mListener.onBookCreated((Book) result);
                    } else {
                        mListener.onBookCreationFailed((IOException) result);
                    }
                } else {
                    Log.w(TAG, "Listener not set, not handling createNewBook result");
                }
            }
        }.execute();
    }

    public void updateScheduledTime(final Set<Long> noteIds, final OrgDateTime time) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.setNotesScheduledTime(noteIds, time);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mListener != null) {
                    mListener.onScheduledTimeUpdated(noteIds, time);
                } else {
                    Log.w(TAG, "Listener not set, not calling onScheduledTimeUpdated");
                }
            }
        }.execute();
    }

    public void updateNoteState(final Set<Long> noteIds, final String state) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.setNotesState(noteIds, state);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mListener != null) {
                    mListener.onStateChanged(noteIds, state);
                } else {
                    Log.w(TAG, "Listener not set, not calling onStateChanged");
                }
            }
        }.execute();
    }

    public void shiftNoteState(final long id, final int direction) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.shiftState(id, direction);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mListener == null) {
                    Log.w(TAG, "Listener not set, not calling onStateChanged");
                }
            }
        }.execute();
    }

    public void onSyncButton() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        Intent intent = new Intent(getActivity(), SyncService.class);
        getActivity().startService(intent);
    }

    public void renameBook(final Book book, final String value) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mShelf.renameBook(book, value);
                } catch (Exception e) {
                    e.printStackTrace();
                    mShelf.setBookStatus(book, null, new BookAction(
                            BookAction.Type.ERROR,
                            getString(R.string.failed_renaming_book_with_reason, e.getLocalizedMessage())));
//                        return e;
                }
                return null;
            }
        }.execute();
    }


    public void updateBookSettings(final Book book) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mShelf.updateBookSettings(book);
                return null;
            }
        }.execute();
    }

    /*
     * Sync button which should be updated from the main UI thread.
     */
    private class SyncButton {
        private final Context appContext;

        private final ProgressBar progressBar;

        private final ViewGroup buttonContainer;
        private final TextView buttonText;
        private final View buttonIcon;

        private final Animation rotation;

        public SyncButton(View view, SyncButton prev) {
            this.appContext = getActivity().getApplicationContext();

            rotation = AnimationUtils.loadAnimation(appContext, R.anim.rotate_counterclockwise);
            rotation.setRepeatCount(Animation.INFINITE);

            progressBar = (ProgressBar) view.findViewById(R.id.sync_progress_bar);

            buttonContainer = (ViewGroup) view.findViewById(R.id.sync_button_container);
            buttonText = (TextView) view.findViewById(R.id.sync_button_text);
            buttonIcon = view.findViewById(R.id.sync_button_icon);

            if (prev != null) {
                /* Restore old state. */
                progressBar.setIndeterminate(prev.progressBar.isIndeterminate());
                progressBar.setMax(prev.progressBar.getMax());
                progressBar.setProgress(prev.progressBar.getProgress());
                progressBar.setVisibility(prev.progressBar.getVisibility());

                buttonText.setText(prev.buttonText.getText());

            } else {
                progressBar.setVisibility(View.GONE);

                setButtonTextToLastSynced();
            }

            buttonContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSyncButton();
                }
            });

            buttonContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(getContext())
                            .setPositiveButton(R.string.ok, null)
                            .setMessage(buttonText.getText())
                            .show();

                    return true;
                }
            });
        }

        private void setButtonTextToLastSynced() {
            long time = AppPreferences.lastSuccessfulSyncTime(appContext);

            if (time > 0) {
                buttonText.setText(getString(R.string.last_sync_prefix, formatLastSyncTime(time)));
            } else {
                buttonText.setText(R.string.sync);
            }
        }

        public void update(SyncStatus status) {
            switch (status.type) {
                case STARTING:
                    progressBar.setIndeterminate(true);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.syncing_in_progress);

                    break;

                case CANCELING:
                    progressBar.setIndeterminate(true);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.canceling);

                    break;

                case BOOKS_COLLECTED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(0);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.syncing_in_progress);

                    break;

                case BOOK_STARTED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(status.currentBook);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(getString(R.string.syncing_book, status.message));

                    break;

                case BOOK_ENDED:
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(status.totalBooks);
                    progressBar.setProgress(status.currentBook);
                    progressBar.setVisibility(View.VISIBLE);

                    setAnimation(true);

                    buttonText.setText(R.string.syncing_in_progress);

                    break;

                case NOT_RUNNING:
                case FINISHED:
                    progressBar.setVisibility(View.GONE);

                    setAnimation(false);

                    setButtonTextToLastSynced();

                    break;

                case CANCELED:
                case FAILED:
                    progressBar.setVisibility(View.GONE);

                    setAnimation(false);

                    buttonText.setText(getString(R.string.last_sync_prefix, status.message));

                    break;
            }
        }

        private void setAnimation(boolean shouldAnimate) {
            if (shouldAnimate) {
                if (buttonIcon.getAnimation() == null) {
                    buttonIcon.startAnimation(rotation);
                }
            } else {
                if (buttonIcon.getAnimation() != null) {
                    buttonIcon.clearAnimation();
                }
            }
        }

        private String formatLastSyncTime(long time) {
            return DateUtils.formatDateTime(
                    appContext,
                    time,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_TIME);
        }
    }

    public void updateNote(final Note note) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... dummy) {
                return mShelf.updateNote(note);
            }

            @Override
            protected void onPostExecute(Integer noOfUpdated) {
                if (noOfUpdated == 1) {
                    mListener.onNoteUpdated(note);
                } else {
                    mListener.onNoteUpdatingFailed(note);
                }
            }
        }.execute();
    }

    public void createNote(final Note note, final NotePlacement notePlacement) {
        new AsyncTask<Void, Void, Note>() {
            @Override
            protected Note doInBackground(Void... dummy) {
                return mShelf.createNote(note, notePlacement);
            }

            @Override
            protected void onPostExecute(Note createdNote) {
                if (createdNote != null) {
                    mListener.onNoteCreated(createdNote, notePlacement);
                } else {
                    mListener.onNoteCreatingFailed();
                }
            }
        }.execute();
    }

    /**
     * Delete notes from the notebook asynchronously.
     * Calls {@link SyncFragmentListener#onNotesDeleted(int)}.
     *
     * @param bookId Book ID
     * @param noteIds Set of notes' IDs
     */
    public void deleteNotes(final long bookId, final TreeSet<Long> noteIds) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mShelf.delete(bookId, noteIds);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mListener.onNotesDeleted(result);
            }
        }.execute();
    }

    public void deleteNotes(long bookId, long noteId) {
        TreeSet<Long> noteIds = new TreeSet<>();
        noteIds.add(noteId);

        deleteNotes(bookId, noteIds);
    }

    /**
     * Cut notes from the notebook asynchronously.
     * Calls {@link SyncFragmentListener#onNotesCut(int)}.
     *
     * @param bookId Book ID
     * @param noteIds Set of notes' IDs
     */
    public void cutNotes(final long bookId, final TreeSet<Long> noteIds) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return mShelf.cut(bookId, noteIds);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mListener.onNotesCut(result);
            }
        }.execute();
    }

    public void pasteNotes(final long bookId, final long noteId, final Placement placement) {
        new AsyncTask<Void, Void, NotesBatch>() {
            @Override
            protected NotesBatch doInBackground(Void... voids) {
                return mShelf.paste(bookId, noteId, placement);
            }

            @Override
            protected void onPostExecute(NotesBatch batch) {
                if (batch != null) {
                    mListener.onNotesPasted(batch);
                } else {
                    mListener.onNotesNotPasted();
                }
            }
        }.execute();
    }

    /**
     * Re-parsing notes currently only checks for notes' title and state.
     */
    public void reParseNotes() {
        new AsyncTask<Void, Object, IOException>() {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage(getString(R.string.updating_notes));
                progressDialog.show();
            }

            @Override
            protected IOException doInBackground(Void[] params) {
                try {
                    mShelf.reParseNotesStateAndTitles(new Shelf.ReParsingNotesListener() {
                        @Override
                        public void noteParsed(int current, int total, String msg) {
                            publishProgress(current, total, msg);
                        }
                    });
                } catch (IOException e) {
                    return e;
                }

                return null; /* Success. */
            }

            @Override
            protected void onPostExecute(IOException exception) {
            /*
             * If dialog is gone due to rotation for example, IllegalArgumentException occurs
             * here on dismiss() (and isShowing() returns true).
             * Catch & ignore - http://stackoverflow.com/questions/2745061/java-lang-illegalargumentexception-view-not-attached-to-window-manager
             */
                try {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {

                }

            /* TODO: Do this for all other errors as well? */
                if (exception != null) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.failure)
                            .setMessage(exception.toString())
                            .setPositiveButton(R.string.ok, null)
                            .show();
                }
            }

            @Override
            protected void onProgressUpdate(Object ... values) {
                int current = (Integer) values[0];
                int total   = (Integer) values[1];
                String msg  = (String)  values[2];

                progressDialog.setMessage(msg);

                if (total == 0) {
                    progressDialog.setIndeterminate(true);

                } else {
                    progressDialog.setIndeterminate(false);
                    progressDialog.setProgress(current);
                    progressDialog.setMax(total);
                }
            }
        }.execute();
    }

    private void bindToSyncService() {
        Intent intent = new Intent(getActivity(), SyncService.class);
        Activity activity = getActivity();
        if (activity != null) {
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindFromSyncService() {
        if (isServiceBound) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.unbindService(serviceConnection);
                isServiceBound = false;
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            isServiceBound = true;

            SyncService.LocalBinder binder = (SyncService.LocalBinder) serviceBinder;

            /* Get current sync status from the service and update the button. */
            SyncStatus status = binder.getService().getStatus();
            mSyncButton.update(status);

            unbindFromSyncService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
        }
    };

    public interface SyncFragmentListener {
        void onBookCreated(Book book);
        void onBookCreationFailed(Exception exception);

        void onBookLoaded(Book book);
        void onBookLoadFailed(Exception exception);

        void onBookSaved(Book book);
        void onBookForceSavingFailed(Exception exception);

        void onSyncFinished(String msg);

        void onBookExported(File file);
        void onBookExportFailed(Exception exception);

        void onNotesPasted(NotesBatch batch);
        void onNotesNotPasted();

        void onDatabaseCleared();

        void onBookDeleted(Book book);
        void onBookDeletingFailed(Book book, IOException exception);

        void onScheduledTimeUpdated(Set<Long> noteIds, OrgDateTime time);

        void onStateChanged(Set<Long> noteIds, String state);

        void onNoteCreated(Note note, NotePlacement notePlacement);
        void onNoteCreatingFailed();

        void onNoteUpdated(Note note);
        void onNoteUpdatingFailed(Note note);

        void onNotesDeleted(int count);
        void onNotesCut(int count);
    }
}

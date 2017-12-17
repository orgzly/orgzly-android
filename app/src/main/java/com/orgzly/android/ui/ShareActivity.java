package com.orgzly.android.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.Note;
import com.orgzly.android.NotesBatch;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.SyncFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.datetime.OrgDateTime;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Activity started when shared to Orgzly.
 *
 * TODO: Resuming - intent will stay the same.
 * If activity is not finished (by save, cancel or pressing back), next share will resume the
 * activity and the intent will stay the same. Other apps seem to have the same problem and
 * it's not a common scenario, but it should be fixed.
 */
public class ShareActivity extends CommonActivity
        implements
        NoteFragment.NoteFragmentListener,
        SyncFragment.SyncFragmentListener {

    public static final String TAG = ShareActivity.class.getName();

    /** Shared text files are read and their content is stored as note content. */
    private static final long MAX_TEXT_FILE_LENGTH_FOR_CONTENT = 1024 * 1024 * 2; // 2 MB

    private static final String SPINNER_POSITION_KEY = "position";

    private SyncFragment mSyncFragment;
    private NoteFragment mNoteFragment;

    private Spinner mBooksSpinner;

    private String mError;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setContentView(R.layout.activity_share);

        getSupportActionBar().setTitle(R.string.new_note);

         /* Set status and action bar colors depending on the fragment. */
        ActivityUtils.setColorsForFragment(this, null);


        Data data = getDataFromIntent(getIntent());

        setupFragments(savedInstanceState, data);

        setupBooksSpinner(savedInstanceState);

        setupBooksSpinnerAdapter(savedInstanceState);
    }

    public Data getDataFromIntent(Intent intent) {
        Data data = new Data();
        mError = null;

        String action = intent.getAction();
        String type = intent.getType();

        if (action == null) {
            // mError = getString(R.string.share_action_not_set);

        } else if (type == null) {
            // mError = getString(R.string.share_type_not_set);

        } else if (action.equals(Intent.ACTION_SEND)) {
            if (type.startsWith("text/")) {

                if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    data.title = intent.getStringExtra(Intent.EXTRA_TEXT);

                } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                    data.title = uri.getLastPathSegment();

                    /*
                     * Store file's content as note content.
                     */
                    try {
                        File file = new File(uri.getPath());

                        /* Don't read large files. */
                        if (file.length() > MAX_TEXT_FILE_LENGTH_FOR_CONTENT) {
                            mError = "File has " + file.length() +
                                    " bytes (refusing to read files larger then " +
                                    MAX_TEXT_FILE_LENGTH_FOR_CONTENT + " bytes)";

                        } else {
                            data.content = MiscUtils.readStringFromFile(file);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        mError = "Failed reading the content of " + uri.toString() + ": " + e.toString();
                    }
                }

                if (data.title != null && data.content == null && intent.hasExtra(Intent.EXTRA_SUBJECT)) {
                    data.content = data.title;
                    data.title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                }

            } else {
                mError = getString(R.string.share_type_not_supported, type);
            }

        } else if (action.equals("com.google.android.gm.action.AUTO_SEND")) {
            if (type.startsWith("text/") && intent.hasExtra(Intent.EXTRA_TEXT)) {
                data.title = intent.getStringExtra(Intent.EXTRA_TEXT);
            }

        } else {
            mError = getString(R.string.share_action_not_supported, action);
        }

        /* Make sure that title is never empty. */
        if (data.title == null) {
            data.title = "";
        }

        return data;
    }

    private void setupBooksSpinnerAdapter(final Bundle savedInstanceState) {
        new AsyncTask<Void, Void, List<Book>>() {
            @Override
            protected List<Book> doInBackground(Void... params) {
                return getBooksList();
            }

            @Override
            protected void onPostExecute(List<Book> books) {
                ArrayAdapter<Book> adapter = new ArrayAdapter<>(ShareActivity.this, R.layout.spinner_item, books);

                adapter.setDropDownViewResource(R.layout.dropdown_item);

                mBooksSpinner.setAdapter(adapter);

                if (savedInstanceState != null && savedInstanceState.containsKey(SPINNER_POSITION_KEY)) {
                    mBooksSpinner.setSelection(savedInstanceState.getInt(SPINNER_POSITION_KEY, 0));
                } else {
                    String defaultBook = AppPreferences.shareNotebook(getApplicationContext());
                    for (int i=0; i<books.size(); i++) {
                        if (defaultBook.equals(books.get(i).getName())) {
                            mBooksSpinner.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }.execute();
    }

    private void setupFragments(Bundle savedInstanceState, Data data) {
         /* Setup fragments. */
        if (savedInstanceState == null) { /* Create and add fragments. */

            mSyncFragment = SyncFragment.getInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mSyncFragment, SyncFragment.FRAGMENT_TAG)
                    .commit();

            mNoteFragment = NoteFragment.getInstance(true, 0, 0, Place.UNSPECIFIED, data.title, data.content);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_share_main, mNoteFragment, NoteFragment.FRAGMENT_TAG)
                    .commit();

        } else { /* Get existing fragments. */
            mSyncFragment = (SyncFragment) getSupportFragmentManager().findFragmentByTag(SyncFragment.FRAGMENT_TAG);
            mNoteFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag(NoteFragment.FRAGMENT_TAG);
        }
    }

    private void setupBooksSpinner(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        mBooksSpinner = (Spinner) findViewById(R.id.activity_share_books_spinner);

        /* On spinner book select - update note's book. */
        mBooksSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, mBooksSpinner.getSelectedItem(), mNoteFragment);

                if (mBooksSpinner.getSelectedItem() != null) {
                    Book book = (Book) mBooksSpinner.getSelectedItem();

                    if (book != null && mNoteFragment != null) {
                        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Setting book for fragment", book);

                        mNoteFragment.setBook(book);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Shelf shelf = new Shelf(this);
        shelf.syncOnResume();

        if (mError != null) {
            showSimpleSnackbarLong(mError);
            mError = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mBooksSpinner != null && mBooksSpinner.getSelectedItem() != null) {
            outState.putInt(SPINNER_POSITION_KEY, mBooksSpinner.getSelectedItemPosition());
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState);
    }

    /**
     * Collects list of books from database.
     * If there are no books available, create one.
     */
    private List<Book> getBooksList() {
        Shelf shelf = new Shelf(this);

        List<Book> books = shelf.getBooks();

        if (books.size() == 0) {
            try {
                Book book = shelf.createBook(AppPreferences.shareNotebook(getApplicationContext()));
                books.add(book);
            } catch (IOException e) {
                // TODO: Test and handle better.
                e.printStackTrace();
                finish();
            }
        }

        return books;
    }

    public static PendingIntent createNewNoteIntent(Context context) {
        Intent resultIntent = new Intent(context, ShareActivity.class);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.setType("text/plain");
        resultIntent.putExtra(Intent.EXTRA_TEXT, "");

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ShareActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

//        return PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void announceChanges(String fragmentTag, CharSequence title, CharSequence subTitle, int selectionCount) {
    }

    @Override
    public void onNoteCreateRequest(Note note, NotePlace notePlace) {
        mSyncFragment.createNote(note, notePlace);
    }

    @Override
    public void onNoteUpdateRequest(Note note) {
    }

    @Override
    public void onNoteCancelRequest(Note note) {
        finish();
    }

    @Override
    public void onNoteDeleteRequest(Note note) {
    }

    @Override
    public void onBookCreated(Book book) {
    }

    @Override
    public void onBookCreationFailed(Exception exception) {
    }

    @Override
    public void onBookSaved(Book book) {
    }

    @Override
    public void onBookForceSavingFailed(Exception exception) {
    }

    @Override
    public void onSyncFinished(String msg) {
    }

    @Override
    public void onBookExported(File file) {
    }

    @Override
    public void onBookExportFailed(Exception exception) {
    }

    @Override
    public void onNotesPasted(NotesBatch batch) {
    }

    @Override
    public void onNotesNotPasted() {
    }

    @Override
    public void onBookDeleted(Book book) {
    }

    @Override
    public void onBookDeletingFailed(Book book, IOException exception) {
    }

    @Override
    public void onScheduledTimeUpdated(Set<Long> noteIds, OrgDateTime time) {
    }

    @Override
    public void onNoteCreated(Note note) {
        finish();
    }

    @Override
    public void onNoteCreatingFailed() {
    }

    @Override
    public void onNoteUpdated(Note note) {
    }

    @Override
    public void onNoteUpdatingFailed(Note note) {
    }

    @Override
    public void onNotesDeleted(int count) {
    }

    @Override
    public void onNotesCut(int count) {
    }

    @Override
    public void onNotesMoved(int result) {
    }

    @Override
    public void onFailure(String message) {
        showSimpleSnackbarLong(message);
    }

    private class Data {
        String title;
        String content;
    }
}

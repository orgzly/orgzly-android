package com.orgzly.android.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Book;
import com.orgzly.android.BookUtils;
import com.orgzly.android.Note;
import com.orgzly.android.NotesBatch;
import com.orgzly.android.Shelf;
import com.orgzly.android.filter.Filter;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryUtils;
import com.orgzly.android.query.user.DottedQueryParser;
import com.orgzly.android.ui.fragments.NoteFragment;
import com.orgzly.android.ui.fragments.SyncFragment;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.IOException;

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

    private SyncFragment mSyncFragment;
    private NoteFragment mNoteFragment;

    private String mError;

    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setContentView(R.layout.activity_share);

        setupActionBar(R.string.new_note, false);

        Data data = getDataFromIntent(getIntent());

        setupFragments(savedInstanceState, data);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
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

                if (intent.hasExtra(AppIntent.EXTRA_FILTER)) {
                    Query query = new DottedQueryParser().parse(intent.getStringExtra(AppIntent.EXTRA_FILTER));
                    String bookName = QueryUtils.extractFirstBookNameFromQuery(query.getCondition());

                    if (bookName != null) {
                        Book book = new Shelf(this).getBook(bookName);
                        if (book != null) {
                            data.bookId = book.getId();
                        }
                    }
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

    private void setupFragments(Bundle savedInstanceState, Data data) {
        if (savedInstanceState == null) { /* Create and add fragments. */

            mSyncFragment = SyncFragment.getInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mSyncFragment, SyncFragment.FRAGMENT_TAG)
                    .commit();

            try {
                long bookId;
                if (data.bookId == null) {
                    bookId = BookUtils.getTargetBook(this).getId();
                } else {
                    bookId = data.bookId;
                }

                mNoteFragment = NoteFragment.forSharedNote(bookId, data.title, data.content);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_share_main, mNoteFragment, NoteFragment.FRAGMENT_TAG)
                        .commit();
            } catch (IOException ex) {
                ex.printStackTrace();
                // bail out here
                finish();
            }
        } else { /* Get existing fragments. */
            mSyncFragment = (SyncFragment) getSupportFragmentManager().findFragmentByTag(SyncFragment.FRAGMENT_TAG);
            mNoteFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag(NoteFragment.FRAGMENT_TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Shelf shelf = new Shelf(this);
        shelf.syncOnResume();

        if (mError != null) {
            showSnackbar(mError);
            mError = null;
        }
    }

    public static PendingIntent createNewNoteIntent(Context context, Filter filter) {
        Intent resultIntent = new Intent(context, ShareActivity.class);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.setType("text/plain");
        resultIntent.putExtra(Intent.EXTRA_TEXT, "");

        if (filter != null && filter.getQuery() != null) {
            resultIntent.putExtra(AppIntent.EXTRA_FILTER, filter.getQuery());
        }

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
    public void onNoteCreated(Note note) {
        finish();
    }

    @Override
    public void onNoteCreatingFailed() {
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
    public void onFailure(String message) {
        showSnackbar(message);
    }

    private class Data {
        String title;
        String content;
        Long bookId = null;
    }
}

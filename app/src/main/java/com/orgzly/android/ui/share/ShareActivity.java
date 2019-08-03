package com.orgzly.android.ui.share;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.TaskStackBuilder;
import android.database.Cursor;
import android.provider.MediaStore;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.usecase.UseCase;
import com.orgzly.android.usecase.UseCaseResult;
import com.orgzly.android.usecase.NoteCreate;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryUtils;
import com.orgzly.android.query.user.DottedQueryParser;
import com.orgzly.android.sync.AutoSync;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.note.NoteFragment;
import com.orgzly.android.ui.main.SyncFragment;
import com.orgzly.android.ui.note.NotePayload;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

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
        NoteFragment.Listener,
        SyncFragment.Listener {

    public static final String TAG = ShareActivity.class.getName();

    /** Shared text files are read and their content is stored as note content. */
    private static final long MAX_TEXT_FILE_LENGTH_FOR_CONTENT = 1024 * 1024 * 2; // 2 MB

    private SyncFragment mSyncFragment;

    private String mError;

    private AlertDialog dialog;

    @Inject
    DataRepository dataRepository;

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

                if (intent.hasExtra(AppIntent.EXTRA_QUERY_STRING)) {
                    Query query = new DottedQueryParser().parse(intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING));
                    String bookName = QueryUtils.extractFirstBookNameFromQuery(query.getCondition());

                    if (bookName != null) {
                        Book book = dataRepository.getBook(bookName);
                        if (book != null) {
                            data.bookId = book.getId();
                        }
                    }
                }

                if (intent.hasExtra(AppIntent.EXTRA_BOOK_ID)) {
                    data.bookId = intent.getLongExtra(AppIntent.EXTRA_BOOK_ID, 0L);
                }

            } else if (type.startsWith("image/")) {
		handleSendImage(intent, data); // Handle single image being sent

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
        NoteFragment noteFragment;

        if (savedInstanceState == null) { /* Create and add fragments. */

            mSyncFragment = SyncFragment.getInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mSyncFragment, SyncFragment.FRAGMENT_TAG)
                    .commit();

            try {
                long bookId;
                if (data.bookId == null) {
                    bookId = dataRepository.getTargetBook(this).getBook().getId();
                } else {
                    bookId = data.bookId;
                }

                noteFragment = NoteFragment.forNewNote(
                        new NotePlace(bookId), data.title, data.content);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_share_main, noteFragment, NoteFragment.FRAGMENT_TAG)
                        .commit();
            } catch (IOException ex) {
                ex.printStackTrace();
                // bail out here
                finish();
            }
        } else { /* Get existing fragments. */
            mSyncFragment = (SyncFragment) getSupportFragmentManager().findFragmentByTag(SyncFragment.FRAGMENT_TAG);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        autoSync.trigger(AutoSync.Type.APP_RESUMED);

        if (mError != null) {
            showSnackbar(mError);
            mError = null;
        }
    }

    public static PendingIntent createNewNoteIntent(Context context, SavedSearch savedSearch) {
        Intent resultIntent = createNewNoteInNotebookIntent(context, null);

        if (savedSearch != null && savedSearch.getQuery() != null) {
            resultIntent.putExtra(AppIntent.EXTRA_QUERY_STRING, savedSearch.getQuery());
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

        // return PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * @param bookId null means default
     */
    public static Intent createNewNoteInNotebookIntent(Context context, Long bookId) {
           Intent intent = new Intent(context, ShareActivity.class);
           intent.setAction(Intent.ACTION_SEND);
           intent.setType("text/plain");
           intent.putExtra(Intent.EXTRA_TEXT, "");
           if (bookId != null) {
               intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId);
           }
           return intent;
    }

    @Override
    public void onNoteCreateRequest(NotePayload notePayload, NotePlace notePlace) {
        mSyncFragment.run(new NoteCreate(notePayload, notePlace));
    }

    @Override
    public void onNoteUpdateRequest(NotePayload notePayload, long noteId) {
    }

    @Override
    public void onNoteCancelRequest() {
        finish();
    }

    @Override
    public void onSyncFinished(String msg) {
    }

    /**
     * User action succeeded.
     */
    @Override
    public void onSuccess(UseCase action, UseCaseResult result) {
        if (action instanceof NoteCreate) {
            finish();
        }
    }

    /**
     * User action failed.
     */
    @Override
    public void onError(UseCase action, Throwable throwable) {
        showSnackbar(throwable.getLocalizedMessage());
    }

    private class Data {
        String title;
        String content;
        Long bookId = null;
    }

    /**
     * Get file path from image shared with orgzly
     * and put it as a file link in the note's content
     */
    private void handleSendImage(Intent intent, Data data) {
	// Get file uri from intent which probably looks like this:
	// content://media/external/images/...
	Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
	String absoluteFilePath = getRealPathFromUri(this, uri);
	// Put real file path prefixed by 'file:' in note's content
	data.content = "file:" + absoluteFilePath;	
    }

    /**
     * Get real file path from content:// link pointing to file
     * ( https://stackoverflow.com/a/20059657 )
     */
    public static String getRealPathFromUri(Context context, Uri contentUri) {
	Cursor cursor = null;
	try {
	    String[] proj = { MediaStore.Images.Media.DATA };
	    cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	} finally {
	    if (cursor != null) {
		cursor.close();
	    }
	}
    }
}

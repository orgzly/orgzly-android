package com.orgzly.android.ui.share;

import androidx.appcompat.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.Note;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryUtils;
import com.orgzly.android.query.user.DottedQueryParser;
import com.orgzly.android.sync.AutoSync;
import com.orgzly.android.ui.AppSnackbarUtils;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.SharingShortcutsManager;
import com.orgzly.android.ui.sync.SyncFragment;
import com.orgzly.android.ui.note.NoteFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.usecase.UseCase;
import com.orgzly.android.usecase.UseCaseResult;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.OrgStringUtils;

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

    public static final String ATTACH_METHOD_LINK = "link";
    public static final String ATTACH_METHOD_COPY_DIR = "copy_dir";
    public static final String ATTACH_METHOD_COPY_ID = "copy_id";

    /** Shared text files are read and their content is stored as note content. */
    private static final long MAX_TEXT_FILE_LENGTH_FOR_CONTENT = 1024 * 1024 * 2; // 2 MB

    private SyncFragment mSyncFragment;

    private String mError;

    private AlertDialog dialog;

    @Inject
    DataRepository dataRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.appComponent.inject(this);

        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setContentView(R.layout.activity_share);

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

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

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

                // TODO: Was used for direct share shortcuts to pass the book name. Used someplace else?
                if (intent.hasExtra(AppIntent.EXTRA_QUERY_STRING)) {
                    Query query = new DottedQueryParser().parse(intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING));
                    String bookName = QueryUtils.extractFirstBookNameFromQuery(query.getCondition());

                    if (bookName != null) {
                        Book book = dataRepository.getBook(bookName);
                        if (book != null) {
                            data.bookId = book.getId();
                            if (BuildConfig.LOG_DEBUG)
                                LogUtils.d(TAG, "Using book " + data.bookId
                                        + " from passed query " + query + " (" + bookName + ")");
                        }
                    }
                }

                if (intent.hasExtra(AppIntent.EXTRA_BOOK_ID)) {
                    data.bookId = intent.getLongExtra(AppIntent.EXTRA_BOOK_ID, 0L);
                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Using book " + data.bookId
                                + " from passed book ID");
                }

                // Coming from Direct Share shortcut
                if (intent.hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
                    String shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID);
                    data.bookId = SharingShortcutsManager.bookIdFromShortcutId(shortcutId);
                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Using book " + data.bookId
                                + " from passed shortcut ID");
                }

            } else if (ATTACH_METHOD_COPY_DIR.equals(AppPreferences.attachMethod(this))) {
                handleCopyFile(intent, data, "file:");
            } else if (ATTACH_METHOD_COPY_ID.equals(AppPreferences.attachMethod(this))) {
                handleCopyFile(intent, data, "attachment:");
            } else {
                // Link method.
                handleLinkFile(intent, data);
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

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, "Data title: " + data.title + " attachmentUri: " + data.attachmentUri);

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
                        new NotePlace(bookId), data.title, data.content, data.attachmentUri);

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
            AppSnackbarUtils.showSnackbar(this, mError);
            mError = null;
        }
    }

    public static PendingIntent createNewNotePendingIntent(Context context, String category, SavedSearch savedSearch) {
        Intent resultIntent = createNewNoteIntent(context);

        // For distinguishing pending events
        resultIntent.addCategory(category);

        if (savedSearch != null) {
            resultIntent.putExtra(AppIntent.EXTRA_QUERY_STRING, savedSearch.getQuery());
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, resultIntent);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ShareActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        return stackBuilder.getPendingIntent(
                0, ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static Intent createNewNoteIntent(Context context) {
        Intent intent = new Intent(context, ShareActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "");
        return intent;
    }

    @Override
    public void onNoteCreated(Note note) {
        finish();
    }

    @Override
    public void onNoteUpdated(Note note) {
    }

    @Override
    public void onNoteCanceled() {
        finish();
    }

    /**
     * User action succeeded.
     */
    @Override
    public void onSuccess(UseCase action, UseCaseResult result) {
    }

    /**
     * User action failed.
     */
    @Override
    public void onError(UseCase action, Throwable throwable) {
        AppSnackbarUtils.showSnackbar(this, throwable.getLocalizedMessage());
    }

    private class Data {
        String title;
        String content;
        public Uri attachmentUri;
        Long bookId = null;
    }

    /**
     * Get file path shared with Orgzly
     * and put it as a file link in the note's content.
     */
    private void handleLinkFile(Intent intent, Data data) {
        // Get file uri from intent which probably looks like this:
        // content://media/external/images/...
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null) {
                cursor.moveToFirst();

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, DatabaseUtils.dumpCursorToString(cursor));

                /*
                 * Get real file path from content:// link pointing to file
                 * ( https://stackoverflow.com/a/20059657 )
                 */
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

                if (dataColumnIndex != -1) {
                    String mediaData = cursor.getString(dataColumnIndex);
                    if (mediaData != null) {
                        data.content = "file:" + mediaData;
                    }
                }

                if (data.content == null) {
                    data.content = uri.toString()
                            + "\n\nCannot determine a local path to this file.";

                    Log.e(TAG, DatabaseUtils.dumpCursorToString(cursor));
                }

                int displayNameColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

                if (displayNameColumnIndex != -1) {
                    data.title = cursor.getString(displayNameColumnIndex);
                } else {
                    data.title = uri.toString();
                }
            }
        }

        if (data.title == null) {
            data.title = uri.toString();
            data.content = "Cannot find filename using this URI.";
        }
    }

    private void handleCopyFile(Intent intent, Data data, String linkPrefix) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        // Get the file name of the content.
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        String fileName = null;
        if (documentFile != null) {
            fileName = documentFile.getName();
        }
        if (!OrgStringUtils.isEmpty(fileName)) {
            data.title = fileName;
            data.content = "[[" + linkPrefix + fileName + "]]";
        } else {
            data.title = uri.toString();
            data.content = uri.toString() + "\n\nCannot determine fileName to this content.";
        }

        // Don't copy the file here, only copy it when a note is saved.
        // Let's pass the Uri to NoteFragment.
        data.attachmentUri = uri;
    }
}

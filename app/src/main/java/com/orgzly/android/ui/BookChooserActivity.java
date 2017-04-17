package com.orgzly.android.ui;

import android.content.Intent;
import android.os.Bundle;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

/**
 * Activity for choosing a notebook, used e.g. for creating shortcuts.
 */
public class BookChooserActivity extends CommonActivity
        implements BooksFragment.BooksFragmentListener {

    public static final String TAG = BookChooserActivity.class.getName();

    private BooksFragment mBooksFragment;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        setContentView(R.layout.activity_bookchooser);

        Intent intent = getIntent();
        action = intent.getAction();
        if (action.equals(Intent.ACTION_CREATE_SHORTCUT)) {
            getSupportActionBar().setTitle(R.string.pick_book_for_shortcut);
        }

         /* Set status and action bar colors depending on the fragment. */
        ActivityUtils.setColorsForFragment(this, null);

        setupFragments(savedInstanceState);
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mBooksFragment = BooksFragment.getInstance(false, false);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_bookchooser_main, mBooksFragment, BooksFragment.FRAGMENT_TAG)
                    .commit();
        } else {
            mBooksFragment = (BooksFragment) getSupportFragmentManager().findFragmentByTag(BooksFragment.FRAGMENT_TAG);
        }
    }

    @Override
    public void onBookClicked(long bookId) {
        if (action.equals(Intent.ACTION_CREATE_SHORTCUT)) {
            Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.cic_orgzly_logo);

            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.putExtra(MainActivity.EXTRA_BOOK_ID, bookId);

            Intent shortcut = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            // TODO add correct title
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, "TODO TITLE");
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);

            setResult(RESULT_OK, shortcut);
            finish();
        }
    }

    @Override
    public void announceChanges(String fragmentTag, CharSequence title, CharSequence subTitle, int selectionCount) {
    }

    @Override
    public void onBookCreateRequest() {
    }

    @Override
    public void onBookDeleteRequest(long bookId) {
    }

    @Override
    public void onBookRenameRequest(long bookId) {
    }

    @Override
    public void onBookLinkSetRequest(long bookId) {
    }

    @Override
    public void onForceSaveRequest(long bookId) {
    }

    @Override
    public void onForceLoadRequest(long bookId) {
    }

    @Override
    public void onBookExportRequest(long bookId) {
    }

    @Override
    public void onBookLoadRequest() {
    }
}

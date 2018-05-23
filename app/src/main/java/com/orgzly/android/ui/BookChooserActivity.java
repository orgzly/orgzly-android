package com.orgzly.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Book;
import com.orgzly.android.BookUtils;
import com.orgzly.android.Shelf;
import com.orgzly.android.ui.fragments.BooksFragment;
import com.orgzly.android.util.LogUtils;

/**
 * Activity for creating a notebook shortcut.
 */
public class BookChooserActivity extends CommonActivity
        implements BooksFragment.BooksFragmentListener {

    public static final String TAG = BookChooserActivity.class.getName();

    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);

        action = getIntent().getAction();

        setContentView(R.layout.activity_bookchooser);

        setupActionBar(R.string.pick_a_notebook, false);

        setupFragments(savedInstanceState);
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            BooksFragment mBooksFragment = BooksFragment.getInstance(false, false);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_bookchooser_main, mBooksFragment, BooksFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public void onBookClicked(long bookId) {
        if (action != null && action.equals(Intent.ACTION_CREATE_SHORTCUT)) {

            // Get Book by its ID
            Shelf shelf = new Shelf(this);
            Book book = shelf.getBook(bookId);

            if (book == null) {
                Toast.makeText(this, R.string.book_does_not_exist_anymore, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            String id = "notebook-" + bookId;
            String name = book.getName();
            String title = BookUtils.getFragmentTitleForBook(book);
            Intent launchIntent = createLaunchIntent(book);
            IconCompat icon = createIcon();

            ShortcutInfoCompat shortcut =
                    new ShortcutInfoCompat.Builder(this, id)
                            .setShortLabel(name)
                            .setLongLabel(title)
                            .setIcon(icon)
                            .setIntent(launchIntent)
                            .build();

            setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut));

            finish();
        }
    }

    /**
     * Create intent for opening specified notebook.
     */
    private Intent createLaunchIntent(Book book) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(AppIntent.EXTRA_BOOK_ID, book.getId());
        return intent;
    }

    /**
     * Create icon for the shortcut.
     */
    private IconCompat createIcon() {
        return IconCompat.createWithResource(this, R.mipmap.cic_shortcut_notebook);
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
    public void onBookImportRequest() {
    }
}

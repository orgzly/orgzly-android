package com.orgzly.android.ui.fragments;


import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.BookAction;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.Fab;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UriUtils;

import java.text.DateFormat;
import java.util.Date;

/**
 * Displays all notebooks.
 * Allows creating new, deleting, renaming, setting links etc.
 */
public class BooksFragment extends ListFragment
        implements
        Fab,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = BooksFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = BooksFragment.class.getName();

    private BooksFragmentListener mListener;
    private SimpleCursorAdapter mListAdapter;
    private View mNoNotebookText;
    private boolean mIsViewCreated = false;


    public static BooksFragment getInstance() {
        return new BooksFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BooksFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    @Override
    public void onAttach(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getActivity());
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (BooksFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + BooksFragmentListener.class);
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

        mListAdapter = createAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_books, container, false);

        mNoNotebookText = view.findViewById(R.id.fragment_books_no_notebooks);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        mIsViewCreated = true;

        /* Request callbacks for Context menu. */
        registerForContextMenu(getListView());

        setListAdapter(mListAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        getActivity().getSupportLoaderManager().initLoader(Loaders.BOOKS_FRAGMENT, null, this);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        /* In case books sort order preference has been changed. */
        getActivity().getSupportLoaderManager().restartLoader(Loaders.BOOKS_FRAGMENT, null, this);

        announceChangesToActivity();
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

        mListener = null;
    }

    private SimpleCursorAdapter createAdapter() {
        SimpleCursorAdapter adapter;

        /* Column field names to be bound. */
        String[] columns = new String[] {
                ProviderContract.Books.Param.NAME,
                ProviderContract.Books.Param.NAME,
                ProviderContract.Books.Param.MTIME,
                ProviderContract.Books.Param.LAST_ACTION,
                ProviderContract.Books.Param.LINK_REPO_URL,
                ProviderContract.Books.Param.LINK_ROOK_URL,
                ProviderContract.Books.Param.SYNCED_REPO_URL,
                ProviderContract.Books.Param.SYNCED_ROOK_URL,
                ProviderContract.Books.Param.SYNCED_ROOK_REVISION,
                ProviderContract.Books.Param.SYNCED_ROOK_MTIME,
                ProviderContract.Books.Param.USED_ENCODING,
                ProviderContract.Books.Param.DETECTED_ENCODING,
                ProviderContract.Books.Param.SELECTED_ENCODING,
        };

        /* Views which the data will be bound to. */
        int[] to = new int[] {
                R.id.item_book_title,
                R.id.item_book_subtitle,
                R.id.item_book_mtime,
                R.id.item_book_last_action,
                R.id.item_book_link_repo,
                R.id.item_book_link_url,
                R.id.item_book_synced_repo,
                R.id.item_book_synced_url,
                R.id.item_book_synced_revision,
                R.id.item_book_synced_mtime,
                R.id.item_book_encoding_used,
                R.id.item_book_encoding_detected,
                R.id.item_book_encoding_selected
        };

        adapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.item_book,
                null,
                columns,
                to,
                0) {


            class ViewHolder {
                TextView title;
                View modifiedAfterSyncIcon;

                View bookDetailsPadding;
                View mtimeContainer;
                View linkDetailsContainer;
                View versionedRookContainer;
                View versionedRookUrlContainer;
                View versionedRookMtimeContainer;
                View versionedRookRevisionContainer;
                View lastActionContainer;
                View usedEncodingContainer;
                View detectedEncodingContainer;
                View selectedEncodingContainer;
                TextView lastAction;
                TextView subTitle;
                TextView mtime;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                boolean isBookDetailDisplayed = false;

                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder == null) {
                    holder = new ViewHolder();
                    holder.title = (TextView) view.findViewById(R.id.item_book_title);
                    holder.modifiedAfterSyncIcon = view.findViewById(R.id.item_book_modified_after_sync_icon);
                    holder.bookDetailsPadding = view.findViewById(R.id.item_book_details_padding);
                    holder.subTitle = (TextView) view.findViewById(R.id.item_book_subtitle);
                    holder.mtimeContainer = view.findViewById(R.id.item_book_mtime_container);
                    holder.mtime = (TextView) view.findViewById(R.id.item_book_mtime);
                    holder.linkDetailsContainer = view.findViewById(R.id.item_book_link_container);
                    holder.versionedRookContainer = view.findViewById(R.id.item_book_synced_container);
                    holder.versionedRookUrlContainer = view.findViewById(R.id.item_book_synced_url_container);
                    holder.versionedRookMtimeContainer = view.findViewById(R.id.item_book_synced_mtime_container);
                    holder.versionedRookRevisionContainer = view.findViewById(R.id.item_book_synced_revision_container);
                    holder.lastActionContainer = view.findViewById(R.id.item_book_last_action_container);
                    holder.lastAction = (TextView) view.findViewById(R.id.item_book_last_action);
                    holder.usedEncodingContainer = view.findViewById(R.id.item_book_encoding_used_container);
                    holder.detectedEncodingContainer = view.findViewById(R.id.item_book_encoding_detected_container);
                    holder.selectedEncodingContainer = view.findViewById(R.id.item_book_encoding_selected_container);

                    view.setTag(holder);
                }

                Book book = BooksClient.fromCursor(cursor);

                /*
                 * If title exists - use title and set book's name as a subtitle.
                 * If title does no exist - use book's name hide the subtitle.
                 */
                if (book.getOrgFileSettings().getTitle() != null) {
                    holder.title.setText(book.getOrgFileSettings().getTitle());
                    holder.subTitle.setText(book.getName());
                    holder.subTitle.setVisibility(View.VISIBLE);
                } else {
                    holder.title.setText(book.getName());
                    holder.subTitle.setVisibility(View.GONE);
                }

                if (book.isModifiedAfterLastSync()) {
                    holder.modifiedAfterSyncIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.modifiedAfterSyncIcon.setVisibility(View.INVISIBLE);
                }

                /*
                 * Modification time.
                 */
                if (AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_mtime))) {
                    holder.mtimeContainer.setVisibility(View.VISIBLE);
                    isBookDetailDisplayed = true;
                } else {
                    holder.mtimeContainer.setVisibility(View.GONE);
                }

                /* If book has no link - remove related rows. */
                if (book.getLink() != null && AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_link_url))) {
                    holder.linkDetailsContainer.setVisibility(View.VISIBLE);
                    isBookDetailDisplayed = true;
                } else {
                    holder.linkDetailsContainer.setVisibility(View.GONE);
                }

                /* If book has no versioned rook - remove all related rows. */
                if (book.getLastSyncedToRook() != null) {
                    holder.versionedRookContainer.setVisibility(View.VISIBLE);

                    if (AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_sync_url))) {
                        holder.versionedRookUrlContainer.setVisibility(View.VISIBLE);
                        isBookDetailDisplayed = true;
                    } else {
                        holder.versionedRookUrlContainer.setVisibility(View.GONE);
                    }

                    if (AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_sync_mtime))) {
                        holder.versionedRookMtimeContainer.setVisibility(View.VISIBLE);
                        isBookDetailDisplayed = true;
                    } else {
                        holder.versionedRookMtimeContainer.setVisibility(View.GONE);
                    }

                    if (AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_sync_revision))) {
                        holder.versionedRookRevisionContainer.setVisibility(View.VISIBLE);
                        isBookDetailDisplayed = true;
                    } else {
                        holder.versionedRookRevisionContainer.setVisibility(View.GONE);
                    }

                } else {
                    holder.versionedRookContainer.setVisibility(View.GONE);
                }

                /* Hide last action if
                 * - there is no last action (sync) performed
                 *   OR
                 * - action is INFO but user choose not to display it
                 */
                if (book.getLastAction() == null || (book.getLastAction().getType() == BookAction.Type.INFO && !AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_last_action)))) {
                    holder.lastActionContainer.setVisibility(View.GONE);

                } else {
                    holder.lastActionContainer.setVisibility(View.VISIBLE);
                    isBookDetailDisplayed = true;

                    SpannableStringBuilder builder = new SpannableStringBuilder();

                    builder.append(DateFormat.getDateTimeInstance().format(new Date(book.getLastAction().getTimestamp())));
                    builder.append(": ");
                    int pos = builder.length();
                    builder.append(book.getLastAction().getMessage());

                    if (book.getLastAction().getType() == BookAction.Type.ERROR) {
                        /* Get error color attribute. */
                        TypedArray arr = getActivity().obtainStyledAttributes(
                                new int[] { R.attr.item_book_error_color });
                        int color = arr.getColor(0, 0);
                        arr.recycle();

                        /* Set error color. */
                        builder.setSpan(new ForegroundColorSpan(color), pos, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    } else if (book.getLastAction().getType() == BookAction.Type.PROGRESS) {
                        builder.setSpan(new StyleSpan(Typeface.BOLD), pos, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    holder.lastAction.setText(builder);
                }

                /* If encoding is not set, removed it. */
                if (book.getUsedEncoding() != null && AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_encoding_used))) {
                    holder.usedEncodingContainer.setVisibility(View.VISIBLE);
                    isBookDetailDisplayed = true;
                } else {
                    holder.usedEncodingContainer.setVisibility(View.GONE);
                }

                if (book.getDetectedEncoding() != null && AppPreferences.displayedBookDetails(context).contains(getString(R.string.pref_value_book_details_encoding_detected))) {
                    holder.detectedEncodingContainer.setVisibility(View.VISIBLE);
                    isBookDetailDisplayed = true;
                } else {
                    holder.detectedEncodingContainer.setVisibility(View.GONE);
                }

                if (book.getSelectedEncoding() != null) {
                    holder.selectedEncodingContainer.setVisibility(View.VISIBLE);
                } else {
                    holder.selectedEncodingContainer.setVisibility(View.GONE);
                }

                /* If it's a dummy book - change opacity. */
                if (book.isDummy()) {
                    view.setAlpha(0.4f);
                } else {
                    view.setAlpha(1);
                }


                /*
                 * Add some vertical spacing if at least one of the notebook details is displayed.
                 */
                if (isBookDetailDisplayed) {
                    holder.bookDetailsPadding.setVisibility(View.VISIBLE);
                } else {
                    holder.bookDetailsPadding.setVisibility(View.GONE);
                }
            }
        };

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                TextView textView;

                switch (view.getId()) {
                    case R.id.item_book_encoding_used:
                        if (! cursor.isNull(columnIndex)) {
                            textView = (TextView) view;
                            textView.setText(cursor.getString(columnIndex));
                            textView.append(" used");
                        }
                        return true;

                    case R.id.item_book_encoding_detected:
                        if (! cursor.isNull(columnIndex)) {
                            textView = (TextView) view;
                            textView.setText(cursor.getString(columnIndex));
                            textView.append(" detected");
                        }
                        return true;

                    case R.id.item_book_encoding_selected:
                        if (! cursor.isNull(columnIndex)) {
                            textView = (TextView) view;
                            textView.setText(cursor.getString(columnIndex));
                            textView.append(" selected");
                        }
                        return true;

                    /* Generic N/A-if-does-not-exist. */
                    case R.id.item_book_synced_revision:
                        textView = (TextView) view;
                        if (! cursor.isNull(columnIndex)) {
                            textView.setText(cursor.getString(columnIndex));
                        } else {
                            textView.setText("N/A");
                        }
                        return true;

                    case R.id.item_book_synced_mtime:
                        textView = (TextView) view;
                        if (! cursor.isNull(columnIndex) && cursor.getLong(columnIndex) > 0) {
                            /* Format time. */
                            textView.setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(columnIndex))));
                        } else {
                            textView.setText("N/A");
                        }
                        return true;

                    case R.id.item_book_mtime:
                        textView = (TextView) view;
                        if (! cursor.isNull(columnIndex) && cursor.getLong(columnIndex) > 0) {
                            /* Format time. */
                            textView.setText(DateFormat.getDateTimeInstance().format(new Date(cursor.getLong(columnIndex))));
                        } else {
                            textView.setText("Never modified locally");
                        }
                        return true;

                    case R.id.item_book_link_url:
                    case R.id.item_book_synced_url:
                        textView = (TextView) view;
                        if (! cursor.isNull(columnIndex)) {
                            textView.setText(UriUtils.friendlyUri(cursor.getString(columnIndex)));
                        }
                        return true;

                }

                return false;
            }
        });

        return adapter;
    }

    /**
     * Callback for options menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.books_actions, menu);
    }

    /**
     * Callback for options menu.
     * Called after each invalidateOptionsMenu().
     */
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        return true;
//    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.books_options_menu_item_import_book:
                mListener.onBookLoadRequest();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        super.onListItemClick(listView, v, position, id);

        if (mListener != null) {
            mListener.onBookClicked(id);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.books_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
		/* Get ID of the item. */
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        long bookId = info.id;

        switch (item.getItemId()) {
            case R.id.books_context_menu_rename: /* Rename book. */
                mListener.onBookRenameRequest(bookId);
                return true;

            case R.id.books_context_menu_set_link: /* Set link to remote book. */
                mListener.onBookLinkSetRequest(bookId);
                return true;

            case R.id.books_context_menu_force_save:
                mListener.onForceSaveRequest(bookId);
                return true;

            case R.id.books_context_menu_force_load:
                mListener.onForceLoadRequest(bookId);
                return true;

            case R.id.books_context_menu_export: /* Export book. */
                mListener.onBookExportRequest(bookId);
                return true;

            case R.id.books_context_menu_delete: /* Delete book. */
                mListener.onBookDeleteRequest(bookId);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, bundle);

        return BooksClient.getCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (mIsViewCreated) {
            /**
             * Swapping instead of changing Cursor here, to keep the old one open.
             * Loader should release the old Cursor - see note in
             * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
             */
            mListAdapter.swapCursor(cursor);

            if (mListAdapter.getCount() > 0) {
                mNoNotebookText.setVisibility(View.GONE);
            } else {
                mNoNotebookText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mIsViewCreated) {
            mListAdapter.changeCursor(null);
        }
    }

    @Override
    public Runnable getFabAction() {
        return new Runnable() {
            @Override
            public void run() {
                mListener.onBookCreateRequest();
            }
        };
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    FRAGMENT_TAG,
                    getString(R.string.notebooks),
                    null,
                    0); // No books ever selected, as we're using the old floating context menu.
        }
    }

    public interface BooksFragmentListener extends FragmentListener {
        /**
         * Request for creating new book.
         */
        void onBookCreateRequest();

        /**
         * Click on a book item has been performed.
         * @param bookId
         */
        void onBookClicked(long bookId);

        /**
         * User wants to delete the book.
         * @param bookId
         */
        void onBookDeleteRequest(long bookId);

        void onBookRenameRequest(long bookId);
        void onBookLinkSetRequest(long bookId);
        void onForceSaveRequest(long bookId);
        void onForceLoadRequest(long bookId);
        void onBookExportRequest(long bookId);

        void onBookLoadRequest();
    }
}

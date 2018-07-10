package com.orgzly.android.ui.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
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
import com.orgzly.android.provider.views.DbBookViewColumns;
import com.orgzly.android.ui.Fab;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.util.LogUtils;

/**
 * Displays all notebooks.
 * Allows creating new, deleting, renaming, setting links etc.
 */
public class BooksFragment extends ListFragment
        implements
        Fab,
        LoaderManager.LoaderCallbacks<Cursor>,
        DrawerItem {

    private static final String TAG = BooksFragment.class.getName();

    /**
     * Name used for {@link android.app.FragmentManager}.
     */
    public static final String FRAGMENT_TAG = BooksFragment.class.getName();

    private static final String ARG_ADD_OPTIONS = "add_options";
    private static final String ARG_SHOW_CONTEXT_MENU = "show_context_menu";

    private BooksFragmentListener mListener;
    private SimpleCursorAdapter mListAdapter;
    private View mNoNotebookText;
    private boolean mAddOptions = true;
    private boolean mShowContextMenu = true;

    /**
     * If sort order changes, destroy the loader so it is recreated.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = (sharedPreferences, key) -> {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            if (activity.getString(R.string.pref_key_notebooks_sort_order).equals(key)) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity, key);
                activity.getSupportLoaderManager().destroyLoader(Loaders.BOOKS_FRAGMENT);
            }
        }
    };

    public static BooksFragment getInstance() {
        return getInstance(true, true);
    }

    public static BooksFragment getInstance(boolean addOptions, boolean showContextMenu) {
        BooksFragment fragment = new BooksFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_ADD_OPTIONS, addOptions);
        args.putBoolean(ARG_SHOW_CONTEXT_MENU, showContextMenu);

        fragment.setArguments(args);
        return fragment;
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

        parseArguments();
    }

    private void parseArguments() {
        if (getArguments() == null) {
            throw new IllegalArgumentException("No arguments found to " + BooksFragment.class.getSimpleName());
        }

        mAddOptions = getArguments().getBoolean(ARG_ADD_OPTIONS);
        mShowContextMenu = getArguments().getBoolean(ARG_SHOW_CONTEXT_MENU);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(mAddOptions);

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

        /* Request callbacks for Context menu. */
        registerForContextMenu(getListView());

        setListAdapter(mListAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        getActivity().getSupportLoaderManager().initLoader(Loaders.BOOKS_FRAGMENT, null, this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
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

        announceChangesToActivity();
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onDestroy() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroy();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    private SimpleCursorAdapter createAdapter() {
        SimpleCursorAdapter adapter;

        /* Column field names to be bound. */
        String[] columns = {
                ProviderContract.Books.Param.NAME,
                ProviderContract.Books.Param.NAME,
                ProviderContract.Books.Param.MTIME,
                ProviderContract.Books.Param.LAST_ACTION,
                ProviderContract.Books.Param.LINK_REPO_URL,
                ProviderContract.Books.Param.SYNCED_REPO_URL,
                ProviderContract.Books.Param.SYNCED_ROOK_URL,
                ProviderContract.Books.Param.SYNCED_ROOK_REVISION,
                ProviderContract.Books.Param.SYNCED_ROOK_MTIME,
                ProviderContract.Books.Param.USED_ENCODING,
                ProviderContract.Books.Param.DETECTED_ENCODING,
                ProviderContract.Books.Param.SELECTED_ENCODING
        };

        /* Views which the data will be bound to. */
        int[] to = {
                R.id.item_book_title,
                R.id.item_book_subtitle,
                R.id.item_book_mtime,
                R.id.item_book_last_action,
                R.id.item_book_link_repo,
                R.id.item_book_synced_repo,
                R.id.item_book_synced_url,
                R.id.item_book_synced_revision,
                R.id.item_book_synced_mtime,
                R.id.item_book_encoding_used,
                R.id.item_book_encoding_detected,
                R.id.item_book_encoding_selected
        };

        adapter = new SimpleCursorAdapter(getActivity(), R.layout.item_book, null, columns, to, 0) {
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
                TextView noteCount;
                View noteCountContainer;
                TextView lastAction;
                TextView subTitle;
                TextView mtime;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder == null) {
                    holder = new ViewHolder();
                    holder.title = view.findViewById(R.id.item_book_title);
                    holder.modifiedAfterSyncIcon = view.findViewById(R.id.item_book_modified_after_sync_icon);
                    holder.bookDetailsPadding = view.findViewById(R.id.item_book_details_padding);
                    holder.subTitle = view.findViewById(R.id.item_book_subtitle);
                    holder.mtimeContainer = view.findViewById(R.id.item_book_mtime_container);
                    holder.mtime = view.findViewById(R.id.item_book_mtime);
                    holder.linkDetailsContainer = view.findViewById(R.id.item_book_link_container);
                    holder.versionedRookContainer = view.findViewById(R.id.item_book_synced_container);
                    holder.versionedRookUrlContainer = view.findViewById(R.id.item_book_synced_url_container);
                    holder.versionedRookMtimeContainer = view.findViewById(R.id.item_book_synced_mtime_container);
                    holder.versionedRookRevisionContainer = view.findViewById(R.id.item_book_synced_revision_container);
                    holder.lastActionContainer = view.findViewById(R.id.item_book_last_action_container);
                    holder.lastAction = view.findViewById(R.id.item_book_last_action);
                    holder.usedEncodingContainer = view.findViewById(R.id.item_book_encoding_used_container);
                    holder.detectedEncodingContainer = view.findViewById(R.id.item_book_encoding_detected_container);
                    holder.selectedEncodingContainer = view.findViewById(R.id.item_book_encoding_selected_container);
                    holder.noteCount = view.findViewById(R.id.item_book_note_count);
                    holder.noteCountContainer = view.findViewById(R.id.item_book_note_count_container);
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
                ElementPlacer placer = new ElementPlacer();
                placer.displayDetailByCondition(holder.mtimeContainer, isPreferenceActivated(R.string.pref_value_book_details_mtime, context));

                /* If book has no link - remove related rows. */
                if (book.hasLink()) {
                    placer.displayDetailByCondition(holder.linkDetailsContainer, isPreferenceActivated(R.string.pref_value_book_details_link_url, context));
                } else {
                    placer.hideElement(holder.linkDetailsContainer);
                }

                /* If book has no versioned rook - remove all related rows. */
                if (book.getLastSyncedToRook() != null) {
                    placer.showElement(holder.versionedRookContainer);

                    placer.displayDetailByCondition(holder.versionedRookUrlContainer, isPreferenceActivated(R.string.pref_value_book_details_sync_url, context));
                    placer.displayDetailByCondition(holder.versionedRookMtimeContainer, isPreferenceActivated(R.string.pref_value_book_details_sync_mtime, context));
                    placer.displayDetailByCondition(holder.versionedRookRevisionContainer, isPreferenceActivated(R.string.pref_value_book_details_sync_revision, context));
                } else {
                    placer.hideElement(holder.versionedRookContainer);
                }

                /* Hide last action if
                 * - there is no last action (sync) performed
                 *   OR
                 * - action is INFO but user choose not to display it
                 */
                boolean shouldHideLastAction = book.getLastAction() == null || (lastActionWasInfo(book) && !isPreferenceActivated(R.string.pref_value_book_details_last_action, context));
                placer.displayDetailByCondition(holder.lastActionContainer, !shouldHideLastAction);
                if (!shouldHideLastAction) {
                    holder.lastAction.setText(getLastActionText(book));
                }

                /*
                 * Display encodings if set.
                 */
                boolean shouldShowSelectedEncoding = book.getSelectedEncoding() != null && isPreferenceActivated(R.string.pref_value_book_details_encoding_selected, context);
                placer.displayDetailByCondition(holder.selectedEncodingContainer, shouldShowSelectedEncoding);

                boolean shouldShowDetectedEncoding = book.getDetectedEncoding() != null && isPreferenceActivated(R.string.pref_value_book_details_encoding_detected, context);
                placer.displayDetailByCondition(holder.detectedEncodingContainer, shouldShowDetectedEncoding);

                boolean shouldShowUsedEncoding = book.getUsedEncoding() != null && isPreferenceActivated(R.string.pref_value_book_details_encoding_used, context);
                placer.displayDetailByCondition(holder.usedEncodingContainer, shouldShowUsedEncoding);

                /* If it's a dummy book - change opacity. */
                if (book.isDummy()) {
                    view.setAlpha(0.4f);
                } else {
                    view.setAlpha(1);
                }

                placer.displayDetailByCondition(holder.noteCountContainer, isPreferenceActivated(R.string.pref_value_book_details_notes_count, context));

                /* Set notes count. For some reason, there are crashes reported here and there
                 * for some users if NOTES_COUNT is included in SimpleCursorAdapter's column list.
                 */
                int noteCount = cursor.getInt(cursor.getColumnIndexOrThrow(DbBookViewColumns.NOTES_COUNT));
                holder.noteCount.setText(noteCount > 0 ?
                        getResources().getQuantityString(R.plurals.notes_count_nonzero, noteCount, noteCount) :
                        getString(R.string.notes_count_zero));

                /*
                 * Add some vertical spacing if at least one of the notebook details is displayed.
                 */
                placer.displayElementByCondition(holder.bookDetailsPadding, placer.anyDetailWasShown());
            }


            class ElementPlacer {
                private boolean detailWasShown = false;

                private void displayElementByCondition(View element, boolean condition) {
                    element.setVisibility(condition ? View.VISIBLE : View.GONE);
                }

                private void displayDetailByCondition(View detail, boolean condition) {
                    displayElementByCondition(detail, condition);
                    if (condition) {
                        detailWasShown = true;
                    }
                }

                private void showElement(View element) {
                    element.setVisibility(View.VISIBLE);
                }

                private void hideElement(View element) {
                    element.setVisibility(View.GONE);
                }

                boolean anyDetailWasShown() {
                    return detailWasShown;
                }
            }

            private boolean isPreferenceActivated(int preferenceCode, Context context) {
                return AppPreferences.displayedBookDetails(context).contains(getString(preferenceCode));
            }

            private boolean lastActionWasInfo(Book book) {
                return book.getLastAction().getType() == BookAction.Type.INFO;
            }

            private CharSequence getLastActionText(Book book) {
                SpannableStringBuilder builder = new SpannableStringBuilder();

                builder.append(timeString(book.getLastAction().getTimestamp()));
                builder.append(": ");
                int pos = builder.length();
                builder.append(book.getLastAction().getMessage());

                if (book.getLastAction().getType() == BookAction.Type.ERROR) {
                    /* Get error color attribute. */
                    TypedArray arr = getActivity().obtainStyledAttributes(
                            new int[]{R.attr.text_error_color});
                    int color = arr.getColor(0, 0);
                    arr.recycle();

                    /* Set error color. */
                    builder.setSpan(new ForegroundColorSpan(color), pos, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                } else if (book.getLastAction().getType() == BookAction.Type.PROGRESS) {
                    builder.setSpan(new StyleSpan(Typeface.BOLD), pos, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                return builder;
            }
        };

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean hasData = !cursor.isNull(columnIndex);
                String viewContent = null;
                switch (view.getId()) {
                    case R.id.item_book_encoding_used:
                        if (hasData) {
                            viewContent = cursor.getString(columnIndex) + " used";
                        }
                        break;

                    case R.id.item_book_encoding_detected:
                        if (hasData) {
                            viewContent = cursor.getString(columnIndex) + " detected";
                        }
                        break;

                    case R.id.item_book_encoding_selected:
                        if (hasData) {
                            viewContent = cursor.getString(columnIndex) + " selected";
                        }
                        break;

                    /* Generic N/A-if-does-not-exist. */
                    case R.id.item_book_synced_revision:
                        if (hasData) {
                            viewContent = cursor.getString(columnIndex);
                        } else {
                            viewContent = "N/A";
                        }
                        break;

                    case R.id.item_book_synced_mtime:
                        if (hasData && cursor.getLong(columnIndex) > 0) {
                            /* Format time. */
                            viewContent = timeString(cursor.getLong(columnIndex));
                        } else {
                            viewContent = "N/A";
                        }
                        break;

                    case R.id.item_book_mtime:
                        if (hasData && cursor.getLong(columnIndex) > 0) {
                            /* Format time. */
                            viewContent = timeString(cursor.getLong(columnIndex));
                        } else {
                            viewContent = getString(R.string.book_never_modified_locally);
                        }
                        break;

                    case R.id.item_book_link_repo:
                    case R.id.item_book_synced_url:
                        if (hasData) {
                            viewContent = cursor.getString(columnIndex);
                        }
                        break;

                    default:
                        return false;
                }

                if (viewContent != null) {
                    ((TextView) view).setText(viewContent);
                }

                return true;
            }
        });

        return adapter;
    }

    private String timeString(long ts) {
        int flags = DateUtils.FORMAT_SHOW_DATE |
                DateUtils.FORMAT_SHOW_TIME |
                DateUtils.FORMAT_ABBREV_MONTH |
                DateUtils.FORMAT_SHOW_WEEKDAY |
                DateUtils.FORMAT_ABBREV_WEEKDAY;

        return DateUtils.formatDateTime(getContext(), ts, flags);
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
                mListener.onBookImportRequest();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        if (mListener != null) {
            mListener.onBookClicked(id);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (mShowContextMenu) {
            getActivity().getMenuInflater().inflate(R.menu.books_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get ID of the item. */
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader);

        mListAdapter.swapCursor(cursor);

        if (mListAdapter.getCount() > 0) {
            mNoNotebookText.setVisibility(View.GONE);
        } else {
            mNoNotebookText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.changeCursor(null);
    }

    @Override
    public Runnable getFabAction() {
        return () -> {
            if (mListener != null) {
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

    @Override
    public String getCurrentDrawerItemId() {
        return getDrawerItemId();
    }

    public static String getDrawerItemId() {
        return TAG;
    }

    public interface BooksFragmentListener extends FragmentListener {
        /**
         * Request for creating new book.
         */
        void onBookCreateRequest();

        /**
         * Click on a book item has been performed.
         *
         * @param bookId
         */
        void onBookClicked(long bookId);

        /**
         * User wants to delete the book.
         *
         * @param bookId
         */
        void onBookDeleteRequest(long bookId);

        void onBookRenameRequest(long bookId);

        void onBookLinkSetRequest(long bookId);

        void onForceSaveRequest(long bookId);

        void onForceLoadRequest(long bookId);

        void onBookExportRequest(long bookId);

        void onBookImportRequest();
    }
}

package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.models.DbNoteColumns;
import com.orgzly.android.provider.views.DbNoteViewColumns;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryParser;
import com.orgzly.android.query.internal.InternalQueryParser;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.AgendaListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.android.util.AgendaUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AgendaFragment extends NoteListFragment
        implements
        TimestampDialogFragment.OnDateTimeSetListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = AgendaFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = AgendaFragment.class.getName();

    private static final String ARG_QUERY = "query";

    private static final int STATE_ITEM_GROUP = 1;

    /* Currently active query. */
    private String mQuery;

    private UserTimeFormatter userTimeFormatter;

    private AgendaListViewAdapter mListAdapter;

    private NoteListFragmentListener mListener;

    /* Maps agenda's item ID to note ID */
    private LongSparseArray<Long> originalNoteIDs = new LongSparseArray<>();

    int currentLoaderId = -1;

    public static AgendaFragment getInstance(String query) {
        AgendaFragment fragment = new AgendaFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AgendaFragment() {
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
            mListener = (NoteListFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + NoteListFragmentListener.class);
        }
        try {
            mActionModeListener = (ActionModeListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + ActionModeListener.class);
        }

        userTimeFormatter = new UserTimeFormatter(context);

        mQuery = getArguments().getString(ARG_QUERY);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        /* On long click */
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListAdapter.getItemViewType(position) != AgendaListViewAdapter.SEPARATOR_TYPE) {
                    mListener.onNoteLongClick(AgendaFragment.this, view, position, id, originalNoteIDs.get(id));
                }
                return true;
            }
        });

        // noteId needs to be translated since they are different now!

        getListView().setOnItemMenuButtonClickListener(
                new GesturedListView.OnItemMenuButtonClickListener() {
                    @Override
                    public boolean onMenuButtonClick(int buttonId, long noteId) {
                        noteId = originalNoteIDs.get(noteId);
                        switch (buttonId) {
                            case R.id.item_menu_schedule_btn:
                                displayScheduleTimestampDialog(R.id.item_menu_schedule_btn, noteId);
                                break;

                            case R.id.item_menu_prev_state_btn:
                                mListener.onStateCycleRequest(noteId, -1);
                                break;

                            case R.id.item_menu_next_state_btn:
                                mListener.onStateCycleRequest(noteId, 1);
                                break;

                            case R.id.item_menu_done_state_btn:
                                if (AppPreferences.isDoneKeyword(getActivity(), "DONE")) {
                                    Set<Long> set = new TreeSet<>();
                                    set.add(noteId);
                                    mListener.onStateChangeRequest(set, "DONE");
                                }
                                break;

                            case R.id.item_menu_open_btn:
                                mListener.onNoteScrollToRequest(noteId);
                                break;
                        }


                        return false;
                    }
                });

        /* Create a selection. */
        mSelection = new Selection();

        mListAdapter = new AgendaListViewAdapter(getActivity(), mSelection,
                getListView().getItemMenus(), false);

        setListAdapter(mListAdapter);

        /*
         * Restore selected items, now that adapter is set.
         * Saved with {@link Selection#saveSelectedIds(android.os.Bundle, String)}.
         */
        mSelection.restoreIds(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        /* Activity created - context available. Create Shelf and populate list with data. */
        mShelf = new Shelf(getActivity().getApplicationContext());

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());

        loadQuery();
    }

    private void loadQuery() {
        /* TODO: If query did not change - reuse loader. Otherwise - restart it. */
        int id = Loaders.generateLoaderId(Loaders.AGENDA_FRAGMENT, mQuery);
        currentLoaderId = id;
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + mQuery);
        getActivity().getSupportLoaderManager().initLoader(id, null, this);
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

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    AgendaFragment.FRAGMENT_TAG,
                    getString(R.string.fragment_agenda_title),
                    mQuery,
                    mSelection.getCount());
        }
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();

        setListAdapter(null);
        mListAdapter = null;
    }

    @Override
    public void onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDetach();

        mListener = null;
        mActionModeListener = null;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        int itemViewType = mListAdapter.getItemViewType(position);

        if (itemViewType == AgendaListViewAdapter.NOTE_TYPE) {
            mListener.onNoteClick(this, view, position, id, originalNoteIDs.get(id));
        }
    }

    @Override
    public void onDateTimeSet(int id, TreeSet<Long> noteIds, OrgDateTime time) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, time);

        switch (id) {
            case R.id.query_cab_schedule:
            case R.id.item_menu_schedule_btn:
                mListener.onScheduledTimeUpdateRequest(noteIds, time);
                break;
        }
    }

    @Override
    public void onDateTimeCleared(int id, TreeSet<Long> noteIds) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id);

        switch (id) {
            case R.id.query_cab_schedule:
            case R.id.item_menu_schedule_btn:
                mListener.onScheduledTimeUpdateRequest(noteIds,  null);
                break;
        }
    }

    @Override
    public void onDateTimeAborted(int id, TreeSet<Long> noteIds) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id);
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public ActionMode.Callback getNewActionMode() {
        return new MyActionMode();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, bundle);

        return NotesClient.getLoaderForQuery(getActivity(), mQuery);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (loader.getId() != currentLoaderId) {
            LogUtils.d(TAG, "Skip callback from loader " + loader.getId());
            return;
        }

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        QueryParser parser = new InternalQueryParser();
        Query query = parser.parse(mQuery);

        int agendaDays = query.getOptions().getAgendaDays();

        // Add IS_SEPARATOR column
        String[] columnNames = new String[cursor.getColumnNames().length + 1];
        System.arraycopy(cursor.getColumnNames(), 0, columnNames, 0, cursor.getColumnNames().length);
        columnNames[columnNames.length - 1] = Columns.IS_SEPARATOR;

        Map<Long, MatrixCursor> agenda = new LinkedHashMap<>();
        DateTime day = DateTime.now().withTimeAtStartOfDay();
        int i = 0;
        // create entries from today to today+agenda_len
        do {
            MatrixCursor matrixCursor = new MatrixCursor(columnNames);
            agenda.put(day.getMillis(), matrixCursor);
            day = day.plusDays(1);
        } while (++i < agendaDays);

        Calendar now = Calendar.getInstance();

        int scheduledRangeStrIdx = cursor.getColumnIndex(DbNoteViewColumns.SCHEDULED_RANGE_STRING);
        int deadlineRangeStrIdx = cursor.getColumnIndex(DbNoteViewColumns.DEADLINE_RANGE_STRING);

        // expand each note if it has a repeater or is a range

        long nextId = 1;
        originalNoteIDs.clear();

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Set<DateTime> dates = AgendaUtils.expandOrgDateTime(
                    new String[] {
                            cursor.getString(scheduledRangeStrIdx),
                            cursor.getString(deadlineRangeStrIdx)
                    },
                    now,
                    agendaDays);

            for (DateTime date: dates) {
                // create agenda cursors
                MatrixCursor matrixCursor = agenda.get(date.getMillis());
                MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
                for (String col: columnNames) {
                    if (col.equalsIgnoreCase(Columns._ID)) {
                        long noteId = cursor.getLong(cursor.getColumnIndex(col));

                        originalNoteIDs.put(nextId, noteId);

                        rowBuilder.add(nextId++);

                    } else if (col.equalsIgnoreCase(Columns.IS_SEPARATOR)) {
                        rowBuilder.add(0); // Not a separator

                    } else {
                        rowBuilder.add(cursor.getString(cursor.getColumnIndex(col)));
                    }
                }
            }
        }

        MergeCursor mCursor = mergeDates(nextId, agenda);

        /*
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
        mListAdapter.swapCursor(mCursor);

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());
    }

    private MergeCursor mergeDates(long nextId, Map<Long, MatrixCursor> agenda) {
        List<Cursor> allCursors = new ArrayList<>();

        for (long dateMilli: agenda.keySet()) {
            DateTime date = new DateTime(dateMilli);
            MatrixCursor dateCursor = new MatrixCursor(Columns.AGENDA_SEPARATOR_COLS);
            MatrixCursor.RowBuilder dateRow = dateCursor.newRow();
            dateRow.add(nextId++);
            dateRow.add(userTimeFormatter.formatDate(AgendaUtils.buildOrgDateTimeFromDate(date, null)));
            dateRow.add(1); // Separator
            allCursors.add(dateCursor);
            allCursors.add(agenda.get(dateMilli));
        }

        return new MergeCursor(allCursors.toArray(new Cursor[allCursors.size()]));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* Make sure this is visible fragment with adapter set (onDestroyVieW() not called). */
        if (mListAdapter == null) {
            return;
        }

        mListAdapter.changeCursor(null);
    }

    public class MyActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menu);

            /* Inflate a menu resource providing context menu items. */
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.query_cab, menu);

            return true;
        }

        /**
         * Called each time the action mode is shown. Always called after onCreateActionMode,
         * but may be called multiple times if the mode is invalidated.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menu);

            /* Update action mode with number of selected items. */
            actionMode.setTitle(String.valueOf(mSelection.getCount()));

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menuItem);
            TreeSet<Long> selectionIds;
            switch (menuItem.getItemId()) {
                case R.id.query_cab_schedule:
                    selectionIds = originalSelectedIds();
                    if (!selectionIds.isEmpty()) {
                        displayScheduleTimestampDialog(R.id.query_cab_schedule, selectionIds);
                    }
                    break;

                case R.id.query_cab_state:
                    /* Add all known states to menu. */
                    SubMenu subMenu = menuItem.getSubMenu();
                    if (subMenu != null) {
                        subMenu.clear();
                        for (String str: new NoteStateSpinner(getActivity(), null).getValues()) {
                            subMenu.add(STATE_ITEM_GROUP, Menu.NONE, Menu.NONE, str);
                        }
                    }
                    break;

                default:
                    /* Click on one of the state keywords. */
                    if (menuItem.getGroupId() == STATE_ITEM_GROUP) {
                        if (mListener != null) {
                            selectionIds = originalSelectedIds();
                            if (!selectionIds.isEmpty()) {
                                mListener.onStateChangeRequest(selectionIds, menuItem.getTitle().toString());
                            }
                        }
                        return true;
                    }

                    return false; // Not handled.
            }

            return true; // Handled.
        }

        private TreeSet<Long> originalSelectedIds() {
            TreeSet<Long> selectionIds = new TreeSet<>();
            for (Long id: mSelection.getIds()) {
                Long originalId = originalNoteIDs.get(id);
                /*
                 * Original ID might be missing if user selects a note before it's gone
                 * (because of sync re-loading a notebook for example).  Adding null to TreeSet
                 * throws NullPointerException.  TODO: De-select notes and remove this check
                 */
                if (originalId != null) {
                    selectionIds.add(originalId);
                }
            }
            return selectionIds;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mSelection.clearSelection();

            /* List adapter could be null, as we could be destroying the action mode of a fragment
             * which is in back stack. That fragment had its onDestroyView called, where list
             * adapter is set to null.
             */
            if (getListAdapter() != null) {
                getListAdapter().notifyDataSetChanged();
            }

            mActionModeListener.actionModeDestroyed();
        }
    }

    public String getQuery() {
        return mQuery;
    }

    public static class Columns implements BaseColumns, DbNoteColumns {
        public static String IS_SEPARATOR = "is_separator";
        public static String AGENDA_DAY = "day";

        private static final String[] AGENDA_SEPARATOR_COLS = {
                Columns._ID,
                Columns.AGENDA_DAY,
                Columns.IS_SEPARATOR
        };
    }
}
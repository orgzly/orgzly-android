package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.views.NotesView;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AgendaFragment extends NoteListFragment
        implements
        TimestampDialogFragment.OnDateTimeSetListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = DrawerFragment.AgendaItem.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = AgendaFragment.class.getName();

    public static final int DEFAULT_AGENDA_SPINNER_POSITION = 0;

    private int agendaDurationInDays = 0;

    private UserTimeFormatter userTimeFormatter;

    private static String[] agendaSpinnerItems;

    /* Arguments. */
    private static final String ARG_QUERY = "query";
    private static final String ARG_FRESH_INSTANCE = "fresh";

    private static final int STATE_ITEM_GROUP = 1;

    private AgendaListViewAdapter mListAdapter;

    /* Currently active query. */
    private SearchQuery mQuery;

    private NoteListFragmentListener mListener;

    private String mActionModeTag;

    private Map<Long, Long> originalNoteIDs = new HashMap<>();

    private String selectedQuery;

    /* Use first spinner item as default */
    private int spinnerSelection = DEFAULT_AGENDA_SPINNER_POSITION;


    public static AgendaFragment getInstance(String query) {
        AgendaFragment fragment = new AgendaFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putBoolean(ARG_FRESH_INSTANCE, true);

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        parseArguments();

        if (savedInstanceState != null && savedInstanceState.getBoolean("actionModeMove", false)) {
            mActionModeTag = "M";
        }

        agendaSpinnerItems = getContext().getResources().getStringArray(R.array.agenda_duration);
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
                if (mListAdapter.getItemViewType(position) == AgendaListViewAdapter.TYPE_SEPARATOR)
                    return true;
                mListener.onNoteLongClick(AgendaFragment.this, view, position, id);
                return true;
            }
        });

        // noteId needs to be translated since they are different now!

        getListView().setOnItemMenuButtonClickListener(
                new GesturedListView.OnItemMenuButtonClickListener() {
                    @Override
                    public boolean onMenuButtonClick(int buttonId, long noteId) {
                        noteId = originalNoteIDs.get(noteId);
                        Fragment fragment = AgendaFragment.this;
                        switch (buttonId) {
                            case R.id.item_menu_schedule_btn:
                                displayScheduleTimestampDialog(R.id.item_menu_schedule_btn, noteId);
                                // refresh fragment, this way swipe menu is closed
                                getFragmentManager().beginTransaction()
                                        .detach(fragment)
                                        .attach(fragment)
                                        .commit();
                                break;

                            case R.id.item_menu_prev_state_btn:
                                mListener.onStateCycleRequest(noteId, -1);
                                // refresh fragment, this way swipe menu is closed
                                getFragmentManager().beginTransaction()
                                        .detach(fragment)
                                        .attach(fragment)
                                        .commit();
                                break;

                            case R.id.item_menu_next_state_btn:
                                mListener.onStateCycleRequest(noteId, 1);
                                // refresh fragment, this way swipe menu is closed
                                getFragmentManager().beginTransaction()
                                        .detach(fragment)
                                        .attach(fragment)
                                        .commit();
                                break;

                            case R.id.item_menu_done_state_btn:
                                if (AppPreferences.isDoneKeyword(getActivity(), "DONE")) {
                                    Set<Long> set = new TreeSet<>();
                                    set.add(noteId);
                                    mListener.onStateChangeRequest(set, "DONE");
                                }
                                // refresh fragment, this way swipe menu is closed
                                getFragmentManager().beginTransaction()
                                        .detach(fragment)
                                        .attach(fragment)
                                        .commit();
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

        /**
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

        mActionModeListener.updateActionModeForSelection(mSelection, new MyActionMode());

        loadQuery();
    }

    private void loadQuery() {
        /* If query did not change - reuse loader. Otherwise - restart it. */
        String newQuery = mQuery.toString();
        int id = Loaders.generateLoaderId(Loaders.AGENDA_FRAGMENT, newQuery);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + newQuery);
        getActivity().getSupportLoaderManager().restartLoader(id, null, this);
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
                    mQuery.toString(),
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

    private void parseArguments() {
        if (getArguments() == null) {
            throw new IllegalArgumentException("No arguments found to " + AgendaFragment.class.getSimpleName());
        }
        String query = getArguments().getString(ARG_QUERY);

        parseQuery(query);
    }

    private void parseQuery(String query) {
        mQuery = new SearchQuery(query);
        agendaDurationInDays = Query.getAgendaDurationInDays(mQuery.toString());
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        int itemViewType = mListAdapter.getItemViewType(position);
        if (itemViewType == AgendaListViewAdapter.TYPE_NOTE) {
            id = originalNoteIDs.get(id);
            mListener.onNoteClick(this, view, position, id);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.agenda_spinner, menu);

        /* Remove search item. */
        MenuItem menuItem = menu.findItem(R.id.activity_action_search);
        menuItem.collapseActionView();
        menu.removeItem(R.id.activity_action_search);

        MenuItem item = menu.findItem(R.id.agenda_spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.agenda_duration,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(spinnerSelection);
        spinner.setOnItemSelectedListener(this);
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

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        Map<Long, MatrixCursor> agenda = new LinkedHashMap<>();
        // add is_separator column to the cursors
        String[] cols = new String[cursor.getColumnNames().length + 1];
        System.arraycopy(cursor.getColumnNames(), 0, cols, 0, cursor.getColumnNames().length);
        cols[cols.length - 1] = Columns.IS_SEPARATOR;
        DateTime day = DateTime.now().withTimeAtStartOfDay();
        int i = 0;
        // available ids for copied notes
        long nextID = Long.MAX_VALUE;
        // create entries from today to today+agenda_len
        do {
            MatrixCursor matrixCursor = new MatrixCursor(cols);
            agenda.put(day.getMillis(), matrixCursor);
            day = day.plusDays(1);
        } while (++i < agendaDurationInDays);

        Calendar now = Calendar.getInstance();
        int scheduledRangeStrIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_RANGE_STRING);
        // expand each note if it has a repeater or is a range
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String scheduledRangeStr = cursor.getString(scheduledRangeStrIdx);
            List<DateTime> dates = AgendaUtils.expandOrgDateTime(scheduledRangeStr, now, agendaDurationInDays);
            for (DateTime date: dates) {
                // clone the item
                MatrixCursor matrixCursor = agenda.get(date.withTimeAtStartOfDay().getMillis());
                MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
                for (String col: cols) {
                    if (col.equalsIgnoreCase(Columns._ID)) {
                        // record the mapping from agenda note ID to original note ID
                        long noteId = cursor.getLong(cursor.getColumnIndex(col));
                        originalNoteIDs.put(nextID, noteId);
                        rowBuilder.add(nextID--);
                    } else if (col.equalsIgnoreCase(Columns.IS_SEPARATOR)) {
                        // notes are not separators!
                        rowBuilder.add(0);
                    } else {
                        rowBuilder.add(cursor.getString(cursor.getColumnIndex(col)));
                    }
                }
            }
        }
        // merge all together
        List<Cursor> allCursors = new ArrayList<>();
        for(long dateMilli: agenda.keySet()) {
            DateTime date = new DateTime(dateMilli);
            MatrixCursor dateCursor = new MatrixCursor(Columns.AGENDA_SEPARATOR_COLS);
            MatrixCursor.RowBuilder dateRow = dateCursor.newRow();
            // use date as number of seconds as id
            int id = (int) (dateMilli / 1000);
            dateRow.add(id);
            dateRow.add(userTimeFormatter.formatDate(AgendaUtils.buildOrgDateTimeFromDate(date, null)));
            dateRow.add(1);
            allCursors.add(dateCursor);
            allCursors.add(agenda.get(dateMilli));
        }
        MergeCursor mCursor = new MergeCursor(allCursors.toArray(new Cursor[allCursors.size()]));

        mCursor.moveToFirst();
        /**
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
        mListAdapter.swapCursor(mCursor);

        mActionModeListener.updateActionModeForSelection(mSelection, new MyActionMode());

        ActionMode actionMode = mActionModeListener.getActionMode();
        if (mActionModeTag != null) {
            actionMode.setTag("M");
            actionMode.invalidate();
            mActionModeTag = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* Make sure this is visible fragment with adapter set (onDestroyVieW() not called). */
        if (mListAdapter == null) {
            return;
        }

        mListAdapter.changeCursor(null);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedStr = agendaSpinnerItems[position];
        spinnerSelection = position;

        if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_day))) {
            selectedQuery = Query.AGENDA_DAY_QUERY;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_week))) {
            selectedQuery = Query.AGENDA_WEEK_QUERY;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_fortnight))) {
            selectedQuery = Query.AGENDA_FORTNIGHT_QUERY;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.thirty_days))) {
            selectedQuery = Query.AGENDA_THIRTY_DAYS_QUERY;
        } else {
            throw new RuntimeException("Invalid agenda period item selected!");
        }
        reloadAgenda();
    }

    private void reloadAgenda() {
        parseQuery(selectedQuery);
        loadQuery();
        announceChangesToActivity();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // nothing to do here!
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
                    selectionIds = new TreeSet<>();
                    for(Long id: mSelection.getIds())
                        selectionIds.add(originalNoteIDs.get(id));
                    displayScheduleTimestampDialog(R.id.query_cab_schedule, selectionIds);
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
                            selectionIds = new TreeSet<>();
                            for(Long id: mSelection.getIds())
                                selectionIds.add(originalNoteIDs.get(id));
                            mListener.onStateChangeRequest(selectionIds, menuItem.getTitle().toString());
                        }
                        return true;
                    }

                    return false; // Not handled.
            }

            return true; // Handled.
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

    public SearchQuery getQuery() {
        return mQuery;
    }

    public static class Columns implements BaseColumns, DbNote.Columns {
        public static String IS_SEPARATOR = "is_separator";
        public static String AGENDA_DAY = "day";

        public static final String[] AGENDA_SEPARATOR_COLS = new String[]{
                Columns._ID,
                Columns.AGENDA_DAY,
                Columns.IS_SEPARATOR};
    }

    public static class Query {
        public static final String AGENDA_DAY_QUERY = ".i.done s.tomorrow";
        public static final String AGENDA_WEEK_QUERY = ".i.done s.1w";
        public static final String AGENDA_FORTNIGHT_QUERY = ".i.done s.2w";
        public static final String AGENDA_THIRTY_DAYS_QUERY = ".i.done s.30d";

        public static final String DEFAULT_AGENDA_QUERY = AGENDA_DAY_QUERY;

        public static int getAgendaDurationInDays(String query) {
            switch (query) {
                case AGENDA_DAY_QUERY:
                    return 1;
                case AGENDA_WEEK_QUERY:
                    return 7;
                case AGENDA_FORTNIGHT_QUERY:
                    return 14;
                case AGENDA_THIRTY_DAYS_QUERY:
                    return 30;
            }
            throw new IllegalStateException("Invalid agenda query!");
        }
    }
}
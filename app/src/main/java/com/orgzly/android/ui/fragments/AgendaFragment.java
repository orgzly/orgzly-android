package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
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
import com.orgzly.android.provider.views.NotesView;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.AgendaListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.android.util.AgendaHelper;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    public static final String AGENDA_QUERY_DAY = ".i.done s.tomorrow";
    public static final String AGENDA_QUERY_WEEK = ".i.done s.1w";
    public static final String AGENDA_QUERY_FORTNIGHT = ".i.done s.2w";
    public static final String AGENDA_QUERY_MONTH = ".i.done s.30d";

    private int agendaDays = 0;

    private UserTimeFormatter userTimeFormatter;

    private static String[] agendaPeriods;

    /* Arguments. */
    private static final String ARG_QUERY = "query";
    private static final String ARG_FRESH_INSTANCE = "fresh";

    private static final int STATE_ITEM_GROUP = 1;

    private AgendaListViewAdapter mListAdapter;

    /* Currently active query. */
    private SearchQuery mQuery;

    private NoteListFragmentListener mListener;
    private AgendaFragmentListener mAgendaListener;

    private String mActionModeTag;

//    private ViewFlipper mViewFlipper;


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

        try {
            mAgendaListener = (AgendaFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + AgendaFragmentListener.class);
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

        agendaPeriods = getContext().getResources().getStringArray(R.array.agenda_periods);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_agenda, container, false);

//        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_agenda_view_flipper);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        /* On long click */
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListAdapter.getItemViewType((int) id) == AgendaListViewAdapter.TYPE_SEPARATOR)
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
                        // refresh fragment
                        Fragment fragment = AgendaFragment.this;
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.detach(fragment).attach(fragment).commit();

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

        /* If query did not change - reuse loader. Otherwise - restart it. */
        String newQuery = mQuery.toString();
        int id = Loaders.generateLoaderId(Loaders.AGENDA_FRAGMENT, newQuery);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + newQuery);
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
                    null, //mQuery.toString(),
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

        mQuery = new SearchQuery(getArguments().getString(ARG_QUERY));

        switch (mQuery.toString()) {
            case AGENDA_QUERY_DAY:
                agendaDays = 1;
                break;
            case AGENDA_QUERY_WEEK:
                agendaDays = 7;
                break;
            case AGENDA_QUERY_FORTNIGHT:
                agendaDays = 14;
                break;
            case AGENDA_QUERY_MONTH:
                agendaDays = 30;
                break;
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (id > Integer.MAX_VALUE)
            throw new RuntimeException("Cannot cast row ID to int!");
        if (mListAdapter.getItemViewType((int) id) == AgendaListViewAdapter.TYPE_ITEM)
            mListener.onNoteClick(this, view, position, id);
    }

    /**
     * Request small animation for the note in the list.
     */
//    public void animateNotes(Set<Long> noteIds, int type) {
//        getListAdapter().animateNotes(noteIds, type);
//
//        /*
//         * After scheduling ids to be animated, must force bindView() to be called.
//         * Can't rely on scheduling happening before data is being updated and content provider
//         * notifying about the change.
//         */
//        getListAdapter().notifyDataSetChanged();
//    }

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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.agenda_periods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(1, false);
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

//    private MatrixCursor cloneCursor(Cursor cursor, OrgDateTime schedTime) {
//        String[] cols = cursor.getColumnNames();
//        MatrixCursor cloned = new MatrixCursor(cols);
//        MatrixCursor.RowBuilder b = cloned.newRow();
//        for(String col: cols) {
//            if(col.equals(NotesView.Columns.SCHEDULED_TIME_STRING))
//                b.add(schedTime.toString());
//            else
//                b.add(cursor.getString(cursor.getColumnIndex(col)));
//        }
//        return cloned;
//    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        // generate agenda days
        // keep all expanded scheduled events in a LinkedHashMap
        Map<Date, MatrixCursor> agenda = new LinkedHashMap<>();
        // create entries from today to today+agenda_len
        // for each cursor: expand shedRange, add agenda.entrySched to agenda[agenda.agendaDate]
        String[] cols = cursor.getColumnNames();
        Calendar day = AgendaHelper.getTodayDate();
        int i = 0;
        do {
            MatrixCursor matrixCursor = new MatrixCursor(cols);
            agenda.put(day.getTime(), matrixCursor);
            day.add(Calendar.DAY_OF_YEAR, 1);
        } while (++i < agendaDays);

        int schedRangeIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_RANGE_STRING);
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String schedRangeStr = cursor.getString(schedRangeIdx);
            List<Date> dates = AgendaHelper.expandOrgRange(schedRangeStr, Calendar.getInstance(), agendaDays);
            for (Date date: dates) {
                // clone the item
                MatrixCursor matrixCursor = agenda.get(date);
                MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
//                System.out.println("ID: " + cursor.getString(cursor.getColumnIndex(BaseColumns._ID)));
                for (String col: cols) {
                    rowBuilder.add(cursor.getString(cursor.getColumnIndex(col)));
                }
            }
        }
        // merge all together
        List<Cursor> allCursors = new ArrayList<>();
        for(Date date: agenda.keySet()) {
            MatrixCursor dateCursor = new MatrixCursor(new String[]{BaseColumns._ID, "day", "separator"});
            MatrixCursor.RowBuilder dateRow = dateCursor.newRow();
            // use date as number of secodns as id
            int id = (int) (date.getTime() / 1000);
            dateRow.add(id);
            dateRow.add(userTimeFormatter.formatDate(AgendaHelper.buildOrgDateTimeFromDate(date)));
            dateRow.add(1);
            allCursors.add(dateCursor);
            mListAdapter.addSeparatorItem(id);
            allCursors.add(agenda.get(date));
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
        System.out.println("ITEM SELECTED ********************");
        String selectedStr = agendaPeriods[position];
        String selectedQuery;
        // TODO: currently relies on comparing the queries, might not be a good idea
        if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_day))) {
            System.out.println(selectedStr);
            selectedQuery = AGENDA_QUERY_DAY;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_week))) {
            System.out.println(selectedStr);
            selectedQuery = AGENDA_QUERY_WEEK;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_fortnight))) {
            System.out.println(selectedStr);
            selectedQuery = AGENDA_QUERY_FORTNIGHT;
        } else if (selectedStr.equalsIgnoreCase(getResources().getString(R.string.agenda_month))) {
            System.out.println(selectedStr);
            selectedQuery = AGENDA_QUERY_MONTH;
        } else {
            throw new RuntimeException("Invalid agenda period item selected!");
        }
        mAgendaListener.onAgendaPeriodChanged(selectedQuery);
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

            switch (menuItem.getItemId()) {
                case R.id.query_cab_schedule:
                    displayScheduleTimestampDialog(R.id.query_cab_schedule, mSelection.getIds());
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
                            mListener.onStateChangeRequest(mSelection.getIds(), menuItem.getTitle().toString());
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

    public interface AgendaFragmentListener {
        void onAgendaPeriodChanged(String query);
    }
}
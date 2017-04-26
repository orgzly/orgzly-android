package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.views.NotesView;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.AgendaListViewAdapter;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.android.util.LogUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgInterval;
import com.orgzly.org.datetime.OrgRange;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;


public class AgendaFragment extends NoteListFragment
        implements
        TimestampDialogFragment.OnDateTimeSetListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = DrawerFragment.AgendaItem.class.getName();

    // TODO: add fromCal to OrgDateTime or make the format public
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd EEE", Locale.ENGLISH);

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = AgendaFragment.class.getName();
    public static final String DEFAULT_AGENDA_QUERY = ".i.done s.7d";

    /* Arguments. */
    private static final String ARG_QUERY = "query";
    private static final String ARG_FRESH_INSTANCE = "fresh";

    private static final int STATE_ITEM_GROUP = 1;

    private AgendaListViewAdapter mListAdapter;

    /* Currently active query. */
    private SearchQuery mQuery;

    private NoteListFragmentListener mListener;

    private String mActionModeTag;

    private ViewFlipper mViewFlipper;


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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        parseArguments();

        if (savedInstanceState != null && savedInstanceState.getBoolean("actionModeMove", false)) {
            mActionModeTag = "M";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_agenda, container, false);

        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_agenda_view_flipper);

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
                mListener.onNoteLongClick(AgendaFragment.this, view, position, id);
                return true;
            }
        });

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

                        return false;
                    }
                });

        /* Create a selection. */
        mSelection = new Selection();

        mListAdapter = new AgendaListViewAdapter(getActivity(), mSelection,
                getListView().getItemMenus(), false, mQuery);

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

        mQuery = new SearchQuery(getArguments().getString(ARG_QUERY));
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (mListAdapter.getItemViewType(position) == AgendaListViewAdapter.TYPE_ITEM)
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

    private MatrixCursor cloneCursor(Cursor cursor, OrgDateTime schedTime) {
        String[] cols = cursor.getColumnNames();
        MatrixCursor cloned = new MatrixCursor(cols);
        MatrixCursor.RowBuilder b = cloned.newRow();
        for(String col: cols) {
            if(col.equals(NotesView.Columns.SCHEDULED_TIME_STRING))
                b.add(schedTime.toString());
            else
                b.add(cursor.getString(cursor.getColumnIndex(col)));
        }
        return cloned;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        MatrixCursor header = new MatrixCursor(new String[]{BaseColumns._ID, "day", "separator"});
        MatrixCursor.RowBuilder headerRow = header.newRow();
        headerRow.add(-1);
        headerRow.add(getQuery().getScheduled().toString());
        headerRow.add(1);

        MatrixCursor extras = null;
        cursor.moveToPosition(-1);
        int schedTimeIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_TIME_STRING);
        int schedRangeIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_RANGE_STRING);
        int schedTimeEndIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_TIME_END_STRING);
        int schedTimestampIdx = cursor.getColumnIndex(NotesView.Columns.SCHEDULED_TIME_TIMESTAMP);
        while(cursor.moveToNext()) {
            System.out.println("position: " + cursor.getPosition());
            System.out.println("sched time: " + cursor.getString(schedTimeIdx));
            System.out.println("sched time range: " + cursor.getString(schedRangeIdx));
            String rangeStr = cursor.getString(schedRangeIdx);
            // normal timestamp, range or repeater
            if (rangeStr.contains("--")) {
                OrgRange range = OrgRange.getInstanceOrNull(rangeStr);
                if( range != null) {
                    OrgDateTime end = range.getEndTime();
                    System.out.println("range end: " + end);
                    OrgDateTime start = range.getStartTime();
                    Calendar cal = range.getStartTime().getCalendar();
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    start = OrgDateTime.getInstance(DATE_FORMAT.format(cal.getTime()));
                    extras = cloneCursor(cursor, start);
                    System.out.println("start+1: " + start);
                }
            }
            System.out.println("sched time end: " + cursor.getString(schedTimeEndIdx));
            System.out.println("sched timestamp:" + cursor.getString(schedTimestampIdx));
        }

        MatrixCursor tail = new MatrixCursor(new String[]{BaseColumns._ID, "day", "separator"});
        MatrixCursor.RowBuilder endRow = tail.newRow();
        endRow.add(-1);
        endRow.add("29-Jul Mon");
        endRow.add(1);

        cursor.moveToFirst();
        MergeCursor mCursor = new MergeCursor(new Cursor[]{cursor, header, extras, tail});

        // go through expanded and sorted cursors and collect separators
        for(mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            int idx = mCursor.getColumnIndex("separator");
            if (idx != -1 && mCursor.getInt(idx) > 0)
                mListAdapter.addSeparatorItem(mCursor.getPosition());
        }


        System.out.println("************************");
        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            switch (mListAdapter.getItemViewType(mCursor.getPosition())) {
                case AgendaListViewAdapter.TYPE_ITEM:
                    System.out.println(mCursor.getPosition() + " ) " + cursor.getString(schedTimeIdx));
                    break;
                case AgendaListViewAdapter.TYPE_SEPARATOR:
                    int idx = mCursor.getColumnIndex("day");
                    System.out.println(mCursor.getPosition() + " -- " + cursor.getString(idx));
            }


        }
        System.out.println("************************");
        mCursor.moveToFirst();

        /**
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
//        mListAdapter.swapCursor(cursor);
        mListAdapter.swapCursor(mCursor);

        mActionModeListener.updateActionModeForSelection(mSelection, new MyActionMode());

        ActionMode actionMode = mActionModeListener.getActionMode();
        if (mActionModeTag != null) {
            actionMode.setTag("M");
            actionMode.invalidate();
            mActionModeTag = null;
        }

        if (mListAdapter.getCount() > 0) {
            mViewFlipper.setDisplayedChild(0);
        } else {
            mViewFlipper.setDisplayedChild(1);
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
}
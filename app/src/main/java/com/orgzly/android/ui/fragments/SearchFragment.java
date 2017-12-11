package com.orgzly.android.ui.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.util.LogUtils;

import java.util.Set;
import java.util.TreeSet;

/**
 * Displays search results.
 */
public class SearchFragment extends QueryFragment {
    private static final String TAG = SearchFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SearchFragment.class.getName();


    private String mActionModeTag;

    private ViewFlipper mViewFlipper;


    public static QueryFragment getInstance(String query) {
        QueryFragment fragment = new SearchFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.getBoolean("actionModeMove", false)) {
            mActionModeTag = "M";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_query, container, false);

        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_query_view_flipper);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        /* On long click */
        getListView().setOnItemLongClickListener((parent, view1, position, id) -> {
            mListener.onNoteLongClick(SearchFragment.this, view1, position, id, id);
            return true;
        });

        getListView().setOnItemMenuButtonClickListener(
                (buttonId, noteId) -> {
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
                });

        /* Create a selection. */
        mSelection = new Selection();

        mListAdapter = new HeadsListViewAdapter(getActivity(), mSelection, getListView().getItemMenus(), false);

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

        /* TODO: If query did not change - reuse loader. Otherwise - restart it. */
        int id = Loaders.generateLoaderId(Loaders.QUERY_FRAGMENT, mQuery);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + mQuery);
        getActivity().getSupportLoaderManager().initLoader(id, null, this);
    }

    @Override
    protected void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    SearchFragment.FRAGMENT_TAG,
                    getString(R.string.fragment_query_title),
                    mQuery,
                    mSelection.getCount());
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        mListener.onNoteClick(this, view, position, id, id);
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
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader, cursor);

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        /*
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
        mListAdapter.swapCursor(cursor);

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());

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

    protected class MyActionMode extends QueryFragment.MyActionMode {
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
    }
}

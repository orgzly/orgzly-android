package com.orgzly.android.ui.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.SelectionUtils;
import com.orgzly.android.util.LogUtils;

/**
 * Displays search results.
 */
public class SearchFragment extends QueryFragment {
    private static final String TAG = SearchFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SearchFragment.class.getName();

    private ViewFlipper mViewFlipper;


    public static QueryFragment getInstance(String query) {
        QueryFragment fragment = new SearchFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_query_search, container, false);

        mViewFlipper = view.findViewById(R.id.fragment_query_search_view_flipper);

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
                (itemView, buttonId, noteId) ->
                        onButtonClick(mListener, itemView, buttonId, noteId));

        /* Create a selection. */
        mSelection = new Selection();

        mListAdapter = new HeadsListViewAdapter(
                getActivity(), mSelection, getListView().getItemMenus(), false);

        setListAdapter(mListAdapter);

        /*
         * Restore selected items, now that adapter is set.
         * Saved with {@link Selection#saveSelectedIds(android.os.Bundle, String)}.
         */
        mSelection.restoreIds(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int id = Loaders.generateLoaderId(Loaders.QUERY_FRAGMENT, mQuery);
        getActivity().getSupportLoaderManager().initLoader(id, null, this);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + mQuery);
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
    public ActionMode.Callback getNewActionMode() {
        return new MyActionMode();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader);

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        SelectionUtils.removeNonExistingIdsFromSelection(mSelection, cursor);

        mListAdapter.swapCursor(cursor);

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());

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
                    displayTimestampDialog(R.id.query_cab_schedule, mSelection.getIds());
                    break;

                case R.id.query_cab_deadline:
                    displayTimestampDialog(R.id.query_cab_deadline, mSelection.getIds());
                    break;

                case R.id.query_cab_state:
                    openNoteStateDialog(mListener, mSelection.getIds(), null);
                    break;
            }

            return true; // Handled.
        }
    }
}

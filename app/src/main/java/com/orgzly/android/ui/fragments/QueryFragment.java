package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.util.LogUtils;

/**
 * Displays search results.
 */
abstract public class QueryFragment extends NoteListFragment
        implements
        TimestampDialogFragment.OnDateTimeSetListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        DrawerItem {

    private static final String TAG = QueryFragment.class.getName();

    /** Arguments. */
    protected static final String ARG_QUERY = "query";

    protected SimpleCursorAdapter mListAdapter;

    protected NoteListFragmentListener mListener;

    /** Currently active query. */
    protected String mQuery;

    @Override
    public NoteListFragmentListener getListener() {
        return mListener;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QueryFragment() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
    }

    @NonNull
    @Override
    public String getCurrentDrawerItemId() {
        return getDrawerItemId(getQuery());
    }

    public static String getDrawerItemId(String query) {
        return TAG + " " + query;
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

        mQuery = getArguments().getString(ARG_QUERY);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), getNewActionMode());
    }


    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();
    }

    abstract void announceChangesToActivity();

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
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id, bundle);
        return NotesClient.getLoaderForQuery(getActivity(), mQuery);
    }

    @Override
    abstract public void onLoadFinished(Loader<Cursor> loader, Cursor data);

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* Make sure this is visible fragment with adapter set (onDestroyVieW() not called). */
        if (mListAdapter == null) {
            return;
        }

        mListAdapter.changeCursor(null);
    }

    @Override
    abstract public ActionMode.Callback getNewActionMode();

    protected abstract class MyActionMode implements ActionMode.Callback {
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
}

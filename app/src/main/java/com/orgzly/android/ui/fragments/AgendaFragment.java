package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.AgendaCursor;
import com.orgzly.android.ui.AgendaListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.NoteStates;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.util.LogUtils;

import java.util.TreeSet;


public class AgendaFragment extends QueryFragment {
    private static final String TAG = AgendaFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = AgendaFragment.class.getName();

    /* Maps agenda's item ID to note ID */
    private LongSparseArray<Long> originalNoteIDs = new LongSparseArray<>();

    int currentLoaderId = -1;


    public static QueryFragment getInstance(String query) {
        QueryFragment fragment = new AgendaFragment();

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_query_agenda, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view, savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        /* On long click */
        getListView().setOnItemLongClickListener((parent, view1, position, id) -> {
            if (mListAdapter.getItemViewType(position) != AgendaListViewAdapter.DIVIDER_VIEW_TYPE) {
                mListener.onNoteLongClick(AgendaFragment.this, view1, position, id, originalNoteIDs.get(id));
            }
            return true;
        });

        // noteId needs to be translated since they are different now!

        getListView().setOnItemMenuButtonClickListener(
                (buttonId, noteId) -> {
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
                            mListener.onStateFlipRequest(noteId);
                            break;

                        case R.id.item_menu_open_btn:
                            mListener.onNoteScrollToRequest(noteId);
                            break;
                    }


                    return false;
                });

        /* Create a selection. */
        mSelection = new Selection();

        mListAdapter = new AgendaListViewAdapter(
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

        // Restart loader in case of settings change which affect results (e.g. default priority)
        int id = Loaders.generateLoaderId(Loaders.AGENDA_FRAGMENT, mQuery);
        currentLoaderId = id;
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + id + " for: " + mQuery);
        getActivity().getSupportLoaderManager().restartLoader(id, null, this);
    }

    @Override
    protected void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    AgendaFragment.FRAGMENT_TAG,
                    getString(R.string.agenda),
                    mQuery,
                    mSelection.getCount());
        }
    }


    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        int itemViewType = mListAdapter.getItemViewType(position);

        if (itemViewType == AgendaListViewAdapter.NOTE_VIEW_TYPE) {
            mListener.onNoteClick(this, view, position, id, originalNoteIDs.get(id));
        }
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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader);

        if (loader.getId() != currentLoaderId) {
            LogUtils.d(TAG, "Skip callback from loader " + loader.getId());
            return;
        }

        if (mListAdapter == null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "adapter is null, view is destroyed?");
            return;
        }

        AgendaCursor.AgendaMergedCursor agendaMergedCursor =
                AgendaCursor.INSTANCE.create(getContext(), cursor, getQuery());

        Cursor mergedCursor = agendaMergedCursor.getCursor();
        originalNoteIDs = agendaMergedCursor.getOriginalNoteIDs();

        /*
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
        mListAdapter.swapCursor(mergedCursor);

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());
    }



    protected class MyActionMode extends QueryFragment.MyActionMode {
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
                        subMenu.add(STATE_ITEM_GROUP, Menu.NONE, Menu.NONE, NoteStates.NO_STATE_KEYWORD);
                        for (String str: NoteStates.Companion.fromPreferences(getActivity()).getArray()) {
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
    }

}
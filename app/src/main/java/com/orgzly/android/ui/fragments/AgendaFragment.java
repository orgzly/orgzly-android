package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.AgendaCursor;
import com.orgzly.android.ui.AgendaListViewAdapter;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.util.LogUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class AgendaFragment extends QueryFragment {
    private static final String TAG = AgendaFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = AgendaFragment.class.getName();

    /* Maps agenda's item ID to real note ID */
    private Map<Long, AgendaCursor.NoteForDay> originalNoteIDs = new HashMap<>();

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
            if (mListAdapter != null && mListAdapter.getItemViewType(position) != AgendaListViewAdapter.DIVIDER_VIEW_TYPE) {
                mListener.onNoteLongClick(AgendaFragment.this, view1, position, id, originalNoteIDs.get(id).getNoteId());
            }
            return true;
        });

        getListView().setOnItemMenuButtonClickListener(
                (itemView, buttonId, noteId) ->
                        // Pass the original note ID
                        onButtonClick(mListener, itemView, buttonId, originalNoteIDs.get(noteId).getNoteId()));

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

        currentLoaderId = Loaders.generateLoaderId(Loaders.AGENDA_FRAGMENT, mQuery);
        getActivity().getSupportLoaderManager().initLoader(currentLoaderId, null, this);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loader #" + currentLoaderId + " for: " + mQuery);
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
        if (mListAdapter != null) {
            int itemViewType = mListAdapter.getItemViewType(position);

            if (itemViewType == AgendaListViewAdapter.NOTE_VIEW_TYPE) {
                mListener.onNoteClick(this, view, position, id, originalNoteIDs.get(id).getNoteId());
            }
        }
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
                AgendaCursor.create(getContext(), cursor, getQuery());


        Long openedQuickMenuId = getListView().getItemMenus().getOpenedId();
        if (openedQuickMenuId != null) {
            if (isSameNote(openedQuickMenuId, agendaMergedCursor)) {
                getListView().getItemMenus().closeAll();
            }
        }


        Set<Long> nonExistingSelectedIds = new HashSet<>();
        for (Long id: mSelection.getIds()) {
            if (isSameNote(id, agendaMergedCursor)) {
                nonExistingSelectedIds.add(id);
            }
        }
        mSelection.deselectAll(nonExistingSelectedIds);


        originalNoteIDs = agendaMergedCursor.getOriginalNoteIDs();
        mListAdapter.swapCursor(agendaMergedCursor.getCursor());

        mActionModeListener.updateActionModeForSelection(mSelection.getCount(), new MyActionMode());
    }

    private boolean isSameNote(long id, AgendaCursor.AgendaMergedCursor agendaMergedCursor) {
        AgendaCursor.NoteForDay prevSelected = originalNoteIDs.get(id);
        AgendaCursor.NoteForDay currSelected = agendaMergedCursor.getOriginalNoteIDs().get(id);
        return currSelected == null || prevSelected == null || !prevSelected.equals(currSelected);
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
                        displayTimestampDialog(R.id.query_cab_schedule, selectionIds);
                    }
                    break;

                case R.id.query_cab_deadline:
                    selectionIds = originalSelectedIds();
                    if (!selectionIds.isEmpty()) {
                        displayTimestampDialog(R.id.query_cab_deadline, selectionIds);
                    }
                    break;

                case R.id.query_cab_state:
                    openNoteStateDialog(mListener, originalSelectedIds(), null);
                    break;
            }

            return true; // Handled.
        }

        private TreeSet<Long> originalSelectedIds() {
            TreeSet<Long> selectionIds = new TreeSet<>();
            for (Long id: mSelection.getIds()) {
                AgendaCursor.NoteForDay originalId = originalNoteIDs.get(id);
                long noteId = originalId.getNoteId();
                selectionIds.add(noteId);
            }
            return selectionIds;
        }
    }

}
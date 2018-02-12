package com.orgzly.android.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.view.ActionMode;
import android.view.View;

import com.orgzly.R;
import com.orgzly.android.Note;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.NoteStates;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.dialogs.NoteStateDialog;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.org.datetime.OrgDateTime;

import java.util.Set;
import java.util.TreeSet;

/**
 * Fragment which is displaying a list of notes,
 * such as {@link BookFragment}, {@link SearchFragment} or {@link AgendaFragment}.
 */
public abstract class NoteListFragment extends ListFragment {

    protected Shelf mShelf;
    protected Selection mSelection;
    protected ActionModeListener mActionModeListener;

    public abstract String getFragmentTag();

    public abstract ActionMode.Callback getNewActionMode();

    protected AlertDialog dialog;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mShelf = new Shelf(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /* Selection could be null if fragment is in back stack and there's configuration change. */
        if (mSelection != null) {
            mSelection.saveIds(outState);
        }

        /* Save action mode state (move mode). */
        if (mActionModeListener != null) {
            ActionMode actionMode = mActionModeListener.getActionMode();
            outState.putBoolean("actionModeMove", actionMode != null && "M".equals(actionMode.getTag()));
        }
    }

    public Selection getSelection() {
        return mSelection;
    }

    public GesturedListView getListView() {
        return (GesturedListView) super.getListView();
    }

    @Override
    public HeadsListViewAdapter getListAdapter() {
        return ((HeadsListViewAdapter) super.getListAdapter());
    }

    protected void displayScheduleTimestampDialog(int id, long noteId) {
        TreeSet<Long> noteIds = new TreeSet<>();
        noteIds.add(noteId);

        displayScheduleTimestampDialog(id, noteIds);
    }

    protected void displayScheduleTimestampDialog(int id, TreeSet<Long> noteIds) {
        OrgDateTime time = null;

        /* If there is only one note, use its time as dialog's default. */
        if (noteIds.size() == 1) {
            time = getScheduledTimeForNote(noteIds.first());
        }

        TimestampDialogFragment f = TimestampDialogFragment.getInstance(
                id,
                R.string.schedule,
                noteIds,
                time);

        f.setTargetFragment(this, 0);

        f.show(getActivity().getSupportFragmentManager(), TimestampDialogFragment.FRAGMENT_TAG);
    }

    private OrgDateTime getScheduledTimeForNote(long id) {
        Note note = mShelf.getNote(id);

        if (note != null && note.getHead().hasScheduled()) {
            return note.getHead().getScheduled().getStartTime();
        }

        return null;
    }


    protected void onButtonClick(NoteListFragmentListener listener, View itemView, int buttonId, long noteId) {
        String currentState = (String) itemView.getTag(R.id.note_view_state);

        NoteStates states = NoteStates.Companion.fromPreferences(getContext());

        switch (buttonId) {
            /* Query fragments. */

            case R.id.item_menu_open_btn:
                listener.onNoteScrollToRequest(noteId);
                break;

            /* Right fling */

            case R.id.item_menu_schedule_btn:
                displayScheduleTimestampDialog(R.id.item_menu_schedule_btn, noteId);
                break;

            case R.id.item_menu_prev_state_btn:
                listener.onStateChangeRequest(
                        noteSet(noteId),
                        states.getPrevious(currentState));
                break;

            case R.id.item_menu_state_btn:
                openNoteStateDialog(listener, noteSet(noteId), currentState);
                break;

            case R.id.item_menu_next_state_btn:
                listener.onStateChangeRequest(
                        noteSet(noteId),
                        states.getNext(currentState));
                break;

            case R.id.item_menu_done_state_btn:
                if (currentState != null && AppPreferences.isDoneKeyword(getContext(), currentState)) {
                    listener.onStateChangeRequest(
                            noteSet(noteId),
                            AppPreferences.getFirstTodoState(getContext()));
                } else {
                    listener.onStateChangeRequest(
                            noteSet(noteId),
                            AppPreferences.getFirstDoneState(getContext()));
                }

                break;
        }
    }


    protected void openNoteStateDialog(NoteListFragmentListener listener, Set<Long> noteIds, String currentState) {
        dialog = NoteStateDialog.INSTANCE.create(
                getContext(),
                currentState,
                (state) -> {
                    listener.onStateChangeRequest(noteIds, state);
                    return null;
                },
                () -> {
                    listener.onStateChangeRequest(noteIds, NoteStates.NO_STATE_KEYWORD);
                    return null;
                }
        );

        dialog.show();
    }

    protected Set<Long> noteSet(Long noteId) {
        Set<Long> ids = new TreeSet<>();
        ids.add(noteId);
        return ids;
    }

    /**
     * Get target note id.
     * If location is above the selected notes, use the first selected note.
     * Otherwise, use the last selected note.
     */
    protected long getTargetNoteIdFromSelection(Place place) {
        return place == Place.ABOVE ?
                mSelection.getIds().first() : mSelection.getIds().last();
    }

    public interface NoteListFragmentListener extends FragmentListener {
        void onNoteScrollToRequest(long noteId);
        void onNoteNewRequest(NotePlace target);
        void onNoteClick(NoteListFragment fragment, View view, int position, long id, long noteId);
        void onNoteLongClick(NoteListFragment fragment, View view, int position, long id, long noteId);

        void onStateChangeRequest(Set<Long> noteIds, String state);
        void onStateFlipRequest(long noteId);
        void onScheduledTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time);
    }
}

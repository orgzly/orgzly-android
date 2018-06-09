package com.orgzly.android.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.view.ActionMode;
import android.view.View;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.Note;
import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.ActionModeListener;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.HeadsListViewAdapter;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.NoteStates;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.Selection;
import com.orgzly.android.ui.dialogs.NoteStateDialog;
import com.orgzly.android.ui.dialogs.TimestampDialogFragment;
import com.orgzly.android.ui.views.GesturedListView;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.datetime.OrgDateTime;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fragment which is displaying a list of notes,
 * such as {@link BookFragment}, {@link SearchFragment} or {@link AgendaFragment}.
 */
public abstract class NoteListFragment extends ListFragment
        implements TimestampDialogFragment.OnDateTimeSetListener {

    private static final String TAG = NoteListFragment.class.getName();

    protected Shelf mShelf;
    protected Selection mSelection;
    protected ActionModeListener mActionModeListener;

    public abstract ActionMode.Callback getNewActionMode();

    public abstract NoteListFragmentListener getListener();

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

    protected void onButtonClick(NoteListFragmentListener listener, View itemView, int buttonId, long noteId) {
        String currentState = (String) itemView.getTag(R.id.note_view_state);

        switch (buttonId) {
            /* Query fragments. */

            case R.id.item_menu_open_btn:
                listener.onNoteFocusInBookRequest(noteId);
                break;

            /* Right fling */

            case R.id.item_menu_schedule_btn:
                displayTimestampDialog(R.id.item_menu_schedule_btn, noteId);
                break;

            case R.id.item_menu_deadline_btn:
                displayTimestampDialog(R.id.item_menu_deadline_btn, noteId);
                break;

            case R.id.item_menu_state_btn:
                openNoteStateDialog(listener, MiscUtils.set(noteId), currentState);
                break;

            case R.id.item_menu_done_state_btn:
                if (currentState != null && AppPreferences.isDoneKeyword(getContext(), currentState)) {
                    listener.onStateChangeRequest(
                            MiscUtils.set(noteId),
                            AppPreferences.getFirstTodoState(getContext()));
                } else {
                    listener.onStateChangeRequest(
                            MiscUtils.set(noteId),
                            AppPreferences.getFirstDoneState(getContext()));
                }

                break;
        }
    }

    protected void openNoteRefileDialog(NoteListFragmentListener listener, long sourceBookId, Set<Long> noteIds) {
        List<Book> books = mShelf.getBooks();
        String[] book_names = new String[books.size()];
        for (int i = 0; i < books.size(); i++) {
            book_names[i] = books.get(i).getName();
        }

        dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.refile_to)
                .setItems(book_names, (d, which) -> {
                    Book target = books.get(which);
                    listener.onNotesRefileRequest(sourceBookId, noteIds, target.getId());
                })
                .show();
    }

    protected void openNoteStateDialog(NoteListFragmentListener listener, Set<Long> noteIds, String currentState) {
        dialog = NoteStateDialog.show(
                getContext(),
                currentState,
                (state) -> {
                    listener.onStateChangeRequest(noteIds, state);
                    return null;
                },
                () -> {
                    listener.onStateChangeRequest(noteIds, NoteStates.NO_STATE_KEYWORD);
                    return null;
                });
    }

    protected void displayTimestampDialog(int id, long noteId) {
        displayTimestampDialog(id, MiscUtils.set(noteId));
    }

    protected void displayTimestampDialog(int id, Set<Long> noteIds) {
        boolean scheduledNotDeadline =
                id == R.id.item_menu_schedule_btn
                || id == R.id.book_cab_schedule
                || id == R.id.query_cab_schedule;

        OrgDateTime time = null;

        /* If there is only one note, use its time as dialog's default. */
        if (noteIds.size() == 1) {
            if (scheduledNotDeadline) {
                time = getScheduledTimeForNote(noteIds.iterator().next());
            } else {
                time = getDeadlineTimeForNote(noteIds.iterator().next());
            }
        }

        TimestampDialogFragment f = TimestampDialogFragment.getInstance(
                id,
                scheduledNotDeadline ? R.string.schedule : R.string.deadline,
                noteIds,
                time);

        f.show(getChildFragmentManager(), TimestampDialogFragment.FRAGMENT_TAG);
    }

    private OrgDateTime getScheduledTimeForNote(long id) {
        Note note = mShelf.getNote(id);

        if (note != null && note.getHead().hasScheduled()) {
            return note.getHead().getScheduled().getStartTime();
        }

        return null;
    }

    private OrgDateTime getDeadlineTimeForNote(long id) {
        Note note = mShelf.getNote(id);

        if (note != null && note.getHead().hasDeadline()) {
            return note.getHead().getDeadline().getStartTime();
        }

        return null;
    }

    @Override
    public void onDateTimeSet(int id, TreeSet<Long> noteIds, OrgDateTime time) {
        switch (id) {
            case R.id.book_cab_schedule:
            case R.id.item_menu_schedule_btn:
            case R.id.query_cab_schedule:
                getListener().onScheduledTimeUpdateRequest(noteIds, time);
                break;

            case R.id.book_cab_deadline:
            case R.id.item_menu_deadline_btn:
            case R.id.query_cab_deadline:
                getListener().onDeadlineTimeUpdateRequest(noteIds, time);
                break;
        }
    }

    @Override
    public void onDateTimeCleared(int id, TreeSet<Long> noteIds) {
        onDateTimeSet(id, noteIds, null);
    }


    @Override
    public void onDateTimeAborted(int id, TreeSet<Long> noteIds) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id);
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
        void onNoteFocusInBookRequest(long noteId);
        void onNoteNewRequest(NotePlace target);
        void onNoteClick(NoteListFragment fragment, View view, int position, long id, long noteId);
        void onNoteLongClick(NoteListFragment fragment, View view, int position, long id, long noteId);

        void onNotesRefileRequest(long sourceBookId, Set<Long> noteIds, long targetBookId);
        void onStateChangeRequest(Set<Long> noteIds, String state);
        void onStateFlipRequest(long noteId);

        void onScheduledTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time);
        void onDeadlineTimeUpdateRequest(Set<Long> noteIds, OrgDateTime time);
    }
}

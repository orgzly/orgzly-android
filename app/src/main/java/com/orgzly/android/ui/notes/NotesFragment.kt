package com.orgzly.android.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ActionMode
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.ActionModeListener
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.dialogs.NoteStateDialog
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.org.datetime.OrgDateTime
import dagger.android.support.DaggerFragment
import java.util.*
import javax.inject.Inject

/**
 * Fragment which is displaying a list of notes,
 * such as BookFragment, SearchFragment or AgendaFragment.
 */
abstract class NotesFragment :
        DaggerFragment(),
        TimestampDialogFragment.OnDateTimeSetListener {

    @JvmField
    var actionModeListener: ActionModeListener? = null

    @Inject
    lateinit var dataRepository: DataRepository

    abstract fun getCurrentListener(): Listener?

    abstract fun getAdapter(): SelectableItemAdapter


    @JvmField
    var dialog: AlertDialog? = null


    var fragmentActionMode: ActionMode? = null

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        dialog = null

        ActivityUtils.keepScreenOnClear(activity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        getAdapter().getSelection().saveIds(outState)

        /* Save action mode state (move mode). */
        val actionMode = actionModeListener?.actionMode
        outState.putBoolean("actionModeMove", actionMode != null && "M" == actionMode.tag)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*
         * Restore selected items, now that adapter is set.
         * Saved with {@link Selection#saveSelectedIds(android.os.Bundle, String)}.
         */
        if (savedInstanceState != null) {
            getAdapter().getSelection().restoreIds(savedInstanceState)
        }
    }

    protected fun openNoteRefileDialog(listener: Listener, sourceBookId: Long, noteIds: Set<Long>) {
        val books = dataRepository.getBooks()
        val bookNames = arrayOfNulls<String>(books.size)
        for (i in books.indices) {
            bookNames[i] = books[i].book.name
        }

        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.refile_to)
                .setItems(bookNames) { _, which ->
                    val (book) = books[which]
                    listener.onNotesRefileRequest(sourceBookId, noteIds, book.id)
                }
                .show()
    }

    protected fun openNoteStateDialog(listener: Listener, noteIds: Set<Long>, currentState: String?) {
        dialog = NoteStateDialog.show(
                context!!,
                currentState,
                { state -> listener.onStateChangeRequest(noteIds, state) },
                { listener.onStateChangeRequest(noteIds, null) })
    }

    protected fun displayTimestampDialog(id: Int, noteIds: Set<Long>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)

        // If there is only one note, use its time as dialog's default
        val time = if (noteIds.size == 1) {
            if (id in scheduledTimeButtonIds()) {
                getScheduledTimeForNote(noteIds.first())
            } else {
                getDeadlineTimeForNote(noteIds.first())
            }
        } else {
            null
        }

        val f = TimestampDialogFragment.getInstance(
                id,
                if (id in scheduledTimeButtonIds()) R.string.schedule else R.string.deadline,
                noteIds,
                time)

        f.show(childFragmentManager, TimestampDialogFragment.FRAGMENT_TAG)
    }

    private fun getScheduledTimeForNote(id: Long): OrgDateTime? {
        val note = dataRepository.getNoteView(id)

        return if (note?.scheduledRangeString != null) {
            OrgDateTime.parse(note.scheduledTimeString)
        } else null

    }

    private fun getDeadlineTimeForNote(id: Long): OrgDateTime? {
        val note = dataRepository.getNoteView(id)

        return if (note?.deadlineRangeString != null) {
            OrgDateTime.parse(note.deadlineTimeString)
        } else null

    }

    override fun onDateTimeSet(id: Int, noteIds: TreeSet<Long>, time: OrgDateTime?) {
        if (id in scheduledTimeButtonIds()) {
            getCurrentListener()?.onScheduledTimeUpdateRequest(noteIds, time)
        } else {
            getCurrentListener()?.onDeadlineTimeUpdateRequest(noteIds, time)
        }
    }

    override fun onDateTimeCleared(id: Int, noteIds: TreeSet<Long>) {
        onDateTimeSet(id, noteIds, null)
    }


    override fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)
    }

    fun scheduledTimeButtonIds(): Set<Int> {
        return setOf(R.id.bottom_action_bar_schedule, R.id.quick_bar_schedule)
    }

    fun deadlineTimeButtonIds(): Set<Int> {
        return setOf(R.id.bottom_action_bar_deadline, R.id.quick_bar_deadline)
    }

    interface Listener {
        fun onNoteOpen(noteId: Long)

        fun onNoteFocusInBookRequest(noteId: Long)

        fun onNoteNewRequest(target: NotePlace)

        fun onNotesRefileRequest(sourceBookId: Long, noteIds: Set<Long>, targetBookId: Long)

        fun onStateChangeRequest(noteIds: Set<Long>, state: String?)

        fun onStateToggleRequest(noteIds: Set<Long>)

        fun onScheduledTimeUpdateRequest(noteIds: Set<Long>, time: OrgDateTime?)
        fun onDeadlineTimeUpdateRequest(noteIds: Set<Long>, time: OrgDateTime?)

    }

    companion object {
        private val TAG = NotesFragment::class.java.name
    }
}

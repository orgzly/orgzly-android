package com.orgzly.android.ui.notes

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.CommonFragment
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.TimeType
import com.orgzly.android.ui.dialogs.NoteStateDialog
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.org.datetime.OrgDateTime
import java.util.*
import javax.inject.Inject

/**
 * Fragment which is displaying a list of notes,
 * such as BookFragment, SearchFragment or AgendaFragment.
 */
abstract class NotesFragment : CommonFragment(), TimestampDialogFragment.OnDateTimeSetListener {

    @Inject
    lateinit var dataRepository: DataRepository

    abstract fun getCurrentListener(): Listener?

    abstract fun getAdapter(): SelectableItemAdapter?


    private var notePopup: PopupWindow? = null

    protected fun showPopupWindow(
        noteId: Long,
        location: NotePopup.Location,
        direction: Int,
        itemView: View,
        e1: MotionEvent,
        e2: MotionEvent,
        listener: NotePopupListener
    ): PopupWindow? {

        val anchor = itemView.findViewById<View>(R.id.item_head_title)

        notePopup = NotePopup.showWindow(noteId, anchor, location, direction, e1, e2) { _, buttonId ->
            listener.onPopupButtonClick(noteId, buttonId)
        }

        // Enable back handler if popup is shown
        if (notePopup != null) {
            notePopupDismissOnBackPress.isEnabled = true
        }

        // Disable back handler on dismiss
        notePopup?.setOnDismissListener {
            notePopup = null
            notePopupDismissOnBackPress.isEnabled = false
        }

        return notePopup
    }

    protected val notePopupDismissOnBackPress = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Dismiss window on back press
            dismiss()

            // Disable self and press back again
            isEnabled = false
            activity?.onBackPressed()
        }
    }

    private fun dismiss() {
        notePopup?.dismiss()
        notePopup = null
    }


    @JvmField
    var dialog: AlertDialog? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)
    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        dialog = null

        ActivityUtils.keepScreenOnClear(activity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        getAdapter()?.getSelection()?.saveIds(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        /*
         * Restore selected items, now that adapter is set.
         * Saved with {@link Selection#saveSelectedIds(android.os.Bundle, String)}.
         */
        if (savedInstanceState != null) {
            getAdapter()?.getSelection()?.restoreIds(savedInstanceState)
        }
    }

    protected fun openNoteStateDialog(listener: Listener, noteIds: Set<Long>, currentState: String?) {
        dialog = NoteStateDialog.show(
            requireContext(),
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

        val timeType = if (id in scheduledTimeButtonIds())
            TimeType.SCHEDULED
        else
            TimeType.DEADLINE

        val f = TimestampDialogFragment.getInstance(id, timeType, noteIds, time)

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

    override fun onDateTimeAborted(id: Int, noteIds: TreeSet<Long>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)
    }

    fun scheduledTimeButtonIds(): Set<Int> {
        return setOf(R.id.schedule, R.id.note_popup_set_schedule)
    }

    fun deadlineTimeButtonIds(): Set<Int> {
        return setOf(R.id.deadline, R.id.note_popup_set_deadline)
    }

    interface Listener {
        fun onNoteOpen(noteId: Long)

        fun onNoteFocusInBookRequest(noteId: Long)

        fun onNoteNewRequest(target: NotePlace)

        fun onStateChangeRequest(noteIds: Set<Long>, state: String?)

        fun onStateToggleRequest(noteIds: Set<Long>)

        fun onScheduledTimeUpdateRequest(noteIds: Set<Long>, time: OrgDateTime?)
        fun onDeadlineTimeUpdateRequest(noteIds: Set<Long>, time: OrgDateTime?)

        fun onClockIn(noteIds: Set<Long>)
        fun onClockOut(noteIds: Set<Long>)
        fun onClockCancel(noteIds: Set<Long>)
    }

    companion object {
        private val TAG = NotesFragment::class.java.name
    }
}

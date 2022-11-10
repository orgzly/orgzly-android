package com.orgzly.android.ui.notes.query

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.lifecycle.ViewModelProvider
import com.orgzly.R
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NotePopup
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.settings.SettingsActivity

/**
 * Displays query results.
 */
abstract class QueryFragment :
        NotesFragment(),
        TimestampDialogFragment.OnDateTimeSetListener,
        DrawerItem {

    /** Currently active query.  */
    var currentQuery: String? = null

    protected var listener: Listener? = null

    protected lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    protected lateinit var viewModel: QueryViewModel


    override fun getCurrentListener(): Listener? {
        return listener
    }

    override fun getCurrentDrawerItemId(): String {
        return getDrawerItemId(currentQuery)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = activity as Listener

        currentQuery = requireArguments().getString(ARG_QUERY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        setHasOptionsMenu(true)
    }


    protected fun handleActionItemClick(ids: Set<Long>, actionId: Int) {
        if (ids.isEmpty()) {
            Log.e(TAG, "Cannot handle action when there are no items selected")
            return
        }

        when (actionId) {
            R.id.note_popup_set_schedule,
            R.id.schedule ->
                displayTimestampDialog(R.id.schedule, ids)

            R.id.note_popup_set_deadline,
            R.id.deadline ->
                displayTimestampDialog(R.id.deadline, ids)

            R.id.note_popup_set_state,
            R.id.state ->
                listener?.let {
                    openNoteStateDialog(it, ids, null)
                }

            R.id.note_popup_toggle_state,
            R.id.toggle_state -> {
                listener?.onStateToggleRequest(ids)
            }

            R.id.note_popup_clock_in,
            R.id.clock_in -> {
                listener?.onClockIn(ids)
            }

            R.id.note_popup_clock_out,
            R.id.clock_out -> {
                listener?.onClockOut(ids)
            }

            R.id.note_popup_clock_cancel,
            R.id.clock_cancel -> {
                listener?.onClockCancel(ids)
            }

            R.id.note_popup_focus,
            R.id.focus ->
                listener?.onNoteFocusInBookRequest(ids.first())

            R.id.sync -> {
                SyncRunner.startSync()
            }

            R.id.activity_action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }
    }

    companion object {
        private val TAG = QueryFragment::class.java.name

        const val ARG_QUERY = "query"

        fun getDrawerItemId(query: String?): String {
            return "$TAG $query"
        }
    }
}

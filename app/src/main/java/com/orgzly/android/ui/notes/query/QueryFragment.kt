package com.orgzly.android.ui.notes.query

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.util.LogUtils

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

    protected fun handleActionItemClick(actionId: Int, ids: Set<Long>) {
        if (ids.isEmpty()) {
            Log.e(TAG, "Cannot handle action when there are no items selected")
            return
        }

        when (actionId) {
            R.id.quick_bar_schedule,
            R.id.schedule ->
                displayTimestampDialog(R.id.schedule, ids)

            R.id.quick_bar_deadline,
            R.id.deadline ->
                displayTimestampDialog(R.id.deadline, ids)

            R.id.quick_bar_state,
            R.id.state ->
                listener?.let {
                    openNoteStateDialog(it, ids, null)
                }

            R.id.quick_bar_focus,
            R.id.focus ->
                listener?.onNoteFocusInBookRequest(ids.first())

            R.id.quick_bar_open ->
                listener?.onNoteOpen(ids.first())

            R.id.quick_bar_done,
            R.id.toggle_state -> {
                listener?.onStateToggleRequest(ids)
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

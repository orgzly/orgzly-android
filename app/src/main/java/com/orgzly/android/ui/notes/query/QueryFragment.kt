package com.orgzly.android.ui.notes.query

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.ActionModeListener
import com.orgzly.android.ui.BottomActionBar
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils

/**
 * Displays query results.
 */
abstract class QueryFragment :
        NotesFragment(),
        TimestampDialogFragment.OnDateTimeSetListener,
        DrawerItem,
        BottomActionBar.Callback {

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
        actionModeListener = activity as ActionModeListener

        currentQuery = arguments!!.getString(ARG_QUERY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = ViewModelProviders.of(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        announceChangesToActivity()
    }

    internal abstract fun announceChangesToActivity()

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        actionModeListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.query_actions, menu)

        ActivityUtils.keepScreenOnUpdateMenuItem(
                activity,
                menu,
                menu.findItem(R.id.query_options_keep_screen_on))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        return when (item.itemId) {
            R.id.query_options_keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, item)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun handleActionItemClick(actionId: Int, actionMode: ActionMode?, ids: Set<Long>) {
        if (ids.isEmpty()) {
            Log.e(TAG, "Cannot handle action when there are no items selected")
            actionMode?.finish()
            return
        }

        when (actionId) {
            R.id.quick_bar_schedule,
            R.id.bottom_action_bar_schedule ->
                displayTimestampDialog(R.id.bottom_action_bar_schedule, ids)

            R.id.quick_bar_deadline,
            R.id.bottom_action_bar_deadline ->
                displayTimestampDialog(R.id.bottom_action_bar_deadline, ids)

            R.id.quick_bar_clock_in ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 0)
            R.id.quick_bar_clock_out ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 1)
            R.id.quick_bar_clock_cancel ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 2)

            R.id.quick_bar_state,
            R.id.bottom_action_bar_state ->
                listener?.let {
                    openNoteStateDialog(it, ids, null)
                }

            R.id.quick_bar_focus,
            R.id.bottom_action_bar_focus ->
                listener?.onNoteFocusInBookRequest(ids.first())

            R.id.quick_bar_open ->
                listener?.onNoteOpen(ids.first())

            R.id.quick_bar_done,
            R.id.bottom_action_bar_done -> {
                listener?.onStateToggleRequest(ids)
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

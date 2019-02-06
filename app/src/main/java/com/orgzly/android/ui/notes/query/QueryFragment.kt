package com.orgzly.android.ui.notes.query

import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ViewFlipper
import androidx.appcompat.view.ActionMode
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.*
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

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onResume()

        announceChangesToActivity()
    }

    internal abstract fun announceChangesToActivity()

    override fun onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDetach()

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

    protected fun handleActionItemClick(
            actionItemId: Int, actionMode: ActionMode?, selection: Selection) {

        when (actionItemId) {
            R.id.bottom_action_bar_schedule ->
                displayTimestampDialog(R.id.bottom_action_bar_schedule, selection.getIds())

            R.id.bottom_action_bar_deadline ->
                displayTimestampDialog(R.id.bottom_action_bar_deadline, selection.getIds())

            R.id.bottom_action_bar_state ->
                listener?.let {
                    openNoteStateDialog(it, selection.getIds(), null)
                }

            R.id.bottom_action_bar_focus ->
                listener?.onNoteFocusInBookRequest(selection.getFirstId())

            R.id.bottom_action_bar_open ->
                listener?.onNoteOpen(selection.getFirstId())

            R.id.bottom_action_bar_done -> {
                listener?.onStateToggleRequest(selection.getIds())
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

package com.orgzly.android.ui.savedsearches

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.savedsearch.FileSavedSearchStore
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.Fab
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentSavedSearchesBinding
import java.io.IOException
import javax.inject.Inject

/**
 * Displays and allows modifying saved searches.
 */
class SavedSearchesFragment : Fragment(), Fab, DrawerItem, OnViewHolderClickListener<SavedSearch> {
    private lateinit var binding: FragmentSavedSearchesBinding

    private var listener: Listener? = null

    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()

    private var dialog: AlertDialog? = null

    private lateinit var viewAdapter: SavedSearchesAdapter

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var viewModel: SavedSearchesViewModel

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    override fun getCurrentDrawerItemId() = getDrawerItemId()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        listener = activity as Listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SavedSearchesViewModelFactory.getInstance(dataRepository)
        viewModel = ViewModelProvider(this, factory).get(SavedSearchesViewModel::class.java)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSavedSearchesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewAdapter = SavedSearchesAdapter(this)
        viewAdapter.setHasStableIds(true)

        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)

        binding.fragmentSavedSearchesRecyclerView.let {
            it.layoutManager = layoutManager
            it.adapter = viewAdapter
            it.addItemDecoration(dividerItemDecoration)
        }
    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
        actionMode?.finish()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.saved_searches_actions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        return when (item.itemId) {
            R.id.saved_searches_import -> {
                listener?.let {
                    importExport(R.string.import_from, it::onSavedSearchesImportRequest)
                }

                true
            }

            R.id.saved_searches_export -> {
                listener?.let {
                    importExport(R.string.export_to, it::onSavedSearchesExportRequest)
                }
                true
            }

            R.id.saved_searches_help -> {
                val uri = Uri.parse("http://www.orgzly.com/help#search")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun importExport(resId: Int, f: (Int, String) -> Any) {
        try {
            val file = FileSavedSearchStore(requireContext(), dataRepository).file()
            f(R.string.searches, getString(resId, file))
        } catch (e: IOException) {
            CommonActivity.showSnackbar(context, e.localizedMessage)
        }
    }

    override fun onClick(view: View, position: Int, item: SavedSearch) {
        if (actionMode == null) {
            listener?.onSavedSearchEditRequest(item.id)

        } else {
            viewAdapter.getSelection().toggle(item.id)
            viewAdapter.notifyItemChanged(position)

            if (viewAdapter.getSelection().count == 0) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }
    }

    override fun onLongClick(view: View, position: Int, item: SavedSearch) {
        viewAdapter.getSelection().toggle(item.id)
        viewAdapter.notifyItemChanged(position)

        if (viewAdapter.getSelection().count > 0) {
            if (actionMode == null) {
                actionMode = with(activity as AppCompatActivity) {
                    startSupportActionMode(actionModeCallback)
                }
            } else {
                actionMode?.invalidate()
            }

        } else {
            actionMode?.finish()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.viewState.observe(viewLifecycleOwner, Observer {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed view state: $it")

            binding.fragmentSavedSearchesFlipper.displayedChild = when (it) {
                SavedSearchesViewModel.ViewState.LOADING -> 0
                SavedSearchesViewModel.ViewState.LOADED -> 1
                SavedSearchesViewModel.ViewState.EMPTY -> 2
                else -> 1
            }
        })

        viewModel.savedSearches.observe(viewLifecycleOwner, Observer { savedSearches ->
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Observed saved searches: ${savedSearches.count()}")

            viewAdapter.submitList(savedSearches)

            val ids = savedSearches.mapTo(hashSetOf()) { it.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            actionMode?.invalidate()
        })
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        announceChangesToActivity()
    }

    override fun getFabAction(): Runnable {
        return Runnable {
            listener?.onSavedSearchNewRequest()
        }
    }

    private fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                getString(R.string.searches),
                null,
                viewAdapter.getSelection().count)
    }

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode

            val inflater = mode.menuInflater
            inflater.inflate(R.menu.saved_searches_cab, menu)

            /* Needed for after orientation change. */
            mode.title = viewAdapter.getSelection().count.toString()

            sharedMainActivityViewModel.lockDrawer()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (viewAdapter.getSelection().count > 1) {
                menu.findItem(R.id.saved_searches_cab_move_up).isVisible = false
                menu.findItem(R.id.saved_searches_cab_move_down).isVisible = false

            } else {
                menu.findItem(R.id.saved_searches_cab_move_up).isVisible = true
                menu.findItem(R.id.saved_searches_cab_move_up).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.findItem(R.id.saved_searches_cab_move_down).isVisible = true
                menu.findItem(R.id.saved_searches_cab_move_down).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            mode.title = viewAdapter.getSelection().count.toString()

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selection = viewAdapter.getSelection()

            when (item.itemId) {
                R.id.saved_searches_cab_move_up ->
                    selection.getOnly()?.let { id ->
                        listener?.onSavedSearchMoveUpRequest(id)
                    }

                R.id.saved_searches_cab_move_down ->
                    selection.getOnly()?.let { id ->
                        listener?.onSavedSearchMoveDownRequest(id)
                    }

                R.id.saved_searches_cab_delete -> {
                    listener?.onSavedSearchDeleteRequest(selection.getIds())

                    /* Close action mode. */
                    mode.finish()
                }

                else -> return false /* Not handled. */
            }

            return true /* Handled. */
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            viewAdapter.getSelection().clear()
            viewAdapter.notifyDataSetChanged() // FIXME

            announceChangesToActivity()

            sharedMainActivityViewModel.unlockDrawer()

            actionMode = null
        }
    }

    interface Listener {
        fun onSavedSearchNewRequest()
        fun onSavedSearchDeleteRequest(ids: Set<Long>)
        fun onSavedSearchEditRequest(id: Long)
        fun onSavedSearchMoveUpRequest(id: Long)
        fun onSavedSearchMoveDownRequest(id: Long)
        fun onSavedSearchesExportRequest(title: Int, message: String)
        fun onSavedSearchesImportRequest(title: Int, message: String)
    }

    companion object {
        private val TAG = SavedSearchesFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmStatic
        val FRAGMENT_TAG: String = SavedSearchesFragment::class.java.name

        @JvmStatic
        val instance: SavedSearchesFragment
            get() = SavedSearchesFragment()

        @JvmStatic
        fun getDrawerItemId(): String {
            return TAG
        }
    }
}

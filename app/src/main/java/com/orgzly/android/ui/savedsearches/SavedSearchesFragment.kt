package com.orgzly.android.ui.savedsearches

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
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
import com.orgzly.android.ui.AppBar
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentSavedSearchesBinding
import java.io.IOException
import javax.inject.Inject

/**
 * Displays and allows modifying saved searches.
 */
class SavedSearchesFragment : Fragment(), DrawerItem, OnViewHolderClickListener<SavedSearch> {
    private lateinit var binding: FragmentSavedSearchesBinding

    private var listener: Listener? = null

    private var dialog: AlertDialog? = null

    private lateinit var viewAdapter: SavedSearchesAdapter

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var viewModel: SavedSearchesViewModel

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private val backPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.appBar.handleOnBackPressed()
        }
    }

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

        requireActivity().onBackPressedDispatcher.addCallback(this, backPressHandler)
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

    private fun appBarToDefault() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.saved_searches_actions)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_menu_24dp, 0)
            })

            setNavigationOnClickListener {
                sharedMainActivityViewModel.openDrawer()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.saved_searches_import -> {
                        listener?.let {
                            importExport(R.string.import_from, it::onSavedSearchesImportRequest)
                        }
                    }

                    R.id.saved_searches_export -> {
                        listener?.let {
                            importExport(R.string.export_to, it::onSavedSearchesExportRequest)
                        }
                    }

                    R.id.saved_searches_help -> {
                        val uri = Uri.parse("http://www.orgzly.com/help#search")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }
                }

                true
            }

            (requireActivity() as? MainActivity)?.setupSearchView(menu) // FIXME
        }

        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        binding.fab.run {
            setOnClickListener {
                listener?.onSavedSearchNewRequest()
            }

            show()
        }
    }

    private fun appBarToMainSelection() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.saved_searches_cab)

            if (viewAdapter.getSelection().count > 1) {
                menu.findItem(R.id.saved_searches_cab_move_up).isVisible = false
                menu.findItem(R.id.saved_searches_cab_move_down).isVisible = false

            } else {
                menu.findItem(R.id.saved_searches_cab_move_up).isVisible = true
                menu.findItem(R.id.saved_searches_cab_move_up).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.findItem(R.id.saved_searches_cab_move_down).isVisible = true
                menu.findItem(R.id.saved_searches_cab_move_down).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_arrow_back_24dp, 0)
            })

            setNavigationOnClickListener {
                viewModel.appBar.toDefault()
            }

            setOnMenuItemClickListener { menuItem ->
                val selection = viewAdapter.getSelection()

                when (menuItem.itemId) {
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
                        viewModel.appBar.toDefault()
                    }
                }

                true
            }
        }

        binding.fab.run {
            hide()
        }

    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
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
        if (viewAdapter.getSelection().count == 0) {
            listener?.onSavedSearchEditRequest(item.id)

        } else {
            viewAdapter.getSelection().toggle(item.id)
            viewAdapter.notifyItemChanged(position)

            if (viewAdapter.getSelection().count > 0) {
                viewModel.appBar.toMainSelection()
            } else {
                viewModel.appBar.toDefault()
            }
        }
    }

    override fun onLongClick(view: View, position: Int, item: SavedSearch) {
        viewAdapter.getSelection().toggle(item.id)
        viewAdapter.notifyItemChanged(position)

        if (viewAdapter.getSelection().count > 0) {
            viewModel.appBar.toMainSelection()
        } else {
            viewModel.appBar.toDefault()
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

        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Observed saved searches: ${data.count()}")

            viewAdapter.submitList(data)

            val ids = data.mapTo(hashSetOf()) { it.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            viewModel.appBar.toState(viewAdapter.getSelection().count)
        })

        viewModel.appBar.state.observeSingle(viewLifecycleOwner) { state ->
            when (state) {
                is AppBar.State.Default, null -> {
                    appBarToDefault()

                    sharedMainActivityViewModel.unlockDrawer()

                    backPressHandler.isEnabled = false
                }

                is AppBar.State.MainSelection -> {
                    appBarToMainSelection()

                    sharedMainActivityViewModel.lockDrawer()

                    backPressHandler.isEnabled = true
                }

                is AppBar.State.NextSelection -> TODO()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
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

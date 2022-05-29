package com.orgzly.android.ui.notes.query.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.main.setupSearchView
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.notes.query.QueryViewModel
import com.orgzly.android.ui.notes.query.QueryViewModel.Companion.APP_BAR_DEFAULT_MODE
import com.orgzly.android.ui.notes.query.QueryViewModel.Companion.APP_BAR_SELECTION_MODE
import com.orgzly.android.ui.notes.query.QueryViewModelFactory
import com.orgzly.android.ui.notes.quickbar.ItemGestureDetector
import com.orgzly.android.ui.notes.quickbar.QuickBarListener
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.setup
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentQuerySearchBinding

/**
 * Displays search results.
 */
class SearchFragment :
        QueryFragment(),
        OnViewHolderClickListener<NoteView>,
        QuickBarListener {

    private lateinit var binding: FragmentQuerySearchBinding

    private lateinit var viewAdapter: SearchAdapter

    private val appBarBackPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.appBar.handleOnBackPressed()
        }
    }


    override fun getAdapter(): SelectableItemAdapter? {
        return if (::viewAdapter.isInitialized) viewAdapter else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = QueryViewModelFactory.forQuery(dataRepository)
        viewModel = ViewModelProvider(this, factory).get(QueryViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this, appBarBackPressHandler)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentQuerySearchBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        val quickBars = QuickBars(binding.root.context, false)

        viewAdapter = SearchAdapter(binding.root.context, this, quickBars)
        viewAdapter.setHasStableIds(true)

        // Restores selection, requires adapter
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)

        binding.fragmentQuerySearchRecyclerView.let { rv ->
            rv.layoutManager = layoutManager
            rv.adapter = viewAdapter
            rv.addItemDecoration(dividerItemDecoration)

            rv.addOnItemTouchListener(ItemGestureDetector(rv.context, object: ItemGestureDetector.Listener {
                override fun onFling(direction: Int, x: Float, y: Float) {
                    rv.findChildViewUnder(x, y)?.let { itemView ->
                        rv.findContainingViewHolder(itemView)?.let { vh ->
                            (vh as? NoteItemViewHolder)?.let {
                                quickBars.onFling(it, direction, this@SearchFragment)
                            }
                        }
                    }
                }
            }))
        }

        binding.swipeContainer.setup()
    }

    override fun onResume() {
        super.onResume()

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
    }

    private fun appBarToDefault() {
        viewAdapter.clearSelection()

        binding.bottomAppBar.run {
            replaceMenu(R.menu.query_actions)

            ActivityUtils.keepScreenOnUpdateMenuItem(activity, menu)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_menu_24dp, 0)
            })

            setNavigationOnClickListener {
                sharedMainActivityViewModel.openDrawer()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.activity_action_settings -> {
                        startActivity(Intent(context, SettingsActivity::class.java))
                    }

                    R.id.keep_screen_on -> {
                        dialog = ActivityUtils.keepScreenOnToggle(activity, menuItem)
                    }
                }
                true
            }

            requireActivity().setupSearchView(menu)
        }
    }

    private fun appBarToMainSelection() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.query_cab)

            // Hide buttons that can't be used when multiple notes are selected
            listOf(R.id.focus).forEach { id ->
                menu.findItem(id)?.isVisible = viewAdapter.getSelection().count == 1
            }

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_arrow_back_24dp, 0)
            })

            setNavigationOnClickListener {
                viewModel.appBar.toMode(APP_BAR_DEFAULT_MODE)
            }

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem.itemId, viewAdapter.getSelection().getIds())
                true
            }
        }
    }

    override fun onQuickBarButtonClick(buttonId: Int, itemId: Long) {
        handleActionItemClick(buttonId, setOf(itemId))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            binding.fragmentQuerySearchViewFlipper.apply {
                displayedChild = when (state) {
                    QueryViewModel.ViewState.LOADING -> 0
                    QueryViewModel.ViewState.LOADED -> 1
                    QueryViewModel.ViewState.EMPTY -> 2
                    else -> 1
                }
            }
        })

        viewModel.data.observe(viewLifecycleOwner, Observer { notes ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed notes: ${notes.size}")

            viewAdapter.submitList(notes)

            val ids = notes.mapTo(hashSetOf()) { it.note.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            viewModel.appBar.toModeFromSelectionCount(viewAdapter.getSelection().count)
        })

        viewModel.refresh(currentQuery, AppPreferences.defaultPriority(context))

        viewModel.appBar.mode.observeSingle(viewLifecycleOwner) { mode ->
            when (mode) {
                APP_BAR_DEFAULT_MODE, null -> {
                    appBarToDefault()
                    sharedMainActivityViewModel.unlockDrawer()
                    appBarBackPressHandler.isEnabled = false

                    // Active query as a title, clickable
                    binding.bottomAppBarTitle.run {
                        text = currentQuery
                    }
                    binding.bottomAppBarTitle.setOnClickListener {
                        binding.bottomAppBar.menu.findItem(R.id.search_view)?.expandActionView()
                    }
                }

                APP_BAR_SELECTION_MODE -> {
                    appBarToMainSelection()
                    sharedMainActivityViewModel.lockDrawer()
                    appBarBackPressHandler.isEnabled = true

                    // Number of selected notes as a title
                    binding.bottomAppBarTitle.run {
                        text = viewAdapter.getSelection().count.toString()
                    }
                }
            }
        }
    }

    override fun onClick(view: View, position: Int, item: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            if (viewAdapter.getSelection().count > 0) {
                toggleNoteSelection(position, item)
            } else {
                openNote(item.note.id)
            }
        } else {
            toggleNoteSelection(position, item)
        }
    }

    override fun onLongClick(view: View, position: Int, item: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            toggleNoteSelection(position, item)
        } else {
            openNote(item.note.id)
        }
    }

    private fun openNote(id: Long) {
        listener?.onNoteOpen(id)
    }

    private fun toggleNoteSelection(position: Int, item: NoteView) {
        val noteId = item.note.id

        viewAdapter.getSelection().toggle(noteId)
        viewAdapter.notifyItemChanged(position)

        viewModel.appBar.toModeFromSelectionCount(viewAdapter.getSelection().count)

    }

    companion object {
        private val TAG = SearchFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = SearchFragment::class.java.name

        @JvmStatic
        fun getInstance(query: String): QueryFragment {
            val fragment = SearchFragment()

            val args = Bundle()
            args.putString(ARG_QUERY, query)

            fragment.arguments = args

            return fragment
        }
    }
}

package com.orgzly.android.ui.notes.query.agenda

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
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.AppBar
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.notes.query.QueryViewModel
import com.orgzly.android.ui.notes.query.QueryViewModelFactory
import com.orgzly.android.ui.notes.quickbar.ItemGestureDetector
import com.orgzly.android.ui.notes.quickbar.QuickBarListener
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.stickyheaders.StickyHeadersLinearLayoutManager
import com.orgzly.android.ui.util.setup
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentQueryAgendaBinding


/**
 * Displays agenda results.
 */
class AgendaFragment :
        QueryFragment(),
        OnViewHolderClickListener<AgendaItem>,
        QuickBarListener {

    private lateinit var binding: FragmentQueryAgendaBinding

    private val item2databaseIds = hashMapOf<Long, Long>()

    lateinit var viewAdapter: AgendaAdapter

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

        requireActivity().onBackPressedDispatcher.addCallback(this, appBarBackPressHandler)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentQueryAgendaBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        val quickBars = QuickBars(binding.root.context, false)

        viewAdapter = AgendaAdapter(binding.root.context, this, quickBars)
        viewAdapter.setHasStableIds(true)

        // Restores selection, requires adapter
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = StickyHeadersLinearLayoutManager<AgendaAdapter>(
                context, LinearLayoutManager.VERTICAL, false)

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)

        binding.fragmentQueryAgendaRecyclerView.let { rv ->
            rv.layoutManager = layoutManager
            rv.adapter = viewAdapter
            rv.addItemDecoration(dividerItemDecoration)

            rv.addOnItemTouchListener(ItemGestureDetector(rv.context, object: ItemGestureDetector.Listener {
                override fun onFling(direction: Int, x: Float, y: Float) {
                    rv.findChildViewUnder(x, y)?.let { itemView ->
                        rv.findContainingViewHolder(itemView)?.let { vh ->
                            (vh as? NoteItemViewHolder)?.let {
                                quickBars.onFling(it, direction, this@AgendaFragment)
                            }
                        }
                    }
                }
            }))

//            val itemTouchHelper = NoteItemTouchHelper(false, object : NoteItemTouchHelper.Listener {
//                override fun onSwiped(viewHolder: NoteItemViewHolder, direction: Int) {
//                    val dbId = item2databaseIds[viewHolder.itemId]
//                    if (dbId != null) {
//                        listener?.onNoteFocusInBookRequest(dbId)
//                    } else {
//                        // Divider
//                    }
//                }
//            })
//
//            itemTouchHelper.attachToRecyclerView(rv)
        }

        binding.swipeContainer.setup()
    }

    override fun onResume() {
        super.onResume()

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
    }

    private fun appBarToDefault() {
        // Clear selection
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        binding.bottomAppBar.run {
            replaceMenu(R.menu.query_actions)

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
                }
                true
            }

            (requireActivity() as? MainActivity)?.setupSearchView(menu) // FIXME
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
                viewModel.appBar.toDefault()
            }

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem.itemId, viewAdapter.getSelection().getIds())
                true
            }
        }
    }

    override fun onQuickBarButtonClick(buttonId: Int, itemId: Long) {
        item2databaseIds[itemId]?.let {
            handleActionItemClick(buttonId, setOf(it))
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        val factory = QueryViewModelFactory.forQuery(dataRepository)

        viewModel = ViewModelProvider(this, factory).get(QueryViewModel::class.java)

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            binding.fragmentQueryAgendaViewFlipper.displayedChild = when (state) {
                QueryViewModel.ViewState.LOADING -> 0
                QueryViewModel.ViewState.LOADED -> 1
                else -> 1
            }
        })

        viewModel.data.observe(viewLifecycleOwner, Observer { notes ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed notes: ${notes.size}")

            val items = AgendaItems.getList(notes, currentQuery, item2databaseIds)

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Replacing data with ${items.size} agenda items")

            viewAdapter.submitList(items)

            val ids = notes.mapTo(hashSetOf()) { it.note.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            viewAdapter.getSelection().setMap(item2databaseIds)

            viewModel.appBar.toState(viewAdapter.getSelection().count)
        })

        viewModel.appBar.state.observeSingle(viewLifecycleOwner) { state ->
            when (state) {
                is AppBar.State.Default, null -> {
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

                is AppBar.State.MainSelection -> {
                    appBarToMainSelection()
                    sharedMainActivityViewModel.lockDrawer()
                    appBarBackPressHandler.isEnabled = true

                    // Number of selected notes as a title
                    binding.bottomAppBarTitle.run {
                        text = viewAdapter.getSelection().count.toString()
                    }
                }

                is AppBar.State.NextSelection -> {
                }
            }
        }

        viewModel.refresh(currentQuery, AppPreferences.defaultPriority(context))
    }

    override fun onClick(view: View, position: Int, item: AgendaItem) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            if (viewAdapter.getSelection().count > 0) {
                toggleNoteSelection(position, item)
            } else {
                openNote(item)
            }
        } else {
            toggleNoteSelection(position, item)
        }
    }

    override fun onLongClick(view: View, position: Int, item: AgendaItem) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            toggleNoteSelection(position, item)
        } else {
            openNote(item)
        }
    }

    private fun openNote(item: AgendaItem) {
        if (item is AgendaItem.Note) {
            val noteId = item.note.note.id

            listener?.onNoteOpen(noteId)
        }
    }

    private fun toggleNoteSelection(position: Int, item: AgendaItem) {
        if (item is AgendaItem.Note) {
            viewAdapter.getSelection().toggle(item.id)
            viewAdapter.notifyItemChanged(position)

            viewModel.appBar.toState(viewAdapter.getSelection().count)
        }
    }

    companion object {
        private val TAG = AgendaFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = AgendaFragment::class.java.name


        @JvmStatic
        fun getInstance(query: String): QueryFragment {
            val fragment = AgendaFragment()

            val args = Bundle()
            args.putString(ARG_QUERY, query)

            fragment.arguments = args

            return fragment
        }
    }

}
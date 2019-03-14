package com.orgzly.android.ui.notes.query.search

import android.os.Bundle
import android.view.*
import android.widget.ViewFlipper
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.BottomActionBar
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.SearchAdapter
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.notes.query.QueryViewModel
import com.orgzly.android.ui.notes.query.QueryViewModelFactory
import com.orgzly.android.ui.notes.quickbar.ItemGestureDetector
import com.orgzly.android.ui.notes.quickbar.QuickBarListener
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import java.util.*

/**
 * Displays search results.
 */
class SearchFragment :
        QueryFragment(),
        OnViewHolderClickListener<NoteView>,
        ActionMode.Callback,
        BottomActionBar.Callback,
        QuickBarListener {

    private lateinit var viewFlipper: ViewFlipper

    private lateinit var viewAdapter: SearchAdapter

    override fun getAdapter(): SelectableItemAdapter {
        return viewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = QueryViewModelFactory.forQuery(dataRepository)
        viewModel = ViewModelProviders.of(this, factory).get(QueryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_query_search, container, false)

        viewFlipper = view.findViewById(R.id.fragment_query_search_view_flipper)

        setupRecyclerView(view)

        return view
    }

    private fun setupRecyclerView(view: View) {
        val quickBars = QuickBars(view.context, false)

        viewAdapter = SearchAdapter(view.context, this, quickBars)
        viewAdapter.setHasStableIds(true)

        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(context, layoutManager.orientation)

        view.findViewById<RecyclerView>(R.id.fragment_query_search_recycler_view).let { rv ->
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

//            val itemTouchHelper = NoteItemTouchHelper(false, object : NoteItemTouchHelper.Listener {
//                override fun onSwiped(viewHolder: NoteItemViewHolder, direction: Int) {
//                    listener?.onNoteFocusInBookRequest(viewHolder.itemId)
//                }
//            })
//
//            itemTouchHelper.attachToRecyclerView(rv)
        }
    }

    override fun onQuickBarButtonClick(buttonId: Int, itemId: Long) {
        handleActionItemClick(buttonId, actionModeListener?.actionMode, Collections.singleton(itemId))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            viewFlipper.apply {
                displayedChild = when (state) {
                    QueryViewModel.ViewState.LOADING -> 0
                    QueryViewModel.ViewState.LOADED -> 1
                    QueryViewModel.ViewState.EMPTY -> 2
                    else -> 1
                }
            }
        })

        viewModel.notes().observe(viewLifecycleOwner, Observer { notes ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed notes: ${notes.size}")

            viewAdapter.submitList(notes)

            val ids = notes.mapTo(hashSetOf()) { it.note.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            activity?.invalidateOptionsMenu()

            actionModeListener?.updateActionModeForSelection(
                    viewAdapter.getSelection().count, this)
        })

        viewModel.refresh(currentQuery, AppPreferences.defaultPriority(context))
    }

    override fun onClick(view: View, position: Int, item: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            if (viewAdapter.getSelection().count > 0) {
                toggleNoteSelection(view, position, item)
            } else {
                openNote(item.note.id)
            }
        } else {
            toggleNoteSelection(view, position, item)
        }
    }

    override fun onLongClick(view: View, position: Int, item: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            toggleNoteSelection(view, position, item)
        } else {
            openNote(item.note.id)
        }
    }

    private fun openNote(id: Long) {
        listener?.onNoteOpen(id)
    }

    private fun toggleNoteSelection(view: View, position: Int, item: NoteView) {
        val noteId = item.note.id

        viewAdapter.getSelection().toggle(noteId)
        viewAdapter.notifyItemChanged(position)

        actionModeListener?.updateActionModeForSelection(
                viewAdapter.getSelection().count, this)

    }

    override fun onBottomActionItemClicked(id: Int) {
        handleActionItemClick(id, actionModeListener?.actionMode, viewAdapter.getSelection().getIds())
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        handleActionItemClick(menuItem.itemId, actionMode, viewAdapter.getSelection().getIds())

        return true
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
//        val inflater = actionMode.menuInflater
//
//        inflater.inflate(R.menu.query_cab, menu)

        sharedMainActivityViewModel.lockDrawer()

        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        /* Update action mode with number of selected items. */
        actionMode.title = viewAdapter.getSelection().count.toString()

        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        actionModeListener?.actionModeDestroyed()

        sharedMainActivityViewModel.unlockDrawer()
    }

    override fun onInflateBottomActionMode(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.bottom_action_bar_query)

        // Hide buttons that can't be used when multiple notes are selected
        listOf(
                R.id.bottom_action_bar_focus,
                R.id.bottom_action_bar_open).forEach { id ->

            toolbar.menu.findItem(id)?.isVisible = viewAdapter.getSelection().count <= 1
        }

        ActivityUtils.distributeToolbarItems(activity, toolbar)
    }

    override fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                getString(R.string.fragment_query_title),
                currentQuery,
                viewAdapter.getSelection().count)
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

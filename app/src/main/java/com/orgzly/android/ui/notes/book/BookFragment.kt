package com.orgzly.android.ui.notes.book

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.*
import android.widget.ViewFlipper
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.BookUtils
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils

/**
 * Displays all notes from the notebook.
 * Allows moving, cutting, pasting etc.
 */
class BookFragment :
        NotesFragment(),
        Fab,
        TimestampDialogFragment.OnDateTimeSetListener,
        DrawerItem,
        ActionMode.Callback,
        BottomActionBar.Callback,
        BookAdapter.OnClickListener {

    private var listener: Listener? = null

    private lateinit var viewAdapter: BookAdapter

    private lateinit var layoutManager: androidx.recyclerview.widget.LinearLayoutManager

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: BookViewModel

    override fun getAdapter(): BookAdapter {
        return viewAdapter
    }

    override fun getCurrentListener(): NotesFragment.Listener? {
        return listener
    }

    var currentBook: Book? = null

    private var mBookId: Long = 0

    private var mActionModeTag: String? = null

    /** Used for different states after loading the notebook and notes. */
    private var viewFlipper: ViewFlipper? = null

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    init {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
    }


    override fun onAttach(context: Context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context)
        super.onAttach(context)

        listener = activity as Listener
        actionModeListener = activity as ActionModeListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true)

        parseArguments()

        val factory = BookViewModelFactory.forBook(dataRepository, mBookId)
        viewModel = ViewModelProviders.of(this, factory).get(BookViewModel::class.java)

        if (savedInstanceState != null && savedInstanceState.getBoolean("actionModeMove", false)) {
            mActionModeTag = "M"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val view = inflater.inflate(R.layout.fragment_notes_book, container, false)

        viewFlipper = view.findViewById<View>(R.id.fragment_book_view_flipper) as ViewFlipper

        setupRecyclerView(view)

        return view
    }

    private fun setupRecyclerView(view: View) {
        viewAdapter = BookAdapter(view.context, this, inBook = true)
        viewAdapter.setHasStableIds(true)

        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.fragment_notes_book_recycler_view)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = viewAdapter

        /*
         * Disable item animator (DefaultItemAnimator).
         * Animation is too slow.  And if animations are off in developer options, items flicker.
         * TODO: Do for query too?
         */
        recyclerView.itemAnimator = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        viewModel.dataLoadState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            viewFlipper?.apply {
                displayedChild = when (state) {
                    BookViewModel.LoadState.IN_PROGRESS -> 0
                    BookViewModel.LoadState.DONE -> 1
                    BookViewModel.LoadState.NO_NOTES -> 2
                    BookViewModel.LoadState.BOOK_DOES_NOT_EXIST -> 3
                    else -> 1
                }
            }
        })

        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Observed data: book ${data.book} and ${data.notes?.size} notes")

            val book = data.book
            val notes = data.notes

            this.currentBook = book

            announceChangesToActivity()

            viewAdapter.setPreface(book)

            if (notes != null) {
                viewAdapter.submitList(notes)

                // Restore scrolling position
                savedInstanceState?.getParcelable<Parcelable>(LAYOUT_STATE)?.let {
                    layoutManager.onRestoreInstanceState(it)
                }

                val ids = notes.mapTo(hashSetOf()) { it.note.id }

                viewAdapter.getSelection().removeNonExistent(ids)

                activity?.invalidateOptionsMenu()

                actionModeListener?.updateActionModeForSelection(
                        viewAdapter.getSelection().count, this)

                actionModeListener?.actionMode?.let { actionMode ->
                    if (mActionModeTag != null) {
                        actionMode.tag = "M" // TODO: Ugh.
                        actionMode.invalidate()
                        mActionModeTag = null
                    }
                }

                scrollToNoteIfSet()
            }

            updateLoadState(notes)
        })
    }

    private fun updateLoadState(notes: List<NoteView>?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        viewFlipper?.apply {
            if (currentBook == null) {
                viewModel.setLoadState(BookViewModel.LoadState.BOOK_DOES_NOT_EXIST)

            } else if (notes == null) {
                viewModel.setLoadState(BookViewModel.LoadState.IN_PROGRESS)

            } else if (viewAdapter.isPrefaceDisplayed() || !notes.isNullOrEmpty()) {
                viewModel.setLoadState(BookViewModel.LoadState.DONE)

            } else {
                viewModel.setLoadState(BookViewModel.LoadState.NO_NOTES)
            }

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onSaveInstanceState(outState)

        outState.putParcelable(LAYOUT_STATE, layoutManager.onSaveInstanceState())
    }

    override fun onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDestroyView()
    }

    override fun onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDetach()

        listener = null
    }

    private fun parseArguments() {
        arguments?.let {
            if (!it.containsKey(ARG_BOOK_ID)) {
                throw IllegalArgumentException("No book id passed")
            }

            mBookId = it.getLong(ARG_BOOK_ID)

            if (mBookId <= 0) {
                throw IllegalArgumentException("Passed book id $mBookId is not valid")
            }
        } ?: throw IllegalArgumentException("No arguments passed")
    }

    /*
     * Options menu.
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.book_actions, menu)

        ActivityUtils.keepScreenOnUpdateMenuItem(
                activity,
                menu,
                menu.findItem(R.id.books_options_keep_screen_on))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        /* Remove some menu items if book doesn't exist or it doesn't contain any notes. */

        if (currentBook == null || viewAdapter.getDataItemCount() == 0) {
            menu.removeItem(R.id.books_options_menu_item_cycle_visibility)
        }

        if (currentBook == null) {
            menu.removeItem(R.id.books_options_menu_book_preface)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        when (item.itemId) {
            R.id.books_options_menu_item_cycle_visibility -> {
                listener?.onCycleVisibilityRequest(currentBook)
                return true
            }

            R.id.books_options_menu_book_preface -> {
                onPrefaceClick()
                return true
            }

            R.id.books_options_keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, item)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    /*
     * Actions
     */

    private fun newNoteRelativeToSelection(place: Place) {
        val targetNoteId = getTargetNoteIdFromSelection(place)
        listener?.onNoteNewRequest(NotePlace(mBookId, targetNoteId, place))
    }

    private fun moveNotes(offset: Int) {
        /* Sanity check. Should not ever happen. */
        if (viewAdapter.getSelection().count == 0) {
            Log.e(TAG, "Trying to move notes up while there are no notes selected")
            return
        }

        listener?.onNotesMoveRequest(mBookId, viewAdapter.getSelection().getFirstId(), offset)
    }

    /**
     * Paste notes.
     * @param place [Place]
     */
    private fun pasteNotes(place: Place) {
        val noteId = getTargetNoteIdFromSelection(place)

        /* Remove selection. */
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        listener?.onNotesPasteRequest(mBookId, noteId, place)
    }

    private fun scrollToNoteIfSet() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val noteId = arguments?.getLong(ARG_NOTE_ID, 0) ?: 0

        if (noteId > 0) {
            val startedAt = System.currentTimeMillis()

            for (i in 0 until viewAdapter.itemCount) {
                val id = viewAdapter.getItemId(i)

                if (id == noteId) {
                    scrollToPosition(i)

                    /* Make sure we don't scroll again (for example after configuration change). */
                    Handler().postDelayed({ arguments?.remove(ARG_NOTE_ID) }, 500)

                    break
                }
            }

            if (BuildConfig.LOG_DEBUG) {
                val ms = System.currentTimeMillis() - startedAt
                LogUtils.d(TAG, "Scrolling to note " + noteId + " took " + ms + "ms")
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        layoutManager.scrollToPositionWithOffset(position, 0)
    }

    private fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                BookUtils.getFragmentTitleForBook(currentBook),
                BookUtils.getFragmentSubtitleForBook(context, currentBook),
                viewAdapter.getSelection().count)
    }

    override fun getFabAction(): Runnable? {
        return if (currentBook != null) {
            Runnable {
                listener?.onNoteNewRequest(NotePlace(mBookId))
            }
        } else
            null
    }

    private fun delete(ids: Set<Long>) {
        dialog = AlertDialog.Builder(context)
                .setTitle(R.string.delete_notes)
                .setMessage(R.string.delete_notes_and_all_subnotes)
                .setPositiveButton(R.string.delete) { _, _ ->
                    listener?.onNotesDeleteRequest(mBookId, ids)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
    }

    override fun getCurrentDrawerItemId(): String {
        return getDrawerItemId(mBookId)
    }

    override fun onNoteClick(view: View, position: Int, noteView: NoteView) {
        val noteId = noteView.note.id

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, listener, noteView)

        viewAdapter.getSelection().toggle(noteId)
        viewAdapter.notifyItemChanged(position)

        actionModeListener?.updateActionModeForSelection(
                viewAdapter.getSelection().count, this)

    }

    override fun onNoteLongClick(view: View, position: Int, noteView: NoteView) {
        val noteId = noteView.note.id

        listener?.onNoteOpen(noteId)
    }

    override fun onPrefaceClick() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        currentBook?.let {
            listener?.onBookPrefaceEditRequest(it)
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menu)

        val inflater = actionMode.menuInflater

        inflater.inflate(R.menu.book_cab, menu)

        sharedMainActivityViewModel.lockDrawer()

        return true
    }

    /**
     * Called each time the action mode is shown. Always called after onCreateActionMode,
     * but may be called multiple times if the mode is invalidated.
     */
    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode.tag)

        /* Update action mode with number of selected items. */
        actionMode.title = viewAdapter.getSelection().count.toString()

        val inflater = actionMode.menuInflater

        menu.clear()

        if ("M" == actionMode.tag) { // Movement menu
            if (viewAdapter.getSelection().count > 1) {
                menu.clear()
                actionMode.tag = null
            } else {
                inflater.inflate(R.menu.book_cab_moving, menu)
            }
        } else {
            inflater.inflate(R.menu.book_cab, menu)
        }

        /* Hide some items if multiple notes are selected. */
        for (id in ITEMS_HIDDEN_ON_MULTIPLE_SELECTED_NOTES) {
            val item = menu.findItem(id)
            if (item != null) {
                item.isVisible = viewAdapter.getSelection().count == 1
            }
        }

        announceChangesToActivity()

        return true
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionMode, menuItem)

        handleActionItemClick(menuItem.itemId, actionMode, viewAdapter.getSelection())

        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        actionModeListener?.actionModeDestroyed()

        announceChangesToActivity()

        sharedMainActivityViewModel.unlockDrawer()
    }

    override fun onInflateBottomActionMode(toolbar: Toolbar) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        toolbar.inflateMenu(R.menu.bottom_action_bar_book)

        // Hide buttons that can't be used when multiple notes are selected
        listOf(
                R.id.bottom_action_bar_new,
                R.id.bottom_action_bar_open).forEach { id ->

            toolbar.menu.findItem(id)?.isVisible = viewAdapter.getSelection().count <= 1
        }

        ActivityUtils.distributeToolbarItems(activity, toolbar)
    }

    override fun onBottomActionItemClicked(id: Int) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)

        handleActionItemClick(id, actionModeListener?.actionMode, viewAdapter.getSelection())
    }

    private fun handleActionItemClick(actionItemId: Int, actionMode: ActionMode?, selection: Selection) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionItemId, selection)

        when (actionItemId) {
            R.id.bottom_action_bar_open -> {
                listener?.onNoteOpen(selection.getFirstId())
            }

            R.id.bottom_action_bar_new_above -> {
                newNoteRelativeToSelection(Place.ABOVE)
                actionMode?.finish()
            }

            R.id.bottom_action_bar_new_under -> {
                newNoteRelativeToSelection(Place.UNDER)
                actionMode?.finish()
            }

            R.id.bottom_action_bar_new_below -> {
                newNoteRelativeToSelection(Place.BELOW)
                actionMode?.finish()
            }

            R.id.book_cab_move -> {
                actionMode?.tag = "M"
                actionMode?.invalidate()
            }

            in scheduledTimeButtonIds(),
            in deadlineTimeButtonIds() ->
                displayTimestampDialog(actionItemId, selection.getIds())

            R.id.book_cab_delete_note -> {
                delete(selection.getIds())

                // TODO: Wait for user confirmation (dialog close) before doing this
                actionMode?.finish()
            }

            R.id.book_cab_cut -> {
                listener?.onNotesCutRequest(mBookId, selection.getIds())

                actionMode?.finish()
            }

            R.id.book_cab_paste_above -> {
                pasteNotes(Place.ABOVE)
                actionMode?.finish()
            }

            R.id.book_cab_refile ->
                listener?.let {
                    openNoteRefileDialog(it, mBookId, selection.getIds())
                }

            R.id.book_cab_paste_under -> {
                pasteNotes(Place.UNDER)
                actionMode?.finish()
            }

            R.id.book_cab_paste_below -> {
                pasteNotes(Place.BELOW)
                actionMode?.finish()
            }

            R.id.notes_action_move_up ->
                moveNotes(-1)

            R.id.notes_action_move_down ->
                moveNotes(1)

            R.id.notes_action_move_left ->
                listener?.onNotesPromoteRequest(mBookId, selection.getIds())

            R.id.notes_action_move_right ->
                listener?.onNotesDemoteRequest(mBookId, selection.getIds())

            R.id.bottom_action_bar_state ->
                listener?.let {
                    openNoteStateDialog(it, selection.getIds(), null)
                }

            R.id.bottom_action_bar_done -> {
                listener?.onStateToggleRequest(selection.getIds())
            }

            R.id.bottom_action_bar_focus ->
                listener?.onNoteFocusInBookRequest(selection.getFirstId())
        }
    }

    interface Listener : NotesFragment.Listener {
        fun onBookPrefaceEditRequest(book: Book)
        fun onBookPrefaceUpdate(bookId: Long, preface: String)

        fun onNotesDeleteRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesCutRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesPasteRequest(bookId: Long, noteId: Long, place: Place)
        fun onNotesPromoteRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesDemoteRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesMoveRequest(bookId: Long, noteId: Long, offset: Int)
        override fun onNotesRefileRequest(sourceBookId: Long, noteIds: Set<Long>, targetBookId: Long)

        fun onCycleVisibilityRequest(book: Book?)
    }

    companion object {

        private val TAG = BookFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = BookFragment::class.java.name

        /* Arguments. */
        private const val ARG_BOOK_ID = "bookId"
        private const val ARG_NOTE_ID = "noteId"

        private const val LAYOUT_STATE = "layout"

        // https://github.com/orgzly/orgzly-android/issues/67
        private val ITEMS_HIDDEN_ON_MULTIPLE_SELECTED_NOTES = intArrayOf(
                R.id.book_cab_cut,
                R.id.book_cab_paste,
                R.id.book_cab_move,
                R.id.book_cab_refile)

        /**
         * @param bookId Book ID
         * @param noteId Set position (scroll to) this note, if greater then zero
         */
        @JvmStatic
        fun getInstance(bookId: Long, noteId: Long): BookFragment {
            val fragment = BookFragment()

            val args = Bundle()
            args.putLong(ARG_BOOK_ID, bookId)
            args.putLong(ARG_NOTE_ID, noteId)

            fragment.arguments = args

            return fragment
        }

        @JvmStatic
        fun getDrawerItemId(bookId: Long): String {
            return "$TAG $bookId"
        }
    }
}

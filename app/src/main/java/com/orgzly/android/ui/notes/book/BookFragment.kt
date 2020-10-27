package com.orgzly.android.ui.notes.book

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.BookUtils
import com.orgzly.android.db.NotesClipboard
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.*
import com.orgzly.android.ui.dialogs.TimestampDialogFragment
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.notes.quickbar.ItemGestureDetector
import com.orgzly.android.ui.notes.quickbar.QuickBarListener
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.refile.RefileFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.setup
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentNotesBookBinding


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
        BookAdapter.OnClickListener,
        QuickBarListener {

    private lateinit var binding: FragmentNotesBookBinding

    private var listener: Listener? = null

    private lateinit var viewAdapter: BookAdapter

    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: BookViewModel

    override fun getAdapter(): BookAdapter? {
        return if (::viewAdapter.isInitialized) viewAdapter else null
    }

    override fun getCurrentListener(): NotesFragment.Listener? {
        return listener
    }

    var currentBook: Book? = null

    private var mBookId: Long = 0

    private var mActionModeTag: String? = null

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    init {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context)

        listener = activity as Listener
        actionModeListener = activity as ActionModeListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProviders.of(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentNotesBookBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quickBars = QuickBars(binding.root.context, true)

        viewAdapter = BookAdapter(binding.root.context, this, quickBars, inBook = true)
        viewAdapter.setHasStableIds(true)

        // Restores selection, requires adapter
        super.onViewCreated(view, savedInstanceState)

        layoutManager = LinearLayoutManager(context)

        binding.fragmentNotesBookRecyclerView.let { rv ->
            rv.layoutManager = layoutManager
            rv.adapter = viewAdapter

            /*
             * Disable item animator (DefaultItemAnimator).
             * Animation is too slow.  And if animations are off in developer options, items flicker.
             * TODO: Do for query too?
             */
            rv.itemAnimator = null

            rv.addOnItemTouchListener(ItemGestureDetector(rv.context, object: ItemGestureDetector.Listener {
                override fun onFling(direction: Int, x: Float, y: Float) {
                    rv.findChildViewUnder(x, y)?.let { itemView ->
                        rv.findContainingViewHolder(itemView)?.let { vh ->
                            (vh as? NoteItemViewHolder)?.let {
                                quickBars.onFling(it, direction, this@BookFragment)
                            }
                        }
                    }
                }
            }))

//            val itemTouchHelper = NoteItemTouchHelper(true, object : NoteItemTouchHelper.Listener {
//                override fun onSwiped(viewHolder: NoteItemViewHolder, direction: Int) {
//                    listener?.onNoteOpen(viewHolder.itemId)
//                }
//            })
//
//            itemTouchHelper.attachToRecyclerView(rv)
        }

        binding.swipeContainer.setup()
    }

    override fun onQuickBarButtonClick(buttonId: Int, itemId: Long) {
        handleActionItemClick(buttonId, actionModeListener?.actionMode, setOf(itemId))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        viewModel.viewState.observe(viewLifecycleOwner, Observer { state ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed load state: $state")

            binding.fragmentBookViewFlipper.apply {
                displayedChild = when (state) {
                    BookViewModel.ViewState.LOADING -> 0
                    BookViewModel.ViewState.LOADED -> 1
                    BookViewModel.ViewState.EMPTY -> 2
                    BookViewModel.ViewState.DOES_NOT_EXIST -> 3
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
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Submitting list")
                viewAdapter.submitList(notes)

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

                scrollToNoteIfSet(arguments?.getLong(ARG_NOTE_ID, 0) ?: 0)
            }

            updateViewState(notes)
        })

        viewModel.refileRequestEvent.observeSingle(viewLifecycleOwner, Observer {
            RefileFragment.getInstance(it.selected, it.count)
                    .show(requireFragmentManager(), RefileFragment.FRAGMENT_TAG)
        })

        viewModel.notesDeleteRequest.observeSingle(viewLifecycleOwner, Observer { pair ->
            val ids = pair.first
            val count = pair.second

            val question = resources.getQuantityString(
                    R.plurals.delete_note_or_notes_with_count_question, count, count)

            dialog = AlertDialog.Builder(context)
                    .setTitle(question)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        listener?.onNotesDeleteRequest(mBookId, ids)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()
        })
    }

    private fun updateViewState(notes: List<NoteView>?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        if (currentBook == null) {
            viewModel.setViewState(BookViewModel.ViewState.DOES_NOT_EXIST)

        } else if (notes == null) {
            viewModel.setViewState(BookViewModel.ViewState.LOADING)

        } else if (viewAdapter.isPrefaceDisplayed() || !notes.isNullOrEmpty()) {
            viewModel.setViewState(BookViewModel.ViewState.LOADED)

        } else {
            viewModel.setViewState(BookViewModel.ViewState.EMPTY)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
    }

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
    }

    private fun parseArguments() {
        arguments?.let {
            require(it.containsKey(ARG_BOOK_ID)) {
                "No book id passed"
            }

            mBookId = it.getLong(ARG_BOOK_ID)

            require(mBookId > 0) {
                "Passed book id $mBookId is not valid"
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

        // Hide paste button if clipboard is empty, update title if not
        menu.findItem(R.id.book_actions_paste)?.apply {
            val count = NotesClipboard.count()

            if (count == 0) {
                isVisible = false

            } else {
                title = resources.getQuantityString(
                        R.plurals.paste_note_or_notes_with_count, count, count)

                isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        when (item.itemId) {
            R.id.books_options_menu_item_cycle_visibility -> {
                viewModel.cycleVisibility()
                return true
            }

            R.id.book_actions_paste -> {
                pasteNotes(Place.UNDER, 0)
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

    private fun newNoteRelativeToSelection(place: Place, noteId: Long) {
        listener?.onNoteNewRequest(NotePlace(mBookId, noteId, place))
    }

    private fun moveNotes(offset: Int) {
        /* Sanity check. Should not ever happen. */
        if (viewAdapter.getSelection().count == 0) {
            Log.e(TAG, "Trying to move notes up while there are no notes selected")
            return
        }

        listener?.onNotesMoveRequest(mBookId, viewAdapter.getSelection().getIds(), offset)
    }

    /**
     * Paste notes.
     * @param place [Place]
     */
    private fun pasteNotes(place: Place, noteId: Long) {
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        listener?.onNotesPasteRequest(mBookId, noteId, place)
    }

    fun scrollToNoteIfSet(noteId: Long) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId)

        if (noteId > 0) {
            val startedAt = System.currentTimeMillis()

            for (i in 0 until viewAdapter.itemCount) {
                val id = viewAdapter.getItemId(i)

                if (id == noteId) {
                    scrollToPosition(i)

                    binding.fragmentNotesBookRecyclerView.post {
                        spotlightScrolledToView(i)
                    }

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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position)
        layoutManager.scrollToPositionWithOffset(position, 0)
    }

    private fun spotlightScrolledToView(position: Int) {
        layoutManager.findViewByPosition(position)?.let {
            highlightScrolledToView(it)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun highlightScrolledToView(view: View) {
        val selectionBgColor = view.context.styledAttributes(R.styleable.ColorScheme) { typedArray ->
            typedArray.getColor(R.styleable.ColorScheme_item_spotlighted_bg_color, 0)
        }

        view.setBackgroundColor(selectionBgColor)

        // Reset background color on touch
        (activity as? CommonActivity)?.apply {
            runOnTouchEvent = Runnable {
                view.setBackgroundColor(0)
                runOnTouchEvent = null
            }
        }
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
        viewModel.requestNotesDelete(ids)
    }

    override fun getCurrentDrawerItemId(): String {
        return getDrawerItemId(mBookId)
    }

    override fun onNoteClick(view: View, position: Int, noteView: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            if (viewAdapter.getSelection().count > 0) {
                toggleNoteSelection(position, noteView)
            } else {
                openNote(noteView.note.id)
            }
        } else {
            toggleNoteSelection(position, noteView)
        }
    }

    override fun onNoteLongClick(view: View, position: Int, noteView: NoteView) {
        if (!AppPreferences.isReverseNoteClickAction(context)) {
            toggleNoteSelection(position, noteView)
        } else {
            openNote(noteView.note.id)
        }
    }

    private fun openNote(id: Long) {
        listener?.onNoteOpen(id)
    }

    private fun toggleNoteSelection(position: Int, noteView: NoteView) {
        val noteId = noteView.note.id

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, listener, noteView)

        viewAdapter.getSelection().toggle(noteId)
        viewAdapter.notifyItemChanged(position)

        actionModeListener?.updateActionModeForSelection(
                viewAdapter.getSelection().count, this)
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
            inflater.inflate(R.menu.book_cab_moving, menu)
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

        handleActionItemClick(menuItem.itemId, actionMode, viewAdapter.getSelection().getIds())

        return true
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

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
        listOf(R.id.bottom_action_bar_new).forEach { id ->
            toolbar.menu.findItem(id)?.isVisible = viewAdapter.getSelection().count <= 1
        }

        ActivityUtils.distributeToolbarItems(activity, toolbar)
    }

    override fun onBottomActionItemClicked(id: Int) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, id)

        handleActionItemClick(id, actionModeListener?.actionMode, viewAdapter.getSelection().getIds())
    }

    private fun handleActionItemClick(actionId: Int, actionMode: ActionMode?, ids: Set<Long>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionId, ids)

        if (ids.isEmpty()) {
            Log.e(TAG, "Cannot handle action when there are no items selected")
            actionMode?.finish()
            return
        }

        when (actionId) {
            R.id.quick_bar_open -> {
                openNote(ids.first())
            }

            R.id.quick_bar_new_above,
            R.id.bottom_action_bar_new_above -> {
                newNoteRelativeToSelection(Place.ABOVE, ids.first())
                actionMode?.finish()
            }

            R.id.quick_bar_new_under,
            R.id.bottom_action_bar_new_under -> {
                newNoteRelativeToSelection(Place.UNDER, ids.first())
                actionMode?.finish()
            }

            R.id.quick_bar_new_below,
            R.id.bottom_action_bar_new_below -> {
                newNoteRelativeToSelection(Place.BELOW, ids.first())
                actionMode?.finish()
            }

            R.id.book_cab_move -> {
                actionMode?.tag = "M"
                actionMode?.invalidate()
            }

            in scheduledTimeButtonIds(),
            in deadlineTimeButtonIds() ->
                displayTimestampDialog(actionId, ids)

            R.id.quick_bar_clock_in ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 0)
            R.id.quick_bar_clock_out ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 1)
            R.id.quick_bar_clock_cancel ->
                sharedMainActivityViewModel.clockingUpdateRequest(ids, 2)

            R.id.quick_bar_delete,
            R.id.book_cab_delete_note -> {
                delete(ids)

                // TODO: Wait for user confirmation (dialog close) before doing this
                actionMode?.finish()
            }

            R.id.book_cab_cut -> {
                listener?.onNotesCutRequest(mBookId, ids)
                actionMode?.finish()
            }

            R.id.book_cab_copy -> {
                listener?.onNotesCopyRequest(mBookId, ids)
                actionMode?.finish()
            }

            R.id.book_cab_paste_above -> {
                pasteNotes(Place.ABOVE, ids.first())
                actionMode?.finish()
            }

            R.id.quick_bar_refile,
            R.id.book_cab_refile ->
                viewModel.refile(ids)

            R.id.book_cab_paste_under -> {
                pasteNotes(Place.UNDER, ids.first())
                actionMode?.finish()
            }

            R.id.book_cab_paste_below -> {
                pasteNotes(Place.BELOW, ids.first())
                actionMode?.finish()
            }

            R.id.notes_action_move_up ->
                moveNotes(-1)

            R.id.notes_action_move_down ->
                moveNotes(1)

            R.id.notes_action_move_left ->
                listener?.onNotesPromoteRequest(ids)

            R.id.notes_action_move_right ->
                listener?.onNotesDemoteRequest(ids)

            R.id.quick_bar_state,
            R.id.bottom_action_bar_state ->
                listener?.let {
                    openNoteStateDialog(it, ids, null)
                }

            R.id.quick_bar_done,
            R.id.bottom_action_bar_done -> {
                listener?.onStateToggleRequest(ids)
            }

            R.id.quick_bar_focus,
            R.id.bottom_action_bar_focus ->
                listener?.onNoteFocusInBookRequest(ids.first())
        }
    }

    interface Listener : NotesFragment.Listener {
        fun onBookPrefaceEditRequest(book: Book)

        fun onBookPrefaceUpdate(bookId: Long, preface: String)

        fun onNotesDeleteRequest(bookId: Long, noteIds: Set<Long>)

        fun onNotesCutRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesCopyRequest(bookId: Long, noteIds: Set<Long>)
        fun onNotesPasteRequest(bookId: Long, noteId: Long, place: Place)

        fun onNotesPromoteRequest(noteIds: Set<Long>)
        fun onNotesDemoteRequest(noteIds: Set<Long>)

        fun onNotesMoveRequest(bookId: Long, noteIds: Set<Long>, offset: Int)
    }

    companion object {

        private val TAG = BookFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmField
        val FRAGMENT_TAG: String = BookFragment::class.java.name

        /* Arguments. */
        private const val ARG_BOOK_ID = "bookId"
        private const val ARG_NOTE_ID = "noteId"

        private val ITEMS_HIDDEN_ON_MULTIPLE_SELECTED_NOTES = intArrayOf(
                R.id.book_cab_paste)

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

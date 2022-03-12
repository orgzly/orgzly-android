package com.orgzly.android.ui.notes.book

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.NotesFragment
import com.orgzly.android.ui.notes.quickbar.ItemGestureDetector
import com.orgzly.android.ui.notes.quickbar.QuickBarListener
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.refile.RefileFragment
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.setup
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentBookBinding


/**
 * Displays all notes from the notebook.
 * Allows moving, cutting, pasting etc.
 */
class BookFragment :
        NotesFragment(),
        TimestampDialogFragment.OnDateTimeSetListener,
        DrawerItem,
        BookAdapter.OnClickListener,
        QuickBarListener {

    private lateinit var binding: FragmentBookBinding

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

    // TODO: Move to ViewModel

    var currentBook: Book? = null

    private var mBookId: Long = 0

    private val appBarBackPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.appBar.handleOnBackPressed()
        }
    }

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        parseArguments()

        val factory = BookViewModelFactory.forBook(dataRepository, mBookId)
        viewModel = ViewModelProvider(this, factory).get(BookViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this, appBarBackPressHandler)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentBookBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quickBars = QuickBars(binding.root.context, true)

        viewAdapter = BookAdapter(binding.root.context, this, quickBars, inBook = true).apply {
            setHasStableIds(true)
        }

        // Restores selection, requires adapter
        super.onViewCreated(view, savedInstanceState)

        layoutManager = LinearLayoutManager(context)

        binding.fragmentBookRecyclerView.let { rv ->
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
        handleActionItemClick(buttonId, setOf(itemId))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        viewModel.flipperDisplayedChild.observe(viewLifecycleOwner, Observer { child ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Observed flipper displayed child: $child")

            binding.fragmentBookViewFlipper.apply {
                displayedChild = when (child) {
                    BookViewModel.FlipperDisplayedChild.LOADING -> 0
                    BookViewModel.FlipperDisplayedChild.LOADED -> 1
                    BookViewModel.FlipperDisplayedChild.EMPTY -> 2
                    BookViewModel.FlipperDisplayedChild.DOES_NOT_EXIST -> 3
                    else -> 1
                }
            }
        })

        viewModel.title.observe(viewLifecycleOwner, Observer { title ->
            binding.toolbar.title = title
        })

        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Observed data: book ${data.book} and ${data.notes?.size} notes")

            val book = data.book
            val notes = data.notes

            this.currentBook = book

            viewModel.setTitle(BookUtils.getFragmentTitleForBook(currentBook))

            viewAdapter.setPreface(book)

            if (notes != null) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Submitting list")
                viewAdapter.submitList(notes)

                val ids = notes.mapTo(hashSetOf()) { it.note.id }

                viewAdapter.getSelection().removeNonExistent(ids)

                viewModel.appBar.toState(viewAdapter.getSelection().count)

                scrollToNoteIfSet(arguments?.getLong(ARG_NOTE_ID, 0) ?: 0)
            }

            setFlipperDisplayedChild(notes)
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

        viewModel.appBar.state.observeSingle(viewLifecycleOwner) { state ->
            when (state) {
                is AppBar.State.Default, null -> {
                    appBarToDefault()

                    sharedMainActivityViewModel.unlockDrawer()

                    appBarBackPressHandler.isEnabled = false

                    // Hide bar's title
                    binding.bottomAppBarTitle.visibility = View.GONE

                    binding.toolbar.menu.clear()

                    viewModel.setTitle(BookUtils.getFragmentTitleForBook(currentBook))
                }

                is AppBar.State.MainSelection -> {
                    appBarToMainSelection()

                    sharedMainActivityViewModel.lockDrawer()

                    appBarBackPressHandler.isEnabled = true

                    // Set bar's title
                    binding.bottomAppBarTitle.run {
                        text = viewAdapter.getSelection().count.toString()
                        visibility = View.VISIBLE
                    }

                    binding.toolbar.menu.clear()
                    binding.toolbar.inflateMenu(R.menu.book_cab_top)
                    hideMenuItemsBasedOnSelection(binding.toolbar.menu)
                    binding.toolbar.setOnMenuItemClickListener { menuItem ->
                        handleActionItemClick(menuItem.itemId, viewAdapter.getSelection().getIds())
                        true
                    }

                    viewModel.hideTitle()
                }

                is AppBar.State.NextSelection -> {
                    appBarToNextSelection()

                    sharedMainActivityViewModel.lockDrawer()

                    appBarBackPressHandler.isEnabled = true

                    // Set bar's title
                    binding.bottomAppBarTitle.run {
                        text = viewAdapter.getSelection().count.toString()
                        visibility = View.VISIBLE
                    }

                    binding.toolbar.menu.clear()
                    binding.toolbar.inflateMenu(R.menu.book_cab_moving)
                    hideMenuItemsBasedOnSelection(binding.toolbar.menu)
                    binding.toolbar.setOnMenuItemClickListener { menuItem ->
                        handleActionItemClick(menuItem.itemId, viewAdapter.getSelection().getIds())
                        true
                    }

                    viewModel.hideTitle()
                }
            }
        }
    }

    private fun setFlipperDisplayedChild(notes: List<NoteView>?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        if (currentBook == null) {
            viewModel.setFlipperDisplayedChild(BookViewModel.FlipperDisplayedChild.DOES_NOT_EXIST)

        } else if (notes == null) {
            viewModel.setFlipperDisplayedChild(BookViewModel.FlipperDisplayedChild.LOADING)

        } else if (viewAdapter.isPrefaceDisplayed() || !notes.isNullOrEmpty()) {
            viewModel.setFlipperDisplayedChild(BookViewModel.FlipperDisplayedChild.LOADED)

        } else {
            viewModel.setFlipperDisplayedChild(BookViewModel.FlipperDisplayedChild.EMPTY)
        }
    }

    override fun onResume() {
        super.onResume()

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)
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

//    override fun onPrepareOptionsMenu(menu: Menu) {
//        super.onPrepareOptionsMenu(menu)
//
//
//    }

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

                    binding.fragmentBookRecyclerView.post {
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

        viewAdapter.getSelection().toggle(noteId)
        viewAdapter.notifyItemChanged(position)

        viewModel.appBar.toState(viewAdapter.getSelection().count)
    }

    override fun onPrefaceClick() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        currentBook?.let {
            listener?.onBookPrefaceEditRequest(it)
        }
    }

    private fun appBarToDefault() {
        // Clear selection
        viewAdapter.getSelection().clear()
        viewAdapter.notifyDataSetChanged() // FIXME

        binding.bottomAppBar.run {
            replaceMenu(R.menu.book_actions)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_menu_24dp, 0)
            })

            setNavigationOnClickListener {
                sharedMainActivityViewModel.openDrawer()
            }

            ActivityUtils.keepScreenOnUpdateMenuItem(
                activity,
                menu,
                menu.findItem(R.id.books_options_keep_screen_on))


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

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.books_options_menu_item_cycle_visibility -> {
                        viewModel.cycleVisibility()
                    }

                    R.id.book_actions_paste -> {
                        pasteNotes(Place.UNDER, 0)
                    }

                    R.id.books_options_menu_book_preface -> {
                        onPrefaceClick()
                    }

                    R.id.books_options_keep_screen_on -> {
                        // TODO: Pass menu, books_options_keep_screen_on should be inside keepScreenOnToggle only
                        val item = menu.findItem(R.id.books_options_keep_screen_on)
                        dialog = ActivityUtils.keepScreenOnToggle(activity, item)
                    }

                    R.id.activity_action_settings -> {
                        startActivity(Intent(context, SettingsActivity::class.java))
                    }
                }

                true
            }

            (requireActivity() as? MainActivity)?.setupSearchView(menu) // FIXME
        }

        binding.fab.run {
            if (currentBook != null) {
                setOnClickListener {
                    listener?.onNoteNewRequest(NotePlace(mBookId))
                }
                show()
            } else {
                hide()
            }
        }
    }

    private fun appBarToMainSelection() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.book_cab)

            hideMenuItemsBasedOnSelection(menu)

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

        binding.fab.hide()
    }

    private fun appBarToNextSelection() {
        binding.bottomAppBar.run {
            replaceMenu(R.menu.book_cab)

            hideMenuItemsBasedOnSelection(menu)

            setNavigationIcon(context.styledAttributes(R.styleable.Icons) { typedArray ->
                typedArray.getResourceId(R.styleable.Icons_ic_arrow_back_24dp, 0)
            })

            setNavigationOnClickListener {
                viewModel.appBar.toMainSelection()
            }

            setOnMenuItemClickListener { menuItem ->
                handleActionItemClick(menuItem.itemId, viewAdapter.getSelection().getIds())
                false
            }
        }

        binding.fab.hide()
    }

    private fun hideMenuItemsBasedOnSelection(menu: Menu) {
        // Hide buttons that can't be used when multiple notes are selected
        for (id in listOf(R.id.paste, R.id.new_note)) {
            menu.findItem(id)?.isVisible = viewAdapter.getSelection().count == 1
        }
    }

    private fun handleActionItemClick(actionId: Int, ids: Set<Long>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, actionId, ids)

        if (ids.isEmpty()) {
            Log.e(TAG, "Cannot handle action when there are no items selected")
            viewModel.appBar.toDefault()
            return
        }

        when (actionId) {
            R.id.quick_bar_open -> {
                openNote(ids.first())
            }

            R.id.quick_bar_new_above,
            R.id.new_note_above -> {
                newNoteRelativeToSelection(Place.ABOVE, ids.first())
                viewModel.appBar.toDefault()
            }

            R.id.quick_bar_new_under,
            R.id.new_note_under -> {
                newNoteRelativeToSelection(Place.UNDER, ids.first())
                viewModel.appBar.toDefault()
            }

            R.id.quick_bar_new_below,
            R.id.new_note_below -> {
                newNoteRelativeToSelection(Place.BELOW, ids.first())
                viewModel.appBar.toDefault()
            }

            R.id.move -> {
                viewModel.appBar.toNextSelection()
            }

            in scheduledTimeButtonIds(),
            in deadlineTimeButtonIds() ->
                displayTimestampDialog(actionId, ids)

            R.id.quick_bar_delete,
            R.id.delete_note -> {
                delete(ids)

                // TODO: Wait for user confirmation (dialog close) before doing this
                // TODO: Don't do it if canceled
                viewModel.appBar.toDefault()
            }

            R.id.cut -> {
                listener?.onNotesCutRequest(mBookId, ids)
                viewModel.appBar.toDefault()
            }

            R.id.copy -> {
                listener?.onNotesCopyRequest(mBookId, ids)
                viewModel.appBar.toDefault()
            }

            R.id.paste_above -> {
                pasteNotes(Place.ABOVE, ids.first())
                viewModel.appBar.toDefault()
            }

            R.id.quick_bar_refile,
            R.id.refile ->
                viewModel.refile(ids)

            R.id.paste_under -> {
                pasteNotes(Place.UNDER, ids.first())
                viewModel.appBar.toDefault()
            }

            R.id.paste_below -> {
                pasteNotes(Place.BELOW, ids.first())
                viewModel.appBar.toDefault()
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
            R.id.state ->
                listener?.let {
                    openNoteStateDialog(it, ids, null)
                }

            R.id.quick_bar_done,
            R.id.toggle_state -> {
                listener?.onStateToggleRequest(ids)
            }

            R.id.quick_bar_focus,
            R.id.focus ->
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

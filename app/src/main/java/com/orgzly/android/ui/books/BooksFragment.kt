package com.orgzly.android.ui.books


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.Fab
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.notes.query.QueryFragment
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.usecase.BookDelete
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * Displays all notebooks.
 * Allows creating new, deleting, renaming, setting links etc.
 */
class BooksFragment :
        DaggerFragment(),
        Fab,
        DrawerItem,
        OnViewHolderClickListener<BookView> {

    private lateinit var viewAdapter: BooksAdapter

    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()

    private var dialog: AlertDialog? = null

    private var listener: Listener? = null

    private lateinit var viewFlipper: ViewFlipper

    private var mAddOptions = true
    private var mShowContextMenu = true

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: BooksViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = activity as Listener

        parseArguments()
    }

    private fun parseArguments() {
        if (arguments == null) {
            throw IllegalArgumentException("No arguments found to " + BooksFragment::class.java.simpleName)
        }

        mAddOptions = arguments?.getBoolean(ARG_ADD_OPTIONS) ?: true
        mShowContextMenu = arguments?.getBoolean(ARG_SHOW_CONTEXT_MENU) ?: true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, savedInstanceState)
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(mAddOptions)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_books, container, false)

        viewFlipper = view.findViewById(R.id.fragment_books_view_flipper)

        viewAdapter = BooksAdapter(this)
        viewAdapter.setHasStableIds(true)

        view.findViewById<RecyclerView>(R.id.fragment_books_recycler_view).let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = viewAdapter
        }

        return view
    }

    override fun onClick(view: View, position: Int, item: BookView) {
        if (actionMode == null) {
            listener?.onBookClicked(item.book.id)

        } else {
            viewAdapter.getSelection().toggle(item.book.id)
            viewAdapter.notifyItemChanged(position)

            if (viewAdapter.getSelection().count == 0) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }
    }

    override fun onLongClick(view: View, position: Int, item: BookView) {
        if (!mShowContextMenu) {
            listener?.onBookClicked(item.book.id)
            return
        }

        viewAdapter.getSelection().toggle(item.book.id)
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

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, mode, item)

            val ids = viewAdapter.getSelection().getIds()

            if (ids.isEmpty()) {
                Log.e(TAG, "Cannot handle action when there are no items selected")

            } else {
                val bookId = ids.first()

                when (item.itemId) {
                    R.id.books_context_menu_rename -> {
                        viewModel.renameBookRequest(bookId)
                    }

                    R.id.books_context_menu_set_link -> {
                        listener?.onBookLinkSetRequest(bookId)
                    }

                    R.id.books_context_menu_force_save -> {
                        listener?.onForceSaveRequest(bookId)
                    }

                    R.id.books_context_menu_force_load -> {
                        listener?.onForceLoadRequest(bookId)
                    }

                    R.id.books_context_menu_export -> {
                        listener?.onBookExportRequest(bookId)
                    }

                    R.id.books_context_menu_delete -> {
                        viewModel.deleteBookRequest(bookId)
                    }

                    else -> {
                    }
                }
            }

            actionMode?.finish()

            return true
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, mode, menu)
            mode?.menuInflater?.inflate(R.menu.books_context, menu)

            sharedMainActivityViewModel.lockDrawer()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, mode, menu, viewAdapter.getSelection().count)

            val selected = viewAdapter.getSelection().count

            if (selected > 0) {
                /* Update action mode with the number of selected items. */
                // Disabled while allowing only one item to be selected
                // mode?.title = viewAdapter.getSelectedItemCount().toString()
            } else {
                mode?.finish()
            }

            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, mode)
            viewAdapter.getSelection().clear()
            viewAdapter.notifyDataSetChanged() // FIXME
            actionMode = null
            sharedMainActivityViewModel.unlockDrawer()
        }

    }

    private fun deleteBookDialog(book: BookView) {
        val view = View.inflate(context, R.layout.dialog_book_delete, null)
        val checkBox = view.findViewById<CheckBox>(R.id.dialog_book_delete_checkbox)
        val textView = view.findViewById<TextView>(R.id.dialog_book_delete_text)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            activity?.obtainStyledAttributes(
                    intArrayOf(R.attr.text_primary_color, R.attr.text_disabled_color))?.let {

                val color = if (isChecked) {
                    it.getColor(0, 0)
                } else {
                    it.getColor(1, 0)
                }

                it.recycle()

                textView.setTextColor(color)
            }
        }

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val deleteLinked = checkBox.isChecked
                    viewModel.deleteBook(book.book.id, deleteLinked)
                }
            }
        }

        val builder = AlertDialog.Builder(context)
                .setTitle(getString(R.string.delete_with_quoted_argument, book.book.name))
                .setPositiveButton(R.string.delete, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)

        if (book.syncedTo != null) {
            textView.text = book.syncedTo.uri.toString()
            builder.setView(view)
        }

        dialog = builder.show()
    }

    private fun renameBookDialog(book: BookView) {

        val dialogView = View.inflate(context, R.layout.dialog_book_rename, null)
        val nameInputLayout = dialogView.findViewById<TextInputLayout>(R.id.name_input_layout)
        val nameView = dialogView.findViewById<EditText>(R.id.name)

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val name = nameView.text.toString()

                    if (!TextUtils.isEmpty(name)) {
                        nameInputLayout.error = null

                        viewModel.renameBook(book, name)

                    } else {
                        nameInputLayout.error = getString(R.string.can_not_be_empty)
                    }
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }

        val dialogBuilder = AlertDialog.Builder(context)
                .setTitle(getString(R.string.rename_book, MiscUtils.quotedString(book.book.name)))
                .setPositiveButton(R.string.rename, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .setView(dialogView)

        nameView.setText(book.book.name)

        val d = dialogBuilder.create()

        /* Finish on keyboard action press. */
        nameView.setOnEditorActionListener { _, _, _ ->
            d.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            true
        }

        d.setOnShowListener { ActivityUtils.openSoftKeyboard(activity, nameView) }
        d.setOnDismissListener { ActivityUtils.closeSoftKeyboard(activity) }

        // Disable positive button if value is empty or same
        nameView.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(str: Editable?) {
                val emptyOrSame = TextUtils.isEmpty(str) || str != null && str.toString() == book.book.name

                d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !emptyOrSame
            }
        })

        d.show()

        d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        dialog = d
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, view, savedInstanceState)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        val factory = BooksViewModelFactory.getInstance(dataRepository)
        viewModel = ViewModelProviders.of(this, factory).get(BooksViewModel::class.java)

        viewModel.viewState.observe(viewLifecycleOwner, Observer {
            viewFlipper.displayedChild = when (it) {
                BooksViewModel.ViewState.LOADING -> 0
                BooksViewModel.ViewState.LOADED -> 1
                BooksViewModel.ViewState.EMPTY -> 2
                else -> 1
            }
        })

        viewModel.books.observe(viewLifecycleOwner, Observer { books ->
            viewAdapter.submitList(books)

            val ids = books.mapTo(hashSetOf()) { it.book.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            actionMode?.invalidate()
        })

        viewModel.bookDeleteRequestEvent.observeSingle(viewLifecycleOwner, Observer { bookView ->
            if (bookView != null) {
                deleteBookDialog(bookView)
            }
        })

        viewModel.bookRenameRequestEvent.observeSingle(viewLifecycleOwner, Observer { bookView ->
            if (bookView != null) {
                renameBookDialog(bookView)
            }
        })

        viewModel.bookDeletedEvent.observeSingle(viewLifecycleOwner, Observer {
            CommonActivity.showSnackbar(context, R.string.message_book_deleted)
        })

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            if (error is BookDelete.NotFound) {
                CommonActivity.showSnackbar(context, resources.getString(
                        R.string.message_deleting_book_failed, error.localizedMessage))

            } else if (error != null) {
                CommonActivity.showSnackbar(context, (error.cause ?: error).localizedMessage)
            }
        })
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, savedInstanceState)
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId)
        super.onResume()

        announceChangesToActivity()

        // Re-query if preference changed
        viewModel.refresh(AppPreferences.notebooksSortOrder(context))
    }

    override fun onPause() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onPause()

        actionMode?.finish()
    }

    override fun onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId)
        super.onDestroyView()

        dialog?.dismiss()
        dialog = null
    }

    override fun onDetach() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId)
        super.onDetach()

        listener = null
    }

    /**
     * Callback for options menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(drawerItemId, menu, inflater)

        inflater.inflate(R.menu.books_actions, menu)
    }

    /**
     * Callback for options menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.books_options_menu_item_import_book -> {
                listener?.onBookImportRequest()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFabAction(): Runnable {
        return Runnable {
            listener?.onBookCreateRequest()
        }
    }

    private fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                getString(R.string.notebooks),
                null,
                0) // No books ever selected, as we're using the old floating context menu.
    }

    override fun getCurrentDrawerItemId(): String {
        return drawerItemId
    }

    interface Listener {
        /**
         * Request for creating new book.
         */
        fun onBookCreateRequest()

        /**
         * Click on a book item has been performed.
         *
         * @param bookId
         */
        fun onBookClicked(bookId: Long)

        fun onBookLinkSetRequest(bookId: Long)

        fun onForceSaveRequest(bookId: Long)

        fun onForceLoadRequest(bookId: Long)

        fun onBookExportRequest(bookId: Long)

        fun onBookImportRequest()
    }

    companion object {
        private val TAG = BooksFragment::class.java.name

        val drawerItemId: String = BooksFragment::class.java.name

        /**
         * Name used for [android.app.FragmentManager].
         */
        val FRAGMENT_TAG: String = BooksFragment::class.java.name

        private const val ARG_ADD_OPTIONS = "add_options"
        private const val ARG_SHOW_CONTEXT_MENU = "show_context_menu"

        val instance: BooksFragment
            get() = getInstance(true, true)

        fun getInstance(addOptions: Boolean, showContextMenu: Boolean): BooksFragment {
            val fragment = BooksFragment()
            val args = Bundle()

            args.putBoolean(ARG_ADD_OPTIONS, addOptions)
            args.putBoolean(ARG_SHOW_CONTEXT_MENU, showContextMenu)

            fragment.arguments = args
            return fragment
        }
    }
}

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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookFormat
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.Fab
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.setup
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.usecase.BookDelete
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.databinding.DialogBookDeleteBinding
import com.orgzly.databinding.DialogBookRenameBinding
import com.orgzly.databinding.FragmentBooksBinding
import javax.inject.Inject

/**
 * Displays all notebooks.
 * Allows creating new, deleting, renaming, setting links etc.
 */
class BooksFragment : Fragment(), Fab, DrawerItem, OnViewHolderClickListener<BookView> {

    private lateinit var binding: FragmentBooksBinding

    private lateinit var viewAdapter: BooksAdapter

    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()

    private var dialog: AlertDialog? = null

    private var listener: Listener? = null

    private var withOptionsMenu = true
    private var withActionBar = true

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: BooksViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        listener = activity as Listener

        parseArguments()
    }

    private fun parseArguments() {
        requireNotNull(arguments) { "No arguments found to " + BooksFragment::class.java.simpleName }

        withOptionsMenu = arguments?.getBoolean(ARG_WITH_OPTIONS_MENU) ?: true
        withActionBar = arguments?.getBoolean(ARG_WITH_ACTION_BAR) ?: true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(withOptionsMenu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentBooksBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        viewAdapter = BooksAdapter(this)
        viewAdapter.setHasStableIds(true)

        binding.fragmentBooksRecyclerView.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = viewAdapter
        }

        binding.swipeContainer.setup()
    }

    override fun onClick(view: View, position: Int, item: BookView) {
        if (actionMode == null) {
            listener?.onBookClicked(item.book.id)

        } else {
            viewAdapter.getSelection().toggleSingleSelect(item.book.id)
            viewAdapter.notifyDataSetChanged() // FIXME

            if (viewAdapter.getSelection().count == 0) {
                actionMode?.finish()
            } else {
                actionMode?.invalidate()
            }
        }
    }

    override fun onLongClick(view: View, position: Int, item: BookView) {
        if (!withActionBar) {
            listener?.onBookClicked(item.book.id)
            return
        }

        viewAdapter.getSelection().toggleSingleSelect(item.book.id)
        viewAdapter.notifyDataSetChanged() // FIXME

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

            val bookId = viewAdapter.getSelection().getOnly()

            if (bookId == null) {
                Log.e(TAG, "Cannot handle action when there are no items selected")

            } else {
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
                        viewModel.exportBookRequest(bookId, BookFormat.ORG)
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
        val dialogBinding = DialogBookDeleteBinding.inflate(LayoutInflater.from(context))

        dialogBinding.deleteLinkedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            activity?.apply {
                val color = styledAttributes(R.styleable.ColorScheme) { typedArray ->
                    val index = if (isChecked) {
                        R.styleable.ColorScheme_text_primary_color
                    } else {
                        R.styleable.ColorScheme_text_disabled_color
                    }

                    typedArray.getColor(index, 0)
                }
                dialogBinding.deleteLinkedUrl.setTextColor(color)
            }
        }

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val deleteLinked = dialogBinding.deleteLinkedCheckbox.isChecked
                    viewModel.deleteBook(book.book.id, deleteLinked)
                }
            }
        }

        val builder = AlertDialog.Builder(context)
                .setTitle(getString(R.string.delete_with_quoted_argument, book.book.name))
                .setPositiveButton(R.string.delete, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)

        if (book.syncedTo != null) {
            dialogBinding.deleteLinkedUrl.text = book.syncedTo.uri.toString()
            builder.setView(dialogBinding.root)
        }

        dialog = builder.show()
    }

    private fun renameBookDialog(book: BookView) {
        val dialogBinding = DialogBookRenameBinding.inflate(LayoutInflater.from(context))

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val name = dialogBinding.name.text.toString()

                    if (!TextUtils.isEmpty(name)) {
                        dialogBinding.nameInputLayout.error = null

                        viewModel.renameBook(book, name)

                    } else {
                        dialogBinding.nameInputLayout.error = getString(R.string.can_not_be_empty)
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
                .setView(dialogBinding.root)

        dialogBinding.name.setText(book.book.name)

        val d = dialogBuilder.create()

        /* Finish on keyboard action press. */
        dialogBinding.name.setOnEditorActionListener { _, _, _ ->
            d.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            true
        }

        d.setOnShowListener { ActivityUtils.openSoftKeyboard(activity, dialogBinding.name) }
        d.setOnDismissListener { ActivityUtils.closeSoftKeyboard(activity) }

        // Disable positive button if value is empty or same
        dialogBinding.name.addTextChangedListener(object : TextWatcher {
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        val factory = BooksViewModelFactory.getInstance(dataRepository)
        viewModel = ViewModelProvider(this, factory).get(BooksViewModel::class.java)

        viewModel.viewState.observe(viewLifecycleOwner, Observer {
            binding.fragmentBooksViewFlipper.displayedChild = when (it) {
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

        viewModel.bookExportRequestEvent.observeSingle(viewLifecycleOwner, Observer { (book, format) ->
            listener?.onBookExportRequest(book, format)
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
        super.onViewStateRestored(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        announceChangesToActivity()

        // Re-query if preference changed
        viewModel.refresh(AppPreferences.notebooksSortOrder(context))
    }

    override fun onPause() {
        super.onPause()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        actionMode?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        dialog?.dismiss()
        dialog = null
    }

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
    }

    /**
     * Callback for options menu.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

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

        fun onBookExportRequest(book: Book, format: BookFormat)

        fun onBookImportRequest()
    }

    companion object {
        private val TAG = BooksFragment::class.java.name

        val drawerItemId: String = BooksFragment::class.java.name

        /**
         * Name used for [android.app.FragmentManager].
         */
        val FRAGMENT_TAG: String = BooksFragment::class.java.name

        private const val ARG_WITH_OPTIONS_MENU = "with_options_menu"
        private const val ARG_WITH_ACTION_BAR = "with_action_bar"

        val instance: BooksFragment
            get() = getInstance(true, true)

        fun getInstance(withOptionsMenu: Boolean, withActionBar: Boolean): BooksFragment {
            val fragment = BooksFragment()
            val args = Bundle()

            args.putBoolean(ARG_WITH_OPTIONS_MENU, withOptionsMenu)
            args.putBoolean(ARG_WITH_ACTION_BAR, withActionBar)

            fragment.arguments = args
            return fragment
        }
    }
}

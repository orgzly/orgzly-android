package com.orgzly.android.ui.books

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookFormat
import com.orgzly.android.BookName
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.sync.SyncRunner
import com.orgzly.android.ui.CommonFragment
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.books.BooksViewModel.Companion.APP_BAR_DEFAULT_MODE
import com.orgzly.android.ui.books.BooksViewModel.Companion.APP_BAR_SELECTION_MODE
import com.orgzly.android.ui.dialogs.SimpleOneLinerDialog
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.main.setupSearchView
import com.orgzly.android.ui.repos.ReposActivity
import com.orgzly.android.ui.settings.SettingsActivity
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.ui.util.KeyboardUtils
import com.orgzly.android.ui.util.setup
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
class BooksFragment : CommonFragment(), DrawerItem, OnViewHolderClickListener<BookView> {

    private lateinit var binding: FragmentBooksBinding

    private lateinit var viewAdapter: BooksAdapter

    private var dialog: AlertDialog? = null

    private var listener: Listener? = null

    private var withActionBar = true

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private lateinit var viewModel: BooksViewModel

    private val appBarBackPressHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.appBar.handleOnBackPressed()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        listener = activity as Listener

        parseArguments()
    }

    private fun parseArguments() {
        requireNotNull(arguments) { "No arguments found to " + BooksFragment::class.java.simpleName }

        withActionBar = arguments?.getBoolean(ARG_WITH_ACTION_BAR) ?: true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        // Result from the dialog prompting for notebook name
        childFragmentManager.setFragmentResultListener("name-new-book", this) { _, result ->
            val bookName = result.getString("value", "")
            viewModel.createBook(bookName)
        }

        // Result from the dialog prompting for notebook name
        childFragmentManager.setFragmentResultListener("name-imported-book", this) { _, result ->
            val bookName = result.getString("value", "")
            val uri = result.getBundle("user-data")?.getParcelable<Uri>("uri")!!
            viewModel.importBook(uri, bookName)
        }

        val factory = BooksViewModelFactory.getInstance(dataRepository)
        viewModel = ViewModelProvider(this, factory).get(BooksViewModel::class.java)

        requireActivity().onBackPressedDispatcher.addCallback(this, appBarBackPressHandler)
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
        if (viewAdapter.getSelection().count == 0) {
            // No books selected
            listener?.onBookClicked(item.book.id)

        } else {
            // There are books selected
            viewAdapter.getSelection().toggleSingleSelect(item.book.id)
            viewAdapter.notifyDataSetChanged() // FIXME

            viewModel.appBar.toModeFromSelectionCount(viewAdapter.getSelection().count)
        }
    }

    override fun onLongClick(view: View, position: Int, item: BookView) {
        if (!withActionBar) {
            listener?.onBookClicked(item.book.id)
            return
        }

        viewAdapter.getSelection().toggleSingleSelect(item.book.id)
        viewAdapter.notifyDataSetChanged() // FIXME

        viewModel.appBar.toModeFromSelectionCount(viewAdapter.getSelection().count)
    }

    private fun topToolbarToDefault() {
        if (withActionBar) {
            binding.topToolbar.run {
                menu.clear()
                inflateMenu(R.menu.books_actions)

                setNavigationIcon(R.drawable.ic_menu)

                setNavigationOnClickListener {
                    sharedMainActivityViewModel.openDrawer()
                }

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.books_options_menu_item_import_book -> {
                            pickFileForBookImport.launch("*/*")
                        }

                        R.id.sync -> {
                            SyncRunner.startSync()
                        }

                        R.id.activity_action_settings -> {
                            startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    }
                    true
                }

                setOnClickListener {
                    binding.fragmentBooksRecyclerView.scrollToPosition(0)
                }

                title = getString(R.string.notebooks)

                requireActivity().setupSearchView(menu)
            }

        } else {
            binding.topToolbar.run {
                menu.clear()
                navigationIcon = null
                title = getString(R.string.select_notebook)
            }
        }
    }

    private fun topToolbarToMainSelection() {
        binding.topToolbar.run {
            menu.clear()
            inflateMenu(R.menu.books_cab)

            setNavigationIcon(R.drawable.ic_arrow_back)

            setNavigationOnClickListener {
                viewModel.appBar.toMode(APP_BAR_DEFAULT_MODE)
            }

            setOnMenuItemClickListener { menuItem ->
                val bookId = viewAdapter.getSelection().getOnly()

                if (bookId == null) {
                    Log.e(TAG, "Cannot handle action when there are no items selected")
                    return@setOnMenuItemClickListener true
                }

                when (menuItem.itemId) {
                    R.id.books_context_menu_rename -> {
                        viewModel.renameBookRequest(bookId)
                    }

                    R.id.books_context_menu_set_link -> {
                        viewModel.setBookLinkRequest(bookId)
                    }

                    R.id.books_context_menu_force_save -> {
                        dialog = MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.books_context_menu_item_force_save)
                            .setMessage(R.string.overwrite_remote_notebook_question)
                            .setPositiveButton(R.string.overwrite) { _, _ ->
                                viewModel.forceSaveBookRequest(bookId)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }

                    R.id.books_context_menu_force_load -> {
                        dialog = MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.books_context_menu_item_force_load)
                            .setMessage(R.string.overwrite_local_notebook_question)
                            .setPositiveButton(R.string.overwrite) { _, _ ->
                                viewModel.forceLoadBookRequest(bookId)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }

                    R.id.books_context_menu_export -> {
                        viewModel.exportBookRequest(bookId, BookFormat.ORG)
                    }

                    R.id.books_context_menu_delete -> {
                        viewModel.deleteBookRequest(bookId)
                    }
                }

                viewModel.appBar.toMode(APP_BAR_DEFAULT_MODE)

                true
            }

            setOnClickListener(null)

            title = viewAdapter.getSelection().count.toString()
        }
    }

    private val pickFileForBookExport =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri != null) {
                viewModel.exportBook(uri)
            } else {
                Log.w(TAG, "Export file not selected")
            }
        }

    private fun exportBook(book: Book, format: BookFormat) {
        val defaultFileName = BookName.fileName(book.name, format)
        pickFileForBookExport.launch(defaultFileName)
    }

    private fun deleteBookDialog(book: BookView) {
        val dialogBinding = DialogBookDeleteBinding.inflate(LayoutInflater.from(context))

        dialogBinding.deleteLinkedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.deleteLinkedUrl.isEnabled = isChecked
        }

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val deleteLinked = dialogBinding.deleteLinkedCheckbox.isChecked
                    viewModel.deleteBook(book.book.id, deleteLinked)
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
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

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
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

        d.setOnShowListener { KeyboardUtils.openSoftKeyboard(dialogBinding.name) }
        d.setOnDismissListener { KeyboardUtils.closeSoftKeyboard(activity) }

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

        viewModel.viewState.observe(viewLifecycleOwner, Observer {
            binding.fragmentBooksViewFlipper.displayedChild = when (it) {
                BooksViewModel.ViewState.LOADING -> 0
                BooksViewModel.ViewState.LOADED -> 1
                BooksViewModel.ViewState.EMPTY -> 2
                else -> 1
            }
        })

        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            viewAdapter.submitList(data)

            val ids = data.mapTo(hashSetOf()) { it.book.id }

            viewAdapter.getSelection().removeNonExistent(ids)

            viewModel.appBar.toModeFromSelectionCount(viewAdapter.getSelection().count)
        })


        viewModel.bookToDeleteEvent.observeSingle(viewLifecycleOwner, Observer { bookView ->
            if (bookView != null) {
                deleteBookDialog(bookView)
            }
        })

        viewModel.bookToRenameEvent.observeSingle(viewLifecycleOwner, Observer { bookView ->
            if (bookView != null) {
                renameBookDialog(bookView)
            }
        })

        viewModel.bookToExportEvent.observeSingle(viewLifecycleOwner, Observer { (book, format) ->
            exportBook(book, format)
        })

        viewModel.bookExportedEvent.observeSingle(viewLifecycleOwner, Observer { location ->
            activity?.showSnackbar(resources.getString(R.string.book_exported, location))
        })

        viewModel.bookDeletedEvent.observeSingle(viewLifecycleOwner, Observer {
            activity?.showSnackbar(R.string.message_book_deleted)
        })

        viewModel.setBookLinkRequestEvent.observeSingle(this) { (book, links, urls, checked) ->
            if (links.isEmpty()) {
                activity?.showSnackbar(getString(R.string.no_repos), R.string.repositories) {
                    activity?.let {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setClass(it, ReposActivity::class.java)
                        ContextCompat.startActivity(it, intent, null)
                    }
                }

            } else {
                dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.book_link)
                    .setSingleChoiceItems(
                        urls.toTypedArray(),
                        checked
                    ) { _: DialogInterface, which: Int ->
                        viewModel.setBookLink(book.id, links[which])
                        dialog?.dismiss()
                        dialog = null
                    }
                    .setNeutralButton(R.string.remove_notebook_link) { dialog, which ->
                        viewModel.setBookLink(book.id)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            if (error is BookDelete.NotFound) {
                activity?.showSnackbar(resources.getString(
                        R.string.message_deleting_book_failed, error.localizedMessage))

            } else if (error != null) {
                activity?.showSnackbar((error.cause ?: error).localizedMessage)
            }
        })

        viewModel.appBar.mode.observeSingle(viewLifecycleOwner) { mode ->
            when (mode) {
                APP_BAR_DEFAULT_MODE -> {
                    viewAdapter.clearSelection()

                    topToolbarToDefault()

                    if (withActionBar) {
                        binding.fab.run {
                            setOnClickListener {
                                SimpleOneLinerDialog
                                    .getInstance("name-new-book", R.string.new_notebook, R.string.create, null)
                                    .show(childFragmentManager, SimpleOneLinerDialog.FRAGMENT_TAG);
                            }

                            show()
                        }
                    } else {
                        binding.fab.visibility = View.GONE
                    }

                    sharedMainActivityViewModel.unlockDrawer()

                    appBarBackPressHandler.isEnabled = false
                }

                APP_BAR_SELECTION_MODE -> {
                    topToolbarToMainSelection()

                    binding.fab.run {
                        hide()
                    }

                    sharedMainActivityViewModel.lockDrawer()

                    appBarBackPressHandler.isEnabled = true
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)

        // Re-query if preference changed
        viewModel.refresh(AppPreferences.notebooksSortOrder(context))
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

    private val pickFileForBookImport =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val guessedBookName = guessBookNameFromUri(uri)
                SimpleOneLinerDialog
                    .getInstance("name-imported-book", R.string.import_as, R.string.import_, guessedBookName, bundleOf("uri" to uri))
                    .show(childFragmentManager, SimpleOneLinerDialog.FRAGMENT_TAG)
            } else {
                Log.w(TAG, "Import file not selected")
            }
        }

    /**
     * @return Guessed book name or `null` if it couldn't be guessed
     */
    private fun guessBookNameFromUri(uri: Uri): String? {
        val fileName: String = BookName.getFileName(requireContext(), uri)
        return if (BookName.isSupportedFormatFileName(fileName)) {
            val bookName = BookName.fromFileName(fileName)
            bookName.name
        } else {
            null
        }
    }

    override fun getCurrentDrawerItemId(): String {
        return drawerItemId
    }

    interface Listener {
        /**
         * Click on a book item has been performed.
         *
         * @param bookId
         */
        fun onBookClicked(bookId: Long)
    }

    companion object {
        private val TAG = BooksFragment::class.java.name

        val drawerItemId: String = BooksFragment::class.java.name

        /**
         * Name used for [android.app.FragmentManager].
         */
        val FRAGMENT_TAG: String = BooksFragment::class.java.name

        private const val ARG_WITH_ACTION_BAR = "with_action_bar"

        @JvmStatic
        @JvmOverloads
        fun getInstance(withActionBar: Boolean = true): BooksFragment {
            val fragment = BooksFragment()
            val args = Bundle()

            args.putBoolean(ARG_WITH_ACTION_BAR, withActionBar)

            fragment.arguments = args
            return fragment
        }
    }
}

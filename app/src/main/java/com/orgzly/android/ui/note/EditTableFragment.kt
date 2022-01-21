package com.orgzly.android.ui.note

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Note
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentEditTableBinding
import javax.inject.Inject

class EditTableFragment : Fragment() {

    private val TAG = EditTableFragment::class.java.name

    private lateinit var binding: FragmentEditTableBinding

    @Inject
    internal lateinit var dataRepository: DataRepository

    private var listener: NoteFragment.Listener? = null

    private lateinit var viewModel: TableViewModel

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    private var dialog: AlertDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        listener = activity as NoteFragment.Listener
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sharedMainActivityViewModel.setFragment(FRAGMENT_TAG, null, null, 0)

        viewModel.noteViewModel.noteDetailsDataEvent.observeSingle(viewLifecycleOwner, {
            viewModel.loadTableData()
        })
        // TODO Load payload from saved Bundle if available
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we ought to do something with the Bundle if it's non-null

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onCreate")

        val args = requireNotNull(arguments)

        val nvmf: NoteViewModelFactory = NoteViewModelFactory.getInstance(
                dataRepository,
                arguments?.getLong(ARG_BOOK_ID) ?: 0,
                args.getLong(ARG_NOTE_ID),
                null,
                null,
                null) as NoteViewModelFactory

        val factory = TableViewModelFactory.getInstance(
                nvmf,
                args.getInt(ARG_TABLE_START_OFFSET),
                args.getInt(ARG_TABLE_END_OFFSET))

        viewModel = ViewModelProviders.of(this, factory).get(TableViewModel::class.java)

        sharedMainActivityViewModel = ViewModelProviders.of(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentEditTableBinding.inflate(inflater, container, false)

        binding.tableViewModel = viewModel

        binding.lifecycleOwner = viewLifecycleOwner

        // from https://stackoverflow.com/a/41022589/116509, to have a DONE tickbox icon on the soft keyboard instead a newline icon
        binding.tableContentEditText.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        binding.tableContentEditText.setRawInputType(InputType.TYPE_CLASS_TEXT);

        binding.tableContentEditText.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                userSave(null)
                return@setOnEditorActionListener true
            }
            false
        }

        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.note_actions, menu)

        menu.removeItem(R.id.activity_action_search)

        menu.removeItem(R.id.note_view_edit)

        menu.removeItem(R.id.metadata)

        menu.removeItem(R.id.delete)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        when (item.itemId) {
            R.id.done -> {
                userSave(null)
                return true
            }

            R.id.keep_screen_on -> {
                dialog = ActivityUtils.keepScreenOnToggle(activity, item)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun userSave(postSave: ((note: Note) -> Unit)?) {
        ActivityUtils.closeSoftKeyboard(activity)
        viewModel.updateNote(postSave)
    }

    private fun onBackPressed() {
        userCancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        viewModel.loadNoteData()
    }

    private fun setupObservers() {

        viewModel.tableUpdatedEvent.observe(viewLifecycleOwner, Observer { note ->
            listener?.onNoteUpdated(note)
        })


        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            showSnackbar((error.cause ?: error).localizedMessage)
        })

        viewModel.snackBarMessage.observeSingle(viewLifecycleOwner, Observer { resId ->
            showSnackbar(resId)
        })
    }

    private fun showSnackbar(message: String?) {
        CommonActivity.showSnackbar(context, message)
    }

    private fun showSnackbar(@StringRes resId: Int) {
        CommonActivity.showSnackbar(context, resId)
    }


    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
    }

    private fun userCancel(): Boolean {
        ActivityUtils.closeSoftKeyboard(activity)

        if (viewModel.isNoteModified()) {
            dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.note_has_been_modified)
                    .setMessage(R.string.discard_or_save_changes)
                    .setPositiveButton(R.string.save) { _, _ ->
                        viewModel.updateNote {
                            listener?.onNoteCanceled()
                        }
                    }
                    .setNegativeButton(R.string.discard) { _, _ ->
                        listener?.onNoteCanceled()
                    }
                    .setNeutralButton(R.string.cancel, null)
                    .show()

            return true

        } else {
            listener?.onNoteCanceled()
            return false
        }
    }


    companion object {

        val FRAGMENT_TAG: String = EditTableFragment::class.java.name
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_NOTE_ID = "note_id"
        private const val ARG_TABLE_START_OFFSET = "table_start_offset"
        private const val ARG_TABLE_END_OFFSET = "table_end_offset"


        @JvmStatic
        fun newInstance(bookId: Long,
                        noteId: Long,
                        tableStartOffset: Int,
                        tableEndOffset: Int) =
                EditTableFragment().apply {
                    arguments = Bundle().apply {
                        putLong(ARG_BOOK_ID, bookId)
                        putLong(ARG_NOTE_ID, noteId)
                        putInt(ARG_TABLE_START_OFFSET, tableStartOffset)
                        putInt(ARG_TABLE_END_OFFSET, tableEndOffset)
                    }
                }
    }
}
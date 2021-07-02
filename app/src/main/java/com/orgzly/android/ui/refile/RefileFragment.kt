package com.orgzly.android.ui.refile

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.ui.Breadcrumbs
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.usecase.NoteRefile
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.DialogRefileBinding
import javax.inject.Inject

class RefileFragment : DialogFragment() {

    private lateinit var binding: DialogRefileBinding

    @Inject
    lateinit var dataRepository: DataRepository

    lateinit var viewModel: RefileViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val noteIds = arguments?.getLongArray(ARG_NOTE_IDS)?.toSet() ?: emptySet()
        val count = arguments?.getInt(ARG_COUNT) ?: 0

        val factory = RefileViewModelFactory.forNotes(dataRepository, noteIds, count)

        viewModel = ViewModelProvider(this, factory).get(RefileViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val dialog = object: Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                if (viewModel.locationHasParent()) {
                    viewModel.openParent()
                } else {
                    super.onBackPressed()
                }
            }
        }

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = DialogRefileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogRefileToolbar.apply {
            title = resources.getQuantityString(
                    R.plurals.refile_notes, viewModel.count, viewModel.count)

            setNavigationOnClickListener {
                dismiss()
            }
        }

        val adapter = RefileAdapter(binding.root.context, object: RefileAdapter.OnClickListener {
            override fun onItem(item: RefileViewModel.Item) {
                viewModel.open(item)
            }

            override fun onButton(item: RefileViewModel.Item) {
                viewModel.refile(item)
            }
        })

        binding.dialogRefileTargets.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }

        val refileHereButton = binding.dialogRefileRefileHere.apply {
            setOnClickListener {
                viewModel.refileHere()
            }
        }

        binding.dialogRefileBreadcrumbs.movementMethod = LinkMovementMethod.getInstance()

        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            val breadcrumbs = data.first
            val list = data.second

            adapter.submitList(list)

            // Hide refile-here button in notebook list
            if (breadcrumbs.size == 1) {
                refileHereButton.visibility = View.INVISIBLE
            } else {
                refileHereButton.visibility = View.VISIBLE
            }

            // Update and scroll breadcrumbs to the end
            binding.dialogRefileBreadcrumbs.text = generateBreadcrumbs(breadcrumbs)
            binding.dialogRefileBreadcrumbsScrollView.apply {
                post {
                    fullScroll(View.FOCUS_RIGHT)
                }
            }
        })

        viewModel.refiledEvent.observeSingle(viewLifecycleOwner, Observer { result ->
            dismiss()

            (result.userData as? Note)?.let { firstRefiledNote ->
                activity?.findViewById<View>(R.id.main_content)?.let { viewForSnackbar ->

                    val snackbar = Snackbar.make(
                            viewForSnackbar, firstRefiledNote.title, Snackbar.LENGTH_LONG)

                    (activity as CommonActivity).showSnackbar(snackbar
                            .setAction(R.string.go_to) {
                                viewModel.goTo(firstRefiledNote.id)
                            })
                }
            }
        })

        viewModel.errorEvent.observeSingle(viewLifecycleOwner, Observer { error ->
            binding.dialogRefileToolbar.subtitle =
                    if (error is NoteRefile.TargetInNotesSubtree) {
                        getString(R.string.cannot_refile_to_the_same_subtree)
                    } else {
                        (error.cause ?: error).localizedMessage
                    }
        })

        viewModel.openForTheFirstTime()
    }

    private fun generateBreadcrumbs(path: List<RefileViewModel.Item>): CharSequence {
        val breadcrumbs = Breadcrumbs()

        path.forEachIndexed { index, item ->
            val onClick = if (index != path.size - 1) { // Not last
                fun() {
                    viewModel.onBreadcrumbClick(item)
                }
            } else {
                null
            }

            when (val payload = item.payload) {
                is RefileViewModel.Home ->
                    breadcrumbs.add(getString(R.string.notebooks), 0, onClick = onClick)
                is Book ->
                    breadcrumbs.add(payload.title ?: payload.name, 0, onClick = onClick)
                is Note ->
                    breadcrumbs.add(payload.title, onClick = onClick)
            }
        }

        return breadcrumbs.toCharSequence()
    }

    override fun onResume() {
        super.onResume()

        dialog?.apply {

            // setCanceledOnTouchOutside(false)

            val w = resources.displayMetrics.widthPixels
            val h = resources.displayMetrics.heightPixels

            requireDialog().window?.apply {
                if (h > w) { // Portrait
                    setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (h * 0.90).toInt())
                } else {
                    setLayout((w * 0.90).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
                }
            }

//            window?.setLayout(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT)
        }

    }

    companion object {
        fun getInstance(noteIds: Set<Long>, count: Int): RefileFragment {
            return RefileFragment().also { fragment ->
                fragment.arguments = Bundle().apply {
                    putLongArray(ARG_NOTE_IDS, noteIds.toLongArray())
                    putInt(ARG_COUNT, count)
                }
            }
        }

        private const val ARG_NOTE_IDS = "note_ids"
        private const val ARG_COUNT = "count"

        private val TAG = RefileFragment::class.java.name

        val FRAGMENT_TAG: String = RefileFragment::class.java.name
    }
}
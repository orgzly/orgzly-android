package com.orgzly.android.ui.notes.book

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentBookPrefaceBinding
import javax.inject.Inject

/**
 * Book's preface and settings.
 */
class BookPrefaceFragment : Fragment() {

    private lateinit var binding: FragmentBookPrefaceBinding

    private var bookId: Long = 0

    private var book: Book? = null

    private var listener: Listener? = null

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity)

        listener = activity as Listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        sharedMainActivityViewModel = ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel::class.java)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentBookPrefaceBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            binding.fragmentBookPrefaceContent.typeface = Typeface.MONOSPACE
        }

        // Open keyboard
        if (activity != null) {
            ActivityUtils.openSoftKeyboardWithDelay(activity, binding.fragmentBookPrefaceContent)
        }

        /* Parse arguments - set content. */
        requireArguments().apply {
            require(containsKey(ARG_BOOK_ID)) { "No book id passed" }

            require(containsKey(ARG_BOOK_PREFACE)) { "No book preface passed" }

            bookId = getLong(ARG_BOOK_ID)

            binding.fragmentBookPrefaceContent.setText(getString(ARG_BOOK_PREFACE))
        }

        book = dataRepository.getBook(bookId)
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        announceChangesToActivity()
    }

    private fun announceChangesToActivity() {
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                BookUtils.getFragmentTitleForBook(book),
                BookUtils.getFragmentSubtitleForBook(context, book),
                0)
    }

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
    }

    /*
	 * Options Menu.
	 */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        menu.clear()

        inflater.inflate(R.menu.close_done_delete, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        when (item.itemId) {
            R.id.close -> {
                listener?.onBookPrefaceEditCancelRequest()
                return true
            }

            R.id.done -> {
                save(binding.fragmentBookPrefaceContent.text.toString())
                return true
            }

            R.id.delete -> {
                save("")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun save(preface: String) {
        book?.let {
            listener?.onBookPrefaceEditSaveRequest(it, preface)
        }
    }

    interface Listener {
        fun onBookPrefaceEditSaveRequest(book: Book, preface: String)
        fun onBookPrefaceEditCancelRequest()
    }

    companion object {
        private val TAG = BookPrefaceFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        val FRAGMENT_TAG: String = BookPrefaceFragment::class.java.name

        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_BOOK_PREFACE = "book_preface"

        fun getInstance(bookId: Long, bookPreface: String?): BookPrefaceFragment {
            val fragment = BookPrefaceFragment()

            /* Set arguments for a fragment. */
            val args = Bundle()
            args.putLong(ARG_BOOK_ID, bookId)
            args.putString(ARG_BOOK_PREFACE, bookPreface)
            fragment.arguments = args

            return fragment
        }
    }
}

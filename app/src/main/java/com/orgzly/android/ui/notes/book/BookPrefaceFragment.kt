package com.orgzly.android.ui.notes.book

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.CommonFragment
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.FragmentBookPrefaceBinding
import javax.inject.Inject

/**
 * Book's preface and settings.
 */
class BookPrefaceFragment : CommonFragment() {

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

        sharedMainActivityViewModel =
            ViewModelProvider(requireActivity())[SharedMainActivityViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        binding = FragmentBookPrefaceBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (AppPreferences.isFontMonospaced(context)) {
            binding.fragmentBookPrefaceContent.setTypeface(Typeface.MONOSPACE)
        }

        binding.fragmentBookPrefaceContent.setOnUserTextChangeListener { str ->
            binding.fragmentBookPrefaceContent.setSourceText(str)
        }

        /* Parse arguments - set content. */
        requireArguments().apply {
            require(containsKey(ARG_BOOK_ID)) { "No book id passed" }

            require(containsKey(ARG_BOOK_PREFACE)) { "No book preface passed" }

            bookId = getLong(ARG_BOOK_ID)

            binding.fragmentBookPrefaceContent.setSourceText(getString(ARG_BOOK_PREFACE))
        }

        book = dataRepository.getBook(bookId)

        topToolbarToDefault()
    }

    private fun topToolbarToDefault() {
        binding.topToolbar.run {
            setNavigationOnClickListener {
                listener?.onBookPrefaceEditCancelRequest()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.delete -> {
                        save("")
                    }

                    R.id.done -> {
                        val source = binding.fragmentBookPrefaceContent.getSourceText()
                        save(source?.toString().orEmpty())
                    }

                }

                true
            }

            setOnClickListener {
                binding.fragmentBookPrefaceContainer.scrollTo(0, 0)
            }

            title = BookUtils.getFragmentTitleForBook(book)
            subtitle = getString(R.string.preface_in_book)
        }
    }

    override fun onPause() {
        super.onPause()

        sharedMainActivityViewModel.unlockDrawer()
    }

    override fun onResume() {
        super.onResume()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG)

        sharedMainActivityViewModel.lockDrawer()
    }

    override fun onDetach() {
        super.onDetach()

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        listener = null
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

package com.orgzly.android.ui.notes.book

import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.EditText
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.BookUtils
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.main.SharedMainActivityViewModel
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * Book's preface and settings.
 */
class BookPrefaceFragment : DaggerFragment() {

    private var bookId: Long = 0

    private var book: Book? = null

    private lateinit var contentView: EditText

    private var listener: Listener? = null

    @Inject
    lateinit var dataRepository: DataRepository

    private lateinit var sharedMainActivityViewModel: SharedMainActivityViewModel

    override fun onAttach(context: Context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity)
        super.onAttach(context)

        listener = activity as Listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onCreate(savedInstanceState)

        sharedMainActivityViewModel = activity?.let {
            ViewModelProviders.of(it).get(SharedMainActivityViewModel::class.java)
        } ?: throw IllegalStateException("No Activity")

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        val top = inflater.inflate(R.layout.fragment_book_preface, container, false)

        contentView = top.findViewById<View>(R.id.fragment_book_preface_content) as EditText

        val activity = activity

        if (activity != null && AppPreferences.isFontMonospaced(context)) {
            contentView.typeface = Typeface.MONOSPACE
        }

        // Open keyboard
        if (activity != null) {
            ActivityUtils.openSoftKeyboard(activity, contentView)
        }

        /* Parse arguments - set content. */
        arguments?.let {
            if (!it.containsKey(ARG_BOOK_ID)) {
                throw IllegalArgumentException("No book id passed")
            }

            if (!it.containsKey(ARG_BOOK_PREFACE)) {
                throw IllegalArgumentException("No book preface passed")
            }

            bookId = it.getLong(ARG_BOOK_ID)

            contentView.setText(it.getString(ARG_BOOK_PREFACE))
        } ?: throw IllegalArgumentException("No arguments passed")

        book = dataRepository.getBook(bookId)

        return top
    }

    override fun onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onResume()

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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDetach()

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
                save(contentView.text.toString())
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

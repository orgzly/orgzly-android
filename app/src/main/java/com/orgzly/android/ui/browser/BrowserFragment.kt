package com.orgzly.android.ui.browser

import android.content.Context
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils

import java.util.ArrayList

/**
 * Generic fragment for browser.
 * File browser or notes browser (for refiling) could extend it.
 */
abstract class BrowserFragment : ListFragment() {

    private var listener: BrowserFragmentListener? = null

    private lateinit var shortcutsLayout: LinearLayout

    protected lateinit var currentItemView: TextView

    protected var itemList: MutableList<BrowserItem> = mutableListOf()

    protected var currentItem: String? = null

    protected var nextItem: String? = null

    protected var itemsHistory = ArrayList<String>()

    protected fun init(entry: String) {
        val args = Bundle()

        args.putString(ARG_ITEM, entry)

        arguments = args
    }

    override fun onAttach(context: Context?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, activity!!)

        super.onAttach(context)
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            listener = activity as BrowserFragmentListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(activity!!.toString() + " must implement " + BrowserFragmentListener::class.java)
        }

        /* Sets current item. Either uses passed argument, or default. */
        if (arguments != null) {
            if (arguments!!.containsKey(ARG_ITEM)) {
                nextItem = arguments!!.getString(ARG_ITEM)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using passed argument: " + nextItem!!)
            }
        }

        if (nextItem == null) {
            nextItem = defaultPath()
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using browser's argument: " + nextItem!!)
        }
    }

    internal abstract fun defaultPath(): String?

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.fragment_browser, container, false)

        // Shortcuts
        shortcutsLayout = view.findViewById<View>(R.id.fragment_browser_shortcuts) as LinearLayout
        setupShortcuts(shortcutsLayout)

        // Current item
        currentItemView = view.findViewById<View>(R.id.fragment_browser_title) as TextView

        // Buttons on the bottom
        setupActionButtons(view)

        return view
    }

    internal abstract fun setupShortcuts(layout: LinearLayout)

    private fun setupActionButtons(view: View) {
        view.findViewById<View>(R.id.fragment_browser_button_cancel).setOnClickListener {
            listener?.onBrowserCancel()
        }

        view.findViewById<View>(R.id.fragment_browser_button_create).setOnClickListener {
            listener?.onBrowserCreate(currentItem)
        }

        view.findViewById<View>(R.id.fragment_browser_button_use).setOnClickListener {
            listener?.onBrowserUse(currentItem)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState)

        super.onSaveInstanceState(outState)

        outState.putString(ARG_ITEM, currentItem)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        throw IllegalStateException("Browser implementations must implement onListItemClick")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_ITEM)) {
            nextItem = savedInstanceState.getString(ARG_ITEM)
        }
    }

    interface BrowserFragmentListener {
        fun onBrowserCancel()
        fun onBrowserCreate(currentItem: String?)
        fun onBrowserUse(item: String?)
    }

    companion object {
        private val TAG = BrowserFragment::class.java.name

        protected const val ARG_ITEM = "item"
    }
}

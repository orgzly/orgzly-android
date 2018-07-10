package com.orgzly.android.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.widget.SimpleCursorAdapter
import android.view.*
import android.widget.AbsListView
import android.widget.ListView
import android.widget.ViewFlipper
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.filter.FileFilterStore
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.clients.FiltersClient
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.Fab
import com.orgzly.android.ui.FragmentListener
import com.orgzly.android.ui.drawer.DrawerItem
import com.orgzly.android.ui.Loaders
import com.orgzly.android.ui.util.ListViewUtils
import com.orgzly.android.util.LogUtils
import java.io.IOException

/**
 * Displays and allows modifying saved filters.
 */
class FiltersFragment : ListFragment(), Fab, LoaderManager.LoaderCallbacks<Cursor>, DrawerItem {

    private var mListAdapter: SimpleCursorAdapter? = null

    private var mListener: FiltersFragmentListener? = null

    private var mIsViewCreated = false

    private var mViewFlipper: ViewFlipper? = null

    private var mActionMode: ActionMode? = null

    private var dialog: AlertDialog? = null

    override fun getCurrentDrawerItemId() = getDrawerItemId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        setupAdapter()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = activity as FiltersFragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement " + FiltersFragmentListener::class.java)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_filters, container, false)

        mViewFlipper = view.findViewById(R.id.fragment_filters_flipper) as ViewFlipper

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mIsViewCreated = true

        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(MyActionMode())
    }

    override fun onPause() {
        super.onPause()

        dialog?.dismiss()
    }

    override fun onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onDestroyView()

        mIsViewCreated = false

        mActionMode?.finish()
    }

    override fun onDetach() {
        super.onDetach()

        mListener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater)

        inflater.inflate(R.menu.filters_actions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, item)

        return when (item?.itemId) {
            R.id.filters_import -> {
                mListener?.let {
                    importExport(R.string.import_from, it::onFiltersImportRequest)
                }

                true
            }

            R.id.filters_export -> {
                mListener?.let {
                    importExport(R.string.export_to, it::onFiltersExportRequest)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun importExport(resId: Int, f: (Int, String) -> Any) {
        try {
            val file = FileFilterStore(context!!).file()
            f(R.string.searches, getString(resId, file))
        } catch (e: IOException) {
            CommonActivity.showSnackbar(context, e.localizedMessage)
        }
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        mListener?.onFilterEditRequest(id)
    }

    private fun setupAdapter() {
        /* Create adapter using Cursor. */
        mListAdapter = createFilterCursorAdapter(context!!, R.layout.item_filter)

        listAdapter = mListAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)
        super.onActivityCreated(savedInstanceState)

        activity?.supportLoaderManager?.initLoader(Loaders.FILTERS_FRAGMENT, null, this)
    }

    override fun onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        super.onResume()

        announceChangesToActivity()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return FiltersClient.getCursorLoader(activity!!)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader)

        if (mIsViewCreated) {
            /*
             * Swapping instead of changing Cursor here, to keep the old one open.
             * Loader should release the old Cursor - see note in
             * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
             */

            mListAdapter?.let {
                it.swapCursor(cursor)

                mViewFlipper?.displayedChild = if (it.count > 0) 0 else 1
            }
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
        if (mIsViewCreated) {
            mListAdapter?.changeCursor(null)
        }
    }

    override fun getFabAction(): Runnable {
        return Runnable {
            mListener?.onFilterNewRequest()
        }
    }

    private fun announceChangesToActivity() {
        mListener?.announceChanges(
                FRAGMENT_TAG,
                getString(R.string.searches),
                null,
                listView.checkedItemCount)
    }

    inner class MyActionMode : AbsListView.MultiChoiceModeListener {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, listView.checkedItemCount)

            mActionMode = mode

            val inflater = mode.menuInflater
            inflater.inflate(R.menu.filters_cab, menu)

            /* Needed for after orientation change. */
            mode.title = listView.checkedItemCount.toString()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, listView.checkedItemCount)

            if (mode.tag != null) {
                menu.findItem(R.id.filters_cab_move_up).isVisible = false
                menu.findItem(R.id.filters_cab_move_down).isVisible = false

            } else {
                menu.findItem(R.id.filters_cab_move_up).isVisible = true
                menu.findItem(R.id.filters_cab_move_up).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                menu.findItem(R.id.filters_cab_move_down).isVisible = true
                menu.findItem(R.id.filters_cab_move_down).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val ids = ListViewUtils.getCheckedIds(listView)

            when (item.itemId) {
                R.id.filters_cab_move_up ->
                    mListener?.onFilterMoveUpRequest(ids.iterator().next())

                R.id.filters_cab_move_down ->
                    mListener?.onFilterMoveDownRequest(ids.iterator().next())

                R.id.filters_cab_delete -> {
                    mListener?.onFilterDeleteRequest(ids)

                    /* Close action mode. */
                    mode.finish()
                }

                else -> return false /* Not handled. */
            }

            return true /* Handled. */
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            listView.clearChoices()

            announceChangesToActivity()

            mActionMode = null
        }

        override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
            mode.title = listView.checkedItemCount.toString()

            /*
             * Request onPrepareActionMode to be called if actions for
             * repositioning need to be added to removed.
             */
            if (listView.checkedItemCount > 1) {
                if (mode.tag == null) { /* Filter repositioning actions exist. */
                    mode.tag = Any()
                    mode.invalidate()
                }
            } else {
                if (mode.tag != null) { /* Filter repositioning actions do not exist. */
                    mode.tag = null
                    mode.invalidate()
                }
            }

            announceChangesToActivity()
        }
    }

    interface FiltersFragmentListener : FragmentListener {
        fun onFilterNewRequest()
        fun onFilterDeleteRequest(ids: Set<Long>)
        fun onFilterEditRequest(id: Long)
        fun onFilterMoveUpRequest(id: Long)
        fun onFilterMoveDownRequest(id: Long)
        fun onFiltersExportRequest(title: Int, message: String)
        fun onFiltersImportRequest(title: Int, message: String)
    }

    companion object {
        private val TAG = FiltersFragment::class.java.name

        /** Name used for [android.app.FragmentManager].  */
        @JvmStatic
        val FRAGMENT_TAG: String = FiltersFragment::class.java.name

        @JvmStatic
        val instance: FiltersFragment
            get() = FiltersFragment()

        @JvmStatic
        fun createFilterCursorAdapter(context: Context, layout: Int): SimpleCursorAdapter {
            /* Column field names to be bound. */
            val columns = arrayOf(ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY)

            /* Views which the data will be bound to. */
            val to = intArrayOf(R.id.item_filter_name, R.id.item_filter_query)

            /* Create adapter using Cursor. */
            return SimpleCursorAdapter(context, layout, null, columns, to, 0)
        }

        @JvmStatic
        fun getDrawerItemId(): String {
            return TAG
        }
    }
}

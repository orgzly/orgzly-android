package com.orgzly.android.prefs

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.dndrv.NotePopupPreferenceRecyclerListAdapter
import com.orgzly.android.ui.dndrv.SimpleItemTouchHelperCallback
import com.orgzly.android.util.LogUtils

class NotePopupPreferenceFragment : PreferenceDialogFragmentCompat() {
    private lateinit var adapter: NotePopupPreferenceRecyclerListAdapter

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, view)

        val list = NotePopupPreference.getAll(requireContext(), preference.key)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        var itemTouchHelper: ItemTouchHelper? = null

        adapter = NotePopupPreferenceRecyclerListAdapter(list) { viewHolder ->
            itemTouchHelper?.startDrag(viewHolder)
        }

        // adapter.setHasStableIds(true)

        // TODO: Calculate from widths
        val layoutManager = GridLayoutManager(requireContext(), 5)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        val callback = SimpleItemTouchHelperCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback).apply {
            attachToRecyclerView(recyclerView)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, positiveResult)

        if (positiveResult) {
            NotePopupPreference.setFromAll(
                requireContext(), preference.key, adapter.items)
        }
    }

    companion object {
        val FRAGMENT_TAG: String = NotePopupPreferenceFragment::class.java.name

        fun getInstance(preference: Preference): PreferenceDialogFragmentCompat {
            return NotePopupPreferenceFragment().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, preference.key)
                }
            }
        }

        private val TAG = NotePopupPreferenceFragment::class.java.name
    }
}

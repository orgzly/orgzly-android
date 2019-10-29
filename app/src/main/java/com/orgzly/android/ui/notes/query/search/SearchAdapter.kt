package com.orgzly.android.ui.notes.query.search

import android.content.Context
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.android.ui.notes.NoteItemViewBinder
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.util.LogUtils
import com.orgzly.databinding.ItemHeadBinding

class SearchAdapter(
        private val context: Context,
        private val clickListener: OnViewHolderClickListener<NoteView>,
        private val quickBar: QuickBars
) :ListAdapter<NoteView, RecyclerView.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

    private val adapterSelection: Selection = Selection()

    private val noteItemViewBinder: NoteItemViewBinder = NoteItemViewBinder(context, inBook = false)

    private val viewHolderListener = object: NoteItemViewHolder.ClickListener {
        override fun onClick(view: View, position: Int) {
            clickListener.onClick(view, position, getItem(position))
        }
        override fun onLongClick(view: View, position: Int) {
            clickListener.onLongClick(view, position, getItem(position))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val binding = ItemHeadBinding.inflate(LayoutInflater.from(context), parent, false)

        NoteItemViewBinder.setupSpacingForDensitySetting(context, binding)

        return NoteItemViewHolder(binding, viewHolderListener)
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        val holder = h as NoteItemViewHolder

        val noteView = getItem(position)

        val note = noteView.note

        noteItemViewBinder.bind(holder, noteView)

        quickBar.bind(holder)

        getSelection().setIsSelectedBackground(holder.itemView, note.id)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).note.id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    companion object {
        private val TAG = SearchAdapter::class.java.name

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<NoteView> =
                object : DiffUtil.ItemCallback<NoteView>() {
                    override fun areItemsTheSame(oldItem: NoteView, newItem: NoteView): Boolean {
                        return oldItem.note.id == newItem.note.id
                    }

                    override fun areContentsTheSame(oldItem: NoteView, newItem: NoteView): Boolean {
                        return oldItem == newItem
                    }
                }
    }
}

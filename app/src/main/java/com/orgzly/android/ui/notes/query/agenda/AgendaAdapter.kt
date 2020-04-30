package com.orgzly.android.ui.notes.query.agenda

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.android.ui.notes.NoteItemViewBinder
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.notes.quickbar.QuickBars
import com.orgzly.android.ui.stickyheaders.StickyHeaders
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.databinding.ItemAgendaDividerBinding
import com.orgzly.databinding.ItemHeadBinding

class AgendaAdapter(
        private val context: Context,
        private val clickListener: OnViewHolderClickListener<AgendaItem>,
        private val quickBar: QuickBars
) : ListAdapter<AgendaItem, RecyclerView.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter, StickyHeaders {

    private val adapterSelection: Selection = Selection()

    private val userTimeFormatter = UserTimeFormatter(context)

    private val noteViewBinder = NoteItemViewBinder(context, inBook = false)

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

        return when (viewType) {
            OVERDUE_ITEM_TYPE, DAY_ITEM_TYPE -> {
                val binding = ItemAgendaDividerBinding.inflate(
                        LayoutInflater.from(context), parent, false)

                NoteItemViewBinder.setupSpacingForDensitySetting(context, binding)

                DividerViewHolder(binding)
            }

            else -> {
                val binding = ItemHeadBinding.inflate(LayoutInflater.from(context), parent, false)

                NoteItemViewBinder.setupSpacingForDensitySetting(context, binding)

                NoteItemViewHolder(binding, viewHolderListener)
            }
        }
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        when (h.itemViewType) {
            OVERDUE_ITEM_TYPE -> {
                val holder = h as DividerViewHolder
                val item = getItem(position) as AgendaItem.Overdue

                bindDividerView(holder, item)
            }

            DAY_ITEM_TYPE -> {
                val holder = h as DividerViewHolder
                val item = getItem(position) as AgendaItem.Day

                bindDividerView(holder, item)
            }

            else -> {
                val holder = h as NoteItemViewHolder
                val item = getItem(position) as AgendaItem.Note

                noteViewBinder.bind(holder, item.note, item.timeType)

                quickBar.bind(holder)

                getSelection().setIsSelectedBackground(holder.itemView, item.id)
            }
        }
    }

    private fun bindDividerView(holder: DividerViewHolder, item: AgendaItem.Overdue) {
        holder.binding.itemAgendaDividerText.text = context.getString(R.string.overdue)
    }

    private fun bindDividerView(holder: DividerViewHolder, item: AgendaItem.Day) {
        holder.binding.itemAgendaDividerText.text = userTimeFormatter.formatDate(item.day)
    }

    inner class DividerViewHolder(val binding: ItemAgendaDividerBinding) :
            RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AgendaItem.Overdue -> OVERDUE_ITEM_TYPE
            is AgendaItem.Day -> DAY_ITEM_TYPE
            else -> NOTE_ITEM_TYPE
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    override fun isStickyHeader(position: Int): Boolean {
        return if (position < itemCount) {
            return when (getItemViewType(position)) {
                OVERDUE_ITEM_TYPE, DAY_ITEM_TYPE -> true
                else -> false
            }
        } else {
            false
        }
    }

    companion object {
        private val TAG = AgendaAdapter::class.java.name

        const val OVERDUE_ITEM_TYPE = 0
        const val DAY_ITEM_TYPE = 1
        const val NOTE_ITEM_TYPE = 2

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<AgendaItem> =
                object : DiffUtil.ItemCallback<AgendaItem>() {
                    override fun areItemsTheSame(oldItem: AgendaItem, newItem: AgendaItem): Boolean {
                        return oldItem == newItem
                    }

                    override fun areContentsTheSame(oldItem: AgendaItem, newItem: AgendaItem): Boolean {
                        return if (oldItem is AgendaItem.Note && newItem is AgendaItem.Note) {
                            // Specifying type to remove the error when comparing below:
                            // Suspicious equality check: equals() is not implemented in AgendaItem
                            val old: AgendaItem.Note = oldItem
                            val new: AgendaItem.Note = newItem

                            old == new

                        } else if (oldItem is AgendaItem.Day && newItem is AgendaItem.Day) {
                            oldItem.day == newItem.day

                        } else {
                            false
                        }
                    }
                }
    }
}
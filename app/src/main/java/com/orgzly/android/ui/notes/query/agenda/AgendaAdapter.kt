package com.orgzly.android.ui.notes.query.agenda

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UserTimeFormatter

class AgendaAdapter(
        private val context: Context,
        private val clickListener: OnViewHolderClickListener<AgendaItem>,
        private val quickBar: QuickBars
 ) : ListAdapter<AgendaItem, RecyclerView.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

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
            DIVIDER_ITEM_TYPE -> {
                val layout = LayoutInflater.from(context)
                        .inflate(R.layout.item_agenda_divider, parent, false)
                DividerViewHolder(layout)
            }

            else -> {
                val layout = LayoutInflater.from(context)
                        .inflate(R.layout.item_head, parent, false)

                NoteItemViewHolder(layout, viewHolderListener)
            }
        }
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        if (h.itemViewType == DIVIDER_ITEM_TYPE) {
            val holder = h as DividerViewHolder
            val item = getItem(position) as AgendaItem.Divider

            bindDividerView(holder, item)

        } else {
            val holder = h as NoteItemViewHolder
            val item = getItem(position) as AgendaItem.Note

            noteViewBinder.bind(holder, item.note)

            quickBar.bind(holder)

            getSelection().setIsSelectedBackground(holder.itemView, item.id)
        }
    }

    private fun bindDividerView(holder: DividerViewHolder, item: AgendaItem.Divider) {
        val margins = NoteItemViewBinder.getMarginsForListDensity(context)

        holder.time.text = userTimeFormatter.formatDate(item.day)

        holder.view.setPadding(
                holder.view.paddingLeft, margins.first, holder.view.paddingRight, margins.first)
    }

    inner class DividerViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.item_agenda_time_text)
    }

//    inner class NoteViewHolder(view: View) : NotesAdapter.NoteViewHolder() {
//        val title: TextView = view.findViewById(R.id.item_head_title)
//        val scheduledTimeText: TextView = view.findViewById(R.id.item_head_scheduled_text)
//    }


    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)

        return if (item is AgendaItem.Note) {
            NOTE_ITEM_TYPE
        } else {
            DIVIDER_ITEM_TYPE
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    companion object {
        private val TAG = AgendaAdapter::class.java.name

        const val DIVIDER_ITEM_TYPE = 0
        const val NOTE_ITEM_TYPE = 1

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<AgendaItem> =
                object : DiffUtil.ItemCallback<AgendaItem>() {
                    override fun areItemsTheSame(oldItem: AgendaItem, newItem: AgendaItem): Boolean {
                        return oldItem == newItem
                    }

                    override fun areContentsTheSame(oldItem: AgendaItem, newItem: AgendaItem): Boolean {
                        if (oldItem is AgendaItem.Note && newItem is AgendaItem.Note) {
                            return oldItem == newItem // TODO: Compare content

                        } else if (oldItem is AgendaItem.Divider && newItem is AgendaItem.Divider) {
                            return oldItem.day == newItem.day

                        } else {
                            return false
                        }
                    }
                }
    }
}
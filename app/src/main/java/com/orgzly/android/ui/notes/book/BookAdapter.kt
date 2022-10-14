package com.orgzly.android.ui.notes.book

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.android.ui.notes.NoteItemViewBinder
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.databinding.ItemHeadBinding
import com.orgzly.databinding.ItemPrefaceBinding

class BookAdapter(
    private val bookId: Long,
    private val context: Context,
    private val clickListener: OnClickListener,
    private val inBook: Boolean
) :
    ListAdapterWithHeaders<NoteView, RecyclerView.ViewHolder>(DIFF_CALLBACK, 1),
    SelectableItemAdapter {

    private var currentPreface: String? = null

    private val adapterSelection = Selection()

    private val noteItemViewBinder = NoteItemViewBinder(context, inBook)
    private val prefaceItemViewBinder = PrefaceItemViewBinder(context)

    private val noteViewHolderListener = object: NoteItemViewHolder.ClickListener {
        override fun onClick(view: View, position: Int) {
            clickListener.onNoteClick(view, position, getItem(position))
        }
        override fun onLongClick(view: View, position: Int) {
            clickListener.onNoteLongClick(view, position, getItem(position))
        }
    }

    inner class FoldedViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class PrefaceViewHolder(val binding: ItemPrefaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                clickListener.onPrefaceClick()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> R.layout.item_preface
            isVisible(getItem(position).note) -> VISIBLE_ITEM_TYPE
            else -> HIDDEN_ITEM_TYPE
        }
    }

    private fun isVisible(note: Note): Boolean {
        return !inBook || note.position.foldedUnderId == 0L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_preface -> {
                val binding = ItemPrefaceBinding.inflate(
                    LayoutInflater.from(context), parent, false)

                PrefaceViewHolder(binding)
            }

            HIDDEN_ITEM_TYPE -> {
                FoldedViewHolder(View(context))
            }

            else -> {
                val binding = ItemHeadBinding.inflate(
                    LayoutInflater.from(context), parent, false)

                NoteItemViewBinder.setupSpacingForDensitySetting(context, binding)

                NoteItemViewHolder(binding, noteViewHolderListener)
            }
        }
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, position: Int) {
        when {
            position == 0 -> {
                val holder = h as PrefaceViewHolder

                prefaceItemViewBinder.bind(holder, bookId, currentPreface, isPrefaceDisplayed())
            }

            h.itemViewType == HIDDEN_ITEM_TYPE -> {
                h.itemView.visibility = View.GONE
                return
            }

            else -> {
                val holder = h as NoteItemViewHolder
                val noteView = getItem(position)
                val note = noteView.note

                noteItemViewBinder.bind(holder, noteView)

                getSelection().setBackgroundIfSelected(holder.itemView, note.id)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position > 0) {
            getItem(position).note.id
        } else {
            -1
        }
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    fun clearSelection() {
        if (getSelection().count > 0) {
            getSelection().clear()
            notifyDataSetChanged() // FIXME
        }
    }

    fun setPreface(book: Book?) {
        currentPreface = book?.preface
        notifyItemChanged(0)
    }

    fun isPrefaceDisplayed(): Boolean {
        val hidden =
            context.getString(R.string.pref_value_preface_in_book_hide) ==
                    AppPreferences.prefaceDisplay(context)

        return !TextUtils.isEmpty(currentPreface) && !hidden
    }

    interface OnClickListener {
        fun onPrefaceClick()

        fun onNoteClick(view: View, position: Int, noteView: NoteView)

        fun onNoteLongClick(view: View, position: Int, noteView: NoteView)
    }

    companion object {
        private val TAG = BookAdapter::class.java.name

        const val HIDDEN_ITEM_TYPE = 0 // Not used
        const val VISIBLE_ITEM_TYPE = 1

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

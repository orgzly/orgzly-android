package com.orgzly.android.ui.notes.book

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.android.ui.notes.NoteItemViewBinder
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.util.LogUtils

class BookAdapter(
        private val context: Context,
        private val clickListener: OnClickListener,
        private val inBook: Boolean
) :
        ListAdapterWithHeaders<NoteView, androidx.recyclerview.widget.RecyclerView.ViewHolder>(DIFF_CALLBACK, 1),
        SelectableItemAdapter {

    private var currentPreface: String? = null

    private val adapterSelection: Selection = Selection()

    private val noteItemViewBinder: NoteItemViewBinder = NoteItemViewBinder(context, inBook)

    private val noteViewHolderListener = object: NoteItemViewHolder.ClickListener {
        override fun onClick(view: View, position: Int) {
            clickListener.onNoteClick(view, position, getItem(position))
        }
        override fun onLongClick(view: View, position: Int) {
            clickListener.onNoteLongClick(view, position, getItem(position))
        }
    }

    inner class FoldedViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

    inner class PrefaceViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        val preface: TextViewWithMarkup = view.findViewById(R.id.fragment_book_header_text)

        init {
            view.setOnClickListener {
                clickListener.onPrefaceClick()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> R.layout.item_head_book_preface
            isVisible(getItem(position).note) -> VISIBLE_ITEM_TYPE
            else -> HIDDEN_ITEM_TYPE
        }
    }

    private fun isVisible(note: Note): Boolean {
        return !inBook || note.position.foldedUnderId == 0L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        return when (viewType) {
            R.layout.item_head_book_preface -> {
                val layout = LayoutInflater.from(context)
                        .inflate(R.layout.item_head_book_preface, parent, false)

                PrefaceViewHolder(layout)
            }

            HIDDEN_ITEM_TYPE -> {
                FoldedViewHolder(View(context))
            }

            else -> {
                val layout = LayoutInflater.from(context)
                        .inflate(R.layout.item_head, parent, false)

                NoteItemViewHolder(layout, noteViewHolderListener)
            }
        }
    }

    override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when {
            position == 0 -> {
                val holder = h as PrefaceViewHolder

                if (!isPrefaceDisplayed()) {
                    holder.preface.visibility = View.GONE


                } else {
                    if (context.getString(R.string.pref_value_preface_in_book_few_lines) ==
                            AppPreferences.prefaceDisplay(context)) {

                        holder.preface.maxLines = 3
                        holder.preface.ellipsize = TextUtils. TruncateAt.END

                    } else {
                        holder.preface.maxLines = Integer.MAX_VALUE
                        holder.preface.ellipsize = null
                    }

                    currentPreface?.let {
                        holder.preface.setRawText(it)
                    }

                    holder.preface.visibility = View.VISIBLE
                }
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

                getSelection().setIsSelectedBackground(holder.itemView, note.id)
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

        const val HIDDEN_ITEM_TYPE = 0
        const val VISIBLE_ITEM_TYPE = 1

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<NoteView> =
                object : DiffUtil.ItemCallback<NoteView>() {
                    override fun areItemsTheSame(oldItem: NoteView, newItem: NoteView): Boolean {
                        return oldItem.note.id == newItem.note.id
                    }

                    override fun areContentsTheSame(oldItem: NoteView, newItem: NoteView): Boolean {
                        return oldItem == newItem // TODO: Compare content
                    }
                }
    }
}

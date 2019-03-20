package com.orgzly.android.ui.notes

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import com.orgzly.R
import com.orgzly.android.ui.views.TextViewWithMarkup

class NoteItemViewHolder(view: View, private val clickListener: ClickListener) :
        RecyclerView.ViewHolder(view),
        View.OnClickListener,
        View.OnLongClickListener {

    val container: View = view.findViewById(R.id.item_head_container)

    val bookNameLeftFromNoteText: TextView = itemView.findViewById(R.id.item_head_book_name_before_note_text)
    val bookNameUnderNote: View = itemView.findViewById(R.id.item_head_book_name)
    val bookNameUnderNoteText: TextView = itemView.findViewById(R.id.item_head_book_name_text)

    val indentContainer: ViewGroup = itemView.findViewById(R.id.item_head_indent_container)

    val bulletContainer: ViewGroup = itemView.findViewById(R.id.item_head_bullet_container)
    val bullet: ImageView = itemView.findViewById(R.id.item_head_bullet)

    val payload: View = itemView.findViewById(R.id.item_head_payload)

    val title: TextView = view.findViewById(R.id.item_head_title)

    val scheduled: View = itemView.findViewById(R.id.item_head_scheduled)
    val scheduledText: TextView = itemView.findViewById(R.id.item_head_scheduled_text)

    val deadline: View = itemView.findViewById(R.id.item_head_deadline)
    val deadlineText: TextView = itemView.findViewById(R.id.item_head_deadline_text)

    val event: View = itemView.findViewById(R.id.item_head_event)
    val eventText: TextView = itemView.findViewById(R.id.item_head_event_text)

    val closed: View = itemView.findViewById(R.id.item_head_closed)
    val closedText: TextView = itemView.findViewById(R.id.item_head_closed_text)

    val content: TextViewWithMarkup = view.findViewById(R.id.item_head_content)

    val foldButton: View = itemView.findViewById(R.id.item_head_fold_button)
    val foldButtonText: TextView = itemView.findViewById(R.id.item_head_fold_button_text)

    val actionBar: ViewFlipper = itemView.findViewById(R.id.quick_bar_flipper)
    val actionBarLeft: ViewGroup = itemView.findViewById(R.id.quick_bar_left)
    val actionBarRight: ViewGroup = itemView.findViewById(R.id.quick_bar_right)

    init {
        view.setOnClickListener(this)
        view.setOnLongClickListener(this)
    }

    override fun onClick(view: View) {
        adapterPosition.let { position ->
            if (position != RecyclerView.NO_POSITION) {
                clickListener.onClick(view, position)
            } else {
                Log.e(TAG, "Adapter position for $view not available")
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        adapterPosition.let { position ->
            return if (position != RecyclerView.NO_POSITION) {
                clickListener.onLongClick(view, position)
                true
            } else {
                Log.e(TAG, "Adapter position for $view not available")
                false
            }
        }
    }

    interface ClickListener {
        fun onClick(view: View, position: Int)
        fun onLongClick(view: View, position: Int)
    }

    companion object {
        private val TAG = NoteItemViewHolder::class.java.name
    }
}
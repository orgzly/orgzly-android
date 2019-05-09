package com.orgzly.android.ui.refile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.notes.NoteItemViewBinder

class RefileAdapter(val context: Context, val listener: OnClickListener) :
        ListAdapter<RefileViewModel.Item, RefileAdapter.RefileViewHolder>(DIFF_CALLBACK) {

    data class Icons(
            @DrawableRes val up: Int,
            @DrawableRes val book: Int,
            @DrawableRes val noteWithChildren: Int,
            @DrawableRes val noteWithoutChildren: Int)

    var icons: Icons? = null

    private val noteItemViewBinder = NoteItemViewBinder(context, true)

    interface OnClickListener {
        fun onItem(item: RefileViewModel.Item)
        fun onButton(item: RefileViewModel.Item)
    }

    class RefileViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val payload: ViewGroup = view.findViewById(R.id.item_refile_payload)
        val icon: ImageView = view.findViewById(R.id.item_refile_icon)
        val name: TextView = view.findViewById(R.id.item_refile_name)
        val button: ImageView = view.findViewById(R.id.item_refile_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RefileViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_refile, parent, false)

        val holder = RefileViewHolder(layout)

        holder.payload.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItem(getItem(holder.adapterPosition))
            }
        }

        holder.button.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onButton(getItem(holder.adapterPosition))
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: RefileViewHolder, position: Int) {

        if (icons == null) {
            icons = getIcons(holder.view.context)
        }

        val item = getItem(position)

        when (val payload = item.payload) {
            is Book -> {
                holder.name.text = payload.title ?: payload.name

                holder.button.visibility = View.VISIBLE

                holder.icon.visibility = View.GONE
//                icons?.let {
//                    holder.icon.setImageResource(it.book)
//                }
            }

            is Note -> {
                holder.name.text = noteItemViewBinder.generateTitle(
                        NoteView(note = payload, bookName = ""))

                holder.button.visibility = View.VISIBLE

                icons?.let {
                    if (payload.position.descendantsCount > 0) {
                        holder.icon.setImageResource(it.noteWithChildren)
                    } else {
                        holder.icon.setImageResource(it.noteWithoutChildren)
                    }
                    holder.icon.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getIcons(context: Context): Icons {
        val typedArray = context.obtainStyledAttributes(R.styleable.Icons)

        val result = Icons(
                typedArray.getResourceId(R.styleable.Icons_ic_keyboard_arrow_up_24dp, 0),
                typedArray.getResourceId(R.styleable.Icons_ic_library_books_24dp, 0),
                typedArray.getResourceId(R.styleable.Icons_bullet_folded, 0),
                typedArray.getResourceId(R.styleable.Icons_bullet_default, 0))

        typedArray.recycle()

        return result
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<RefileViewModel.Item> =
                object : DiffUtil.ItemCallback<RefileViewModel.Item>() {
                    override fun areItemsTheSame(oldItem: RefileViewModel.Item, newItem: RefileViewModel.Item): Boolean {
                        return oldItem == newItem
                    }

                    override fun areContentsTheSame(oldItem: RefileViewModel.Item, newItem: RefileViewModel.Item): Boolean {
                        return oldItem == newItem
                    }
                }
    }
}
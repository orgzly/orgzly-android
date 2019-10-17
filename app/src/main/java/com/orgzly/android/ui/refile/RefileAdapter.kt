package com.orgzly.android.ui.refile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.R
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.notes.NoteItemViewBinder
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.databinding.ItemRefileBinding

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

    class RefileViewHolder(val binding: ItemRefileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RefileViewHolder {
        val holder = RefileViewHolder(ItemRefileBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))

        holder.binding.itemRefilePayload.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItem(getItem(holder.adapterPosition))
            }
        }

        holder.binding.itemRefileButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onButton(getItem(holder.adapterPosition))
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: RefileViewHolder, position: Int) {

        if (icons == null) {
            icons = getIcons(holder.binding.root.context)
        }

        val item = getItem(position)

        when (val payload = item.payload) {
            is Book -> {
                holder.binding.itemRefileName.text = payload.title ?: payload.name

                holder.binding.itemRefileButton.visibility = View.VISIBLE

                holder.binding.itemRefileIcon.visibility = View.GONE
//                icons?.let {
//                    holder.icon.setImageResource(it.book)
//                }
            }

            is Note -> {
                holder.binding.itemRefileName.text = noteItemViewBinder.generateTitle(
                        NoteView(note = payload, bookName = ""))

                holder.binding.itemRefileButton.visibility = View.VISIBLE

                icons?.let {
                    if (payload.position.descendantsCount > 0) {
                        holder.binding.itemRefileIcon.setImageResource(it.noteWithChildren)
                    } else {
                        holder.binding.itemRefileIcon.setImageResource(it.noteWithoutChildren)
                    }
                    holder.binding.itemRefileIcon.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getIcons(context: Context): Icons {
        return context.styledAttributes(R.styleable.Icons) { typedArray ->
            Icons(
                    typedArray.getResourceId(R.styleable.Icons_ic_keyboard_arrow_up_24dp, 0),
                    typedArray.getResourceId(R.styleable.Icons_ic_library_books_24dp, 0),
                    typedArray.getResourceId(R.styleable.Icons_bullet_folded, 0),
                    typedArray.getResourceId(R.styleable.Icons_bullet_default, 0))
        }
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
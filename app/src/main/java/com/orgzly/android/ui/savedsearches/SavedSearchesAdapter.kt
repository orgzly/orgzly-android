package com.orgzly.android.ui.savedsearches

import android.util.Log
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection
import com.orgzly.databinding.ItemSavedSearchBinding

class SavedSearchesAdapter(
        private val clickListener: OnViewHolderClickListener<SavedSearch>
) : ListAdapter<SavedSearch, SavedSearchesAdapter.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

    private val adapterSelection: Selection = Selection()

    inner class ViewHolder(val binding: ItemSavedSearchBinding) :
            RecyclerView.ViewHolder(binding.root),
            View.OnClickListener,
            View.OnLongClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            adapterPosition.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onClick(view, position, getItem(position))
                } else {
                    Log.e(TAG, "Adapter position for $view not available")
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            adapterPosition.let { position ->
                return if (position != RecyclerView.NO_POSITION) {
                    clickListener.onLongClick(view, position, getItem(position))
                    true
                } else {
                    Log.e(TAG, "Adapter position for $view not available")
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSavedSearchBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val savedSearch = getItem(position)

        with(holder.binding) {
            name.text = savedSearch.name
            query.text = savedSearch.query

            getSelection().setIsSelectedBackground(container, savedSearch.id)
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    companion object {
        private val TAG = SavedSearchesAdapter::class.java.name

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<SavedSearch> =
                object : DiffUtil.ItemCallback<SavedSearch>() {
                    override fun areItemsTheSame(oldItem: SavedSearch, newItem: SavedSearch): Boolean {
                        return oldItem.id == newItem.id
                    }

                    override fun areContentsTheSame(oldItem: SavedSearch, newItem: SavedSearch): Boolean {
                        return oldItem.areContentsTheSame(newItem)
                    }
                }
    }
}
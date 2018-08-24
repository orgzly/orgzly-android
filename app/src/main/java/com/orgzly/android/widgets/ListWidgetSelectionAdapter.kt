package com.orgzly.android.widgets

import android.util.Log
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.orgzly.R
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.ui.OnViewHolderClickListener
import com.orgzly.android.ui.SelectableItemAdapter
import com.orgzly.android.ui.Selection

class ListWidgetSelectionAdapter(
        private val clickListener: OnViewHolderClickListener<SavedSearch>
) : ListAdapter<SavedSearch, ListWidgetSelectionAdapter.ViewHolder>(DIFF_CALLBACK), SelectableItemAdapter {

    private val adapterSelection: Selection = Selection()

    inner class ViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view),
            View.OnClickListener,
            View.OnLongClickListener {

        val container: View = view.findViewById(R.id.item_list_widget_selection_container)

        val name: TextView = view.findViewById(R.id.item_list_widget_selection_name)
        val query: TextView = view.findViewById(R.id.item_list_widget_selection_description)

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
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
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_widget_selection, parent, false)

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val savedSearch = getItem(position)

        with(holder) {
            name.text = savedSearch.name
            query.text = savedSearch.query
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getSelection(): Selection {
        return adapterSelection
    }

    companion object {
        private val TAG = ListWidgetSelectionAdapter::class.java.name

        private val DIFF_CALLBACK: DiffUtil.ItemCallback<SavedSearch> =
                object : DiffUtil.ItemCallback<SavedSearch>() {
                    override fun areItemsTheSame(oldItem: SavedSearch, newItem: SavedSearch): Boolean {
                        return oldItem.id == newItem.id
                    }

                    override fun areContentsTheSame(oldItem: SavedSearch, newItem: SavedSearch): Boolean {
                        return oldItem == newItem // TODO: Compare content
                    }
                }
    }
}
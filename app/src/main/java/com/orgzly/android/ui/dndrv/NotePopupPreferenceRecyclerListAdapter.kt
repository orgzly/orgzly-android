package com.orgzly.android.ui.dndrv

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.NotePopupPreference
import com.orgzly.android.ui.notes.NotePopup
import com.orgzly.android.util.LogUtils
import java.util.*

//private val DIFF_CALLBACK: DiffUtil.ItemCallback<NotePopup.Action> =
//    object : DiffUtil.ItemCallback<NotePopup.Action>() {
//        override fun areItemsTheSame(oldItem: NotePopup.Action, newItem: NotePopup.Action): Boolean {
//            return oldItem == newItem
//        }
//
//        override fun areContentsTheSame(oldItem: NotePopup.Action, newItem: NotePopup.Action): Boolean {
//            return oldItem.id == newItem.id
//        }
//    }

class NotePopupPreferenceRecyclerListAdapter(
    private val initialList: List<NotePopup.Action>,
    private val dragStartListener: OnStartDragListener
) :
    RecyclerView.Adapter<NotePopupPreferenceRecyclerListAdapter.ItemViewHolder>(),
    ItemTouchHelperAdapter {

    val items: MutableList<NotePopup.Action> = ArrayList()

    init {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Adding items from initial list", initialList)
        items.addAll(initialList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = if (viewType == DIVIDER_VIEW_TYPE) { // Divider
            LayoutInflater.from(parent.context)
                .inflate(R.layout.note_popup_pref_dialog_divider_item, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.note_popup_pref_dialog_item, parent, false)

        }
        return ItemViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val action = items[position]

        holder.button.setIconResource(action.icon)

        // Start dragging to reposition the item
        holder.button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                dragStartListener.onStartDrag(holder)
            }
            false
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, action)
    }

    override fun onItemDismiss(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, fromPosition, toPosition)

        val item  = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, item)

        notifyItemMoved(fromPosition, toPosition)

        return true
    }

    override fun getItemCount(): Int {
        return items.size
    }

//    override fun getItemId(position: Int): Long {
//        return items[position].id.toLong()
//    }

    override fun getItemViewType(position: Int): Int {
        val action = items[position]

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, action)

        return if (action.id == R.id.note_popup_divider) {
            DIVIDER_VIEW_TYPE
        } else {
            ACTION_VIEW_TYPE
        }
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemTouchHelperViewHolder {
        val button: MaterialButton

        init {
            button = itemView.findViewById(R.id.button)
        }

        override fun onItemSelected() {
            button.alpha = 0.85f
        }

        override fun onItemClear() {
            button.alpha = 1f
        }
    }

    companion object {
        private const val DIVIDER_VIEW_TYPE = 0
        private const val ACTION_VIEW_TYPE = 1

        private val TAG = NotePopupPreferenceRecyclerListAdapter::class.java.name
    }
}
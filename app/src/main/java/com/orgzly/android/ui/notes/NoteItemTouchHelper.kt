package com.orgzly.android.ui.notes

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class NoteItemTouchHelper(val listener: Listener) : ItemTouchHelper(Callback(listener)) {

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // super.onDraw(c, parent, state)
    }

    interface Listener {
        fun onSwipeLeft(id: Long)
    }

    class Callback(val listener: Listener) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (direction == ItemTouchHelper.START) {
                listener.onSwipeLeft(viewHolder.itemId)
            }
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            }

            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        }
    }
}
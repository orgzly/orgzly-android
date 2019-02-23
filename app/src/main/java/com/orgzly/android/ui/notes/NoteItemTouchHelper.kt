package com.orgzly.android.ui.notes

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class NoteItemTouchHelper(val listener: Listener) : ItemTouchHelper(Callback(listener)) {

    interface Listener {
        fun onSwipeLeft(id: Long)
    }

    class Callback(val listener: Listener) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {

        private var leftSwipeAction: SwipeAction? = null

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean) {

            val itemView = viewHolder.itemView

            val noteItemViewHolder = viewHolder as? NoteItemViewHolder ?: return

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, itemView.left, itemView.right, dX, dY, actionState, isCurrentlyActive)

            when {
                dX < 0 -> { // Swipe left
                    // Hide indent
                    noteItemViewHolder.indentContainer.visibility = View.INVISIBLE

                    if (leftSwipeAction == null) {
                        leftSwipeAction = SwipeAction.OpenNote(recyclerView.context)
                    }

                    leftSwipeAction?.drawForLeftSwipe(canvas, itemView, dX)
                }

                dX > 0 -> { // Swipe right
                }

                dX == 0f -> { // Original position
                    noteItemViewHolder.indentContainer.visibility = View.VISIBLE

                    // Reset so it can be re-initialized if settings changed
                    leftSwipeAction = null
                }
            }

            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder): Boolean {

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

    companion object {
        private val TAG = NoteItemTouchHelper::class.java.name
    }

}
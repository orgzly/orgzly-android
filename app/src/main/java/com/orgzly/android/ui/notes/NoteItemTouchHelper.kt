package com.orgzly.android.ui.notes

import android.content.Context
import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class NoteItemTouchHelper(inBook: Boolean, listener: Listener) :
        ItemTouchHelper(Callback(inBook, listener)) {

    interface Listener {
        fun onSwiped(viewHolder: NoteItemViewHolder, direction: Int)
    }

    class Callback(private val inBook: Boolean, private val listener: Listener) :
            ItemTouchHelper.SimpleCallback(0, START or END) {

        private var leftSwipeAction: SwipeAction? = null
        private var rightSwipeAction: SwipeAction? = null

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
                    noteItemViewHolder.binding.itemHeadIndentContainer.visibility = View.INVISIBLE

                    if (leftSwipeAction == null) {
                        leftSwipeAction = getLeftSwipeAction(inBook, recyclerView.context)
                    }

                    leftSwipeAction?.drawForLeftSwipe(canvas, itemView, dX)
                }

                dX > 0 -> { // Swipe right
                    // Hide indent
                    noteItemViewHolder.binding.itemHeadIndentContainer.visibility = View.INVISIBLE

                    if (rightSwipeAction == null) {
                        rightSwipeAction = getLeftSwipeAction(inBook, recyclerView.context)
                    }

                    rightSwipeAction?.drawForRightSwipe(canvas, itemView, dX)
                }

                dX == 0f -> { // Original position
                    noteItemViewHolder.binding.itemHeadIndentContainer.visibility = View.VISIBLE

                    // Reset so it can be re-initialized if settings changed
                    leftSwipeAction = null
                    rightSwipeAction = null
                }
            }

            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun getLeftSwipeAction(inBook: Boolean, context: Context): SwipeAction {
            return if (inBook) {
                SwipeAction.OpenNote(context)
            } else {
                SwipeAction.FocusNote(context)
            }
        }

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder): Boolean {

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, direction, viewHolder.itemId, viewHolder.adapterPosition, viewHolder.layoutPosition)

            listener.onSwiped(viewHolder as NoteItemViewHolder, direction)
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return .33f
        }

        override fun getAnimationDuration(recyclerView: RecyclerView, animationType: Int, animateDx: Float, animateDy: Float): Long {
            return 0
        }
    }

    companion object {
        private val TAG = NoteItemTouchHelper::class.java.name
    }

}
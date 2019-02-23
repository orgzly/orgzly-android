package com.orgzly.android.ui.notes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils


class NoteItemTouchHelper(val listener: Listener) : ItemTouchHelper(Callback(listener)) {
//    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
//        super.onDraw(c, parent, state)
//    }

    interface Listener {
        fun onSwipeLeft(id: Long)
    }

    class Callback(val listener: Listener) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
        data class Attrs(@ColorInt val itemBg: Int, @ColorInt val labelBg: Int) {
            companion object {
                @SuppressWarnings("ResourceType")
                fun getInstance(context: Context): Attrs {
                    val typedArray = context.obtainStyledAttributes(intArrayOf(
                            android.R.attr.windowBackground,
                            R.attr.app_bar_bg_color))

                    val attrs = Attrs(typedArray.getColor(0, 0), typedArray.getColor(1, 0))

                    typedArray.recycle()

                    return attrs
                }
            }
        }

        lateinit var attrs: Attrs

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView

            val noteItemViewHolder = viewHolder as? NoteItemViewHolder ?: return

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, itemView.left, itemView.right, dX, dY, actionState, isCurrentlyActive)

            if (dX < 0) { // Swiping left

                // Hide indent
                noteItemViewHolder.indentContainer.visibility = View.INVISIBLE

                if (!::attrs.isInitialized) {
                    attrs = Attrs.getInstance(recyclerView.context)
                }

                // Label background
                ColorDrawable(attrs.labelBg).apply {
                    setBounds((itemView.right + dX).toInt(), itemView.top, itemView.right, itemView.bottom)
                    draw(c)
                }

                // Item background
                ColorDrawable(attrs.itemBg).apply {
                    setBounds(itemView.left, itemView.top, (itemView.right + dX).toInt(), itemView.bottom)
                    draw(c)
                }

                // Border

                val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = attrs.labelBg
                    style = Paint.Style.STROKE
                }

                val backgroundRectangle = RectF(
                        itemView.left.toFloat() - 1,
                        itemView.top.toFloat(),
                        itemView.right.toFloat() + 1,
                        itemView.bottom.toFloat())

                c.drawRect(backgroundRectangle, backgroundPaint)


            } else if (dX == 0f) { // Original position
                noteItemViewHolder.indentContainer.visibility = View.VISIBLE
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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

    companion object {
        private val TAG = NoteItemTouchHelper::class.java.name
    }
}
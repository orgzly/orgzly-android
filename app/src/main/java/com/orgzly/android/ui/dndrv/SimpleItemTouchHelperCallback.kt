package com.orgzly.android.ui.dndrv

import android.graphics.Canvas
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils
import kotlin.math.abs

class SimpleItemTouchHelperCallback(private val adapter: ItemTouchHelperAdapter) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val flags = if (recyclerView.layoutManager is GridLayoutManager) {
            (ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) to 0
        } else {
            (ItemTouchHelper.UP or ItemTouchHelper.DOWN) to (ItemTouchHelper.START or ItemTouchHelper.END)
        }

        return makeMovementFlags(flags.first, flags.second).also {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, viewHolder, it)
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, viewHolder, target)

        return adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        adapter.onItemDismiss(viewHolder.bindingAdapterPosition)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, viewHolder, actionState)

        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is ItemTouchHelperViewHolder) {
                viewHolder.onItemSelected()
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, viewHolder)

        viewHolder.itemView.alpha = 1f

        if (viewHolder is ItemTouchHelperViewHolder) {
            viewHolder.onItemClear()
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Swiping left/right - set alpha based on the distance
            val alpha = 1f - abs(dX) / viewHolder.itemView.width.toFloat()
            viewHolder.itemView.alpha = alpha

            viewHolder.itemView.translationX = dX

        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    companion object {
        private val TAG = SimpleItemTouchHelperCallback::class.java.name
    }
}
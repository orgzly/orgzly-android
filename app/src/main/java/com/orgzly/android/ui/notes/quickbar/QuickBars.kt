package com.orgzly.android.ui.notes.quickbar

import android.content.Context
import com.orgzly.BuildConfig
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.util.LogUtils


class QuickBars(val context: Context, val inBook: Boolean) {

    private val bars = HashMap<Long,QuickBar>()

    fun bind(holder: NoteItemViewHolder) {
        bars.values.forEach {
            it.bind(holder)
        }
    }

    fun onFling(holder: NoteItemViewHolder, direction: Int, listener: QuickBarListener) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "direction:$direction id:${holder.itemId}")

        // Close others
        bars.keys.forEach { id ->
            if (holder.itemId != id) {
                bars[id]?.close()
                bars.remove(id)
            }
        }

        val bar = bars[holder.itemId]

        if (bar != null) {
            // Fling existing
            if (bar.onFling(holder, direction, listener)) {
                bars.remove(holder.itemId)
            }

        } else {
            // Open new
            QuickBar(context, inBook).apply {
                open(holder, direction, listener)
                bars[holder.itemId] = this
            }
        }
    }

    companion object {
        private val TAG = QuickBars::class.java.name
    }
}
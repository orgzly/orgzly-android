package com.orgzly.android.ui.notes

import android.content.Context
import androidx.annotation.ColorInt
import com.orgzly.R

data class SwipeActionAttributes(
        @ColorInt val itemBg: Int,
        @ColorInt val labelBg: Int,
        @ColorInt val labelColor: Int,
        val labelSize: Int) {

    companion object {
        @SuppressWarnings("ResourceType")
        fun getInstance(context: Context): SwipeActionAttributes {
            val typedArray = context.obtainStyledAttributes(intArrayOf(
                    android.R.attr.windowBackground,
                    R.attr.indent_line_color,
                    R.attr.text_primary_color,
                    R.attr.item_head_post_title_text_size))

            val attrs = SwipeActionAttributes(
                    typedArray.getColor(0, 0),
                    typedArray.getColor(1, 0),
                    typedArray.getColor(2, 0),
                    typedArray.getDimensionPixelSize(3, 0))

            typedArray.recycle()

            return attrs
        }
    }
}
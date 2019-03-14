package com.orgzly.android.ui.notes

import android.content.Context
import androidx.annotation.ColorInt
import com.orgzly.R

data class SwipeActionAttributes(
        @ColorInt val viewBg: Int,
        @ColorInt val labelTextColor: Int,
        @ColorInt val labelBgColor: Int,
        @ColorInt val labelBorderColor: Int,
        val labelSize: Int) {

    companion object {
        @SuppressWarnings("ResourceType")
        fun getInstance(context: Context): SwipeActionAttributes {
            val typedArray = context.obtainStyledAttributes(intArrayOf(
                    android.R.attr.windowBackground,
                    R.attr.swipe_label_text_color,
                    R.attr.swipe_label_bg_color,
                    R.attr.swipe_label_border_color,
                    R.attr.item_head_post_title_text_size))

            val attrs = SwipeActionAttributes(
                    typedArray.getColor(0, 0),
                    typedArray.getColor(1, 0),
                    typedArray.getColor(2, 0),
                    typedArray.getColor(3, 0),
                    typedArray.getDimensionPixelSize(4, 0))

            typedArray.recycle()

            return attrs
        }
    }
}
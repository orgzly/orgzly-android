package com.orgzly.android.ui.notes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.StringRes
import com.orgzly.R


open class SwipeAction(val context: Context, @StringRes val res: Int) {
    private val attrs = SwipeActionAttributes.getInstance(context)

    private val labelText = context.getString(res).toUpperCase()

    private val margin = context.resources.getDimension(R.dimen.screen_edge)

    private val labelBgPaint: Paint

    private val labelTextPaint: Paint

    private val labelTextRect: Rect

    private val borderPaint: Paint

    private val itemBgPaint: Paint

    init {
        labelBgPaint = Paint().also { paint ->
            paint.color = attrs.labelBg
        }

        labelTextPaint = Paint().also { paint ->
            paint.color = attrs.labelColor
            paint.textSize = attrs.labelSize.toFloat()
            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true
            paint.typeface = Typeface.DEFAULT_BOLD

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                paint.letterSpacing = 0.05f
            }

            labelTextRect = Rect().also { rect ->
                paint.getTextBounds(labelText, 0, labelText.length, rect)
            }
        }

        borderPaint = Paint().also { paint ->
            paint.color = attrs.labelBg
            paint.style = Paint.Style.STROKE
        }

        itemBgPaint = Paint().also { paint ->
            paint.color = attrs.itemBg
        }
    }

    fun drawForLeftSwipe(c: Canvas, itemView: View, dX: Float) {
        c.drawRect(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat(),
                labelBgPaint)


        val x = itemView.right - labelTextRect.width() - margin
        val y = (itemView.top + itemView.bottom) / 2f - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2
        c.drawText(labelText, x, y, labelTextPaint)

        c.drawRect(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.right + dX,
                itemView.bottom.toFloat(),
                itemBgPaint)

        c.drawRect(
                itemView.left.toFloat() - 1,
                itemView.top.toFloat(),
                itemView.right.toFloat() + 1,
                itemView.bottom.toFloat(),
                borderPaint)
    }

    fun drawForRightSwipe(c: Canvas, itemView: View, dX: Float) {
        c.drawRect(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.right.toFloat() - dX,
                itemView.bottom.toFloat(),
                labelBgPaint)

        val x = itemView.left + margin
        val y = (itemView.top + itemView.bottom) / 2f - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2
        c.drawText(labelText, x, y, labelTextPaint)

        c.drawRect(
                itemView.left.toFloat() + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat() ,
                itemView.bottom.toFloat(),
                itemBgPaint)

        c.drawRect(
                itemView.left.toFloat() - 1,
                itemView.top.toFloat(),
                itemView.right.toFloat() + 1,
                itemView.bottom.toFloat(),
                borderPaint)
    }

    class OpenNote(context: Context) : SwipeAction(context, R.string.open)
    class FocusNote(context: Context) : SwipeAction(context, R.string.focus)
}
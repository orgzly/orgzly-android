package com.orgzly.android.ui.notes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.IdRes
import com.orgzly.R
import com.orgzly.android.ui.util.getLayoutInflater


fun interface NotePopupListener {
    fun onPopupButtonClick(@IdRes buttonId: Int)
}

object NotePopup {
    enum class Location {
        BOOK,
        QUERY
    }

    fun showWindow(anchor: View, location: Location, direction: Int, e1: MotionEvent, e2: MotionEvent, listener: NotePopupListener) {
        val context = anchor.context

        val popupView = context.getLayoutInflater().inflate(R.layout.note_popup_buttons, null)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT

        val focusable = false

        val popupWindow = PopupWindow(popupView, width, height, focusable).apply {
            isOutsideTouchable = true

            // Required on API 21 and 22 (Lollipop) so it can be dismissed on outside click
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val thisLocationButtons = getButtonsForLocation(location, direction)

        availableButtons.forEach { buttonId ->
            popupView.findViewById<Button>(buttonId)?.let { button ->
                if (thisLocationButtons.contains(buttonId)) {
                    button.setOnClickListener {
                        listener.onPopupButtonClick(buttonId)
                        popupWindow.dismiss()
                    }
                } else {
                    button.visibility = View.GONE
                }
            }
        }

//        popupWindow.setOnDismissListener {
//        }

        val gravity = Gravity.START or Gravity.TOP

        // End position of the swipe
        val x = e2.rawX.toInt()

        // Starting position of the swipe
        val y = e1.rawY.toInt()

        popupWindow.showAtLocation(anchor, gravity, x, y)
    }

    // TODO: Move to preferences.
    // TODO: Allow selecting the action only, without showing the popup (e.g. swipe right to toggle state).
    private fun getButtonsForLocation(location: Location, direction: Int): ArrayList<Int> {
        return when {
            location == Location.BOOK && direction > 0 -> bookRight
            location == Location.BOOK && direction < 0 -> bookLeft
            location == Location.QUERY && direction > 0 -> queryRight
            location == Location.QUERY && direction < 0 -> queryLeft

            else -> throw IllegalArgumentException("No buttons for $location/$direction")
        }
    }

    private val bookRight = arrayListOf(
        R.id.note_popup_set_schedule,
        R.id.note_popup_set_deadline,
        R.id.note_popup_set_state,
        R.id.note_popup_toggle_state,
//        R.id.note_popup_clock_in,
//        R.id.note_popup_clock_out,
//        R.id.note_popup_clock_cancel,
    )

    private val bookLeft = arrayListOf(
        R.id.note_popup_delete,
        R.id.note_popup_new_above,
        R.id.note_popup_new_under,
        R.id.note_popup_new_below,
        R.id.note_popup_refile
    )

    private val queryRight = arrayListOf(
        R.id.note_popup_set_schedule,
        R.id.note_popup_set_deadline,
        R.id.note_popup_set_state,
        R.id.note_popup_toggle_state,
//        R.id.note_popup_clock_in,
//        R.id.note_popup_clock_out,
//        R.id.note_popup_clock_cancel,
    )

    private val queryLeft = arrayListOf(
        R.id.note_popup_focus
    )

    private val availableButtons = arrayListOf(
        R.id.note_popup_set_schedule,
        R.id.note_popup_set_deadline,
        R.id.note_popup_set_state,
        R.id.note_popup_toggle_state,
        R.id.note_popup_clock_in,
        R.id.note_popup_clock_out,
        R.id.note_popup_clock_cancel,
        R.id.note_popup_delete,
        R.id.note_popup_new_above,
        R.id.note_popup_new_under,
        R.id.note_popup_new_below,
        R.id.note_popup_refile,
        R.id.note_popup_focus
    )

    private val TAG = NotePopup::class.java.name
}
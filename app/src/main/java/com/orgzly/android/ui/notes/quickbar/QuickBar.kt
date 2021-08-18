package com.orgzly.android.ui.notes.quickbar

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ViewFlipper
import androidx.annotation.IdRes
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.notes.NoteItemViewHolder
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils


class QuickBar(val context: Context, val inBook: Boolean) {

    private var state = State()

    private val animator = QuickBarAnimator(context)


    fun bind(holder: NoteItemViewHolder) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$state - bind -> id:${holder.itemId}")

        if (state.id == holder.itemId) {
            if (holder.binding.quickBar.quickBarLeft.childCount == 0 && holder.binding.quickBar.quickBarRight.childCount == 0) {
                val direction = state.direction
                val listener = state.listener

                if (direction != null && listener != null) {
                    inflate(holder, listener, direction)
                }
            }

            holder.binding.quickBar.quickBarFlipper.visibility = View.VISIBLE

            state.direction?.let {
                animator.removeFlipperAnimation(holder.binding.quickBar.quickBarFlipper)

                flipForDirection(holder.binding.quickBar.quickBarFlipper, it)
            }

            // Update view as user scrolls
            state = state.copy(layout = holder.binding.quickBar.quickBarFlipper)

        } else {
            holder.binding.quickBar.quickBarFlipper.visibility = View.GONE
        }
    }

    fun onFling(holder: NoteItemViewHolder, direction: Int, listener: QuickBarListener): Long? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$state -> direction:$direction id:${holder.itemId}")

        if (state.id == holder.itemId) { // Same note
            if (state.direction == direction) { // Same direction
                close()
                return null

            } else { // Different direction
                change(holder, direction, listener)
                return state.id
            }

        } else { // Different note
            close()
            return holder.itemId
        }
    }

    fun open(holder: NoteItemViewHolder, direction: Int, listener: QuickBarListener) {
        inflate(holder, listener, direction)

        animator.removeFlipperAnimation(holder.binding.quickBar.quickBarFlipper)

        animator.open(holder.binding.quickBar.quickBarFlipper)

        state = State(direction, holder.itemId, holder.binding.quickBar.quickBarFlipper, listener)
    }

    private fun change(holder: NoteItemViewHolder, direction: Int, listener: QuickBarListener) {
        animator.setFlipperAnimation(holder.binding.quickBar.quickBarFlipper, direction)

        flipForDirection(holder.binding.quickBar.quickBarFlipper, direction)

        state = State(direction, holder.itemId, holder.binding.quickBar.quickBarFlipper, listener)
    }

    private fun inflate(holder: NoteItemViewHolder, listener: QuickBarListener, direction: Int) {
        val inflater = LayoutInflater.from(context)

        context.styledAttributes(R.styleable.Icons) { typedArray ->
            fun doInflate(container: ViewGroup, buttons: List<Button>) {
                container.removeAllViews()

                buttons.forEach { button ->
                    (inflater.inflate(R.layout.quick_bar_button, container, false) as ImageButton).apply {
                        id = button.id

                        setImageResource(typedArray.getResourceId(button.icon, 0))

                        setOnClickListener {
                            listener.onQuickBarButtonClick(button.id, holder.itemId)
                        }

                        container.addView(this)
                    }
                }
            }

            Buttons.fromPreferences(context).let {
                doInflate(holder.binding.quickBar.quickBarLeft, if (inBook) it.leftSwipeInBook else it.leftSwipeInQuery)
                doInflate(holder.binding.quickBar.quickBarRight, it.rightSwipeInBook)
            }

            animator.removeFlipperAnimation(holder.binding.quickBar.quickBarFlipper)

            flipForDirection(holder.binding.quickBar.quickBarFlipper, direction)
        }
    }

    private fun flipForDirection(flipper: ViewFlipper, direction: Int) {
        val child = if (direction == -1) 0 else 1

        if (flipper.displayedChild != child) {
            flipper.displayedChild = child
        }
    }

    fun close() {
        val layout = state.layout
        val id = state.id

        if (layout != null && id != null) {
            animator.close(layout, true)
        }

        state = State()
    }

    data class State(
            val direction: Int? = null,
            val id: Long? = null,
            val layout: ViewGroup? = null,
            val listener: QuickBarListener? = null) {

        override fun toString(): String {
            return "State(direction:$direction id:$id)"
        }
    }

    // TODO: Add contentDescription
    data class Button(@IdRes val id: Int, val icon: Int)

    data class Buttons(
            val rightSwipeInBook: List<Button>,
            val leftSwipeInBook: List<Button>,
            val leftSwipeInQuery: List<Button>
    ) {
        companion object {

            private val RIGHT_IN_BOOK = listOf(
                    Button(R.id.quick_bar_schedule, R.styleable.Icons_ic_today_24dp),
                    Button(R.id.quick_bar_deadline, R.styleable.Icons_ic_alarm_24dp),
                    Button(R.id.quick_bar_state, R.styleable.Icons_ic_flag_24dp),
                    Button(R.id.quick_bar_done, R.styleable.Icons_ic_outline_check_circle_24dp),
                    Button(R.id.quick_bar_clock_in, R.styleable.Icons_ic_hourglass_top),
                    Button(R.id.quick_bar_clock_out, R.styleable.Icons_ic_hourglass_bottom),
                    Button(R.id.quick_bar_clock_cancel, R.styleable.Icons_ic_hourglass_disabled)
            )

            private val LEFT_IN_BOOK = listOf(
                    Button(R.id.quick_bar_delete, R.styleable.Icons_ic_delete_24dp),
                    Button(R.id.quick_bar_new_above, R.styleable.Icons_oic_new_above_24dp),
                    Button(R.id.quick_bar_new_under, R.styleable.Icons_oic_new_under_24dp),
                    Button(R.id.quick_bar_new_below, R.styleable.Icons_oic_new_below_24dp),
                    Button(R.id.quick_bar_refile, R.styleable.Icons_ic_move_to_inbox_24dp)
            )

            private val LEFT_IN_QUERY = listOf(
                    Button(R.id.quick_bar_focus, R.styleable.Icons_ic_center_focus_strong_24dp)
            )

            fun fromPreferences(@Suppress("UNUSED_PARAMETER") context: Context): Buttons {
                // TODO: Allow user to modify buttons
                return Buttons(RIGHT_IN_BOOK, LEFT_IN_BOOK, LEFT_IN_QUERY)
            }
        }
    }

    companion object {
        private val TAG = QuickBar::class.java.name
    }
}
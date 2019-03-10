package com.orgzly.android.ui.notes.quickbar

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ViewFlipper
import androidx.core.view.ViewCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.util.LogUtils

class QuickBarAnimator(val context: Context) {
    private val animationDuration = context.resources.getInteger(R.integer.quick_bar_animation_duration)

    private val height = context.resources.getDimension(R.dimen.quick_bar_height)

    private var state = State.CLOSED


    fun open(actionBar: ViewFlipper) {
        state = State.OPENING
        ViewCompat.setHasTransientState(actionBar, true)

        val animations = AnimationSet(false)

        animations.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                state = State.OPENED
                ViewCompat.setHasTransientState(actionBar, false)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        /* Fade in animation. */

        AnimationUtils.loadAnimation(context, R.anim.fade_in).apply {
            interpolator = OPEN_INTERPOLATOR
            duration = animationDuration.toLong()

            animations.addAnimation(this)
        }


        /* Height change animation. */

        val params = actionBar.layoutParams as ViewGroup.MarginLayoutParams

        // Initial state
        params.height = 0
        actionBar.visibility = View.VISIBLE

        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)

                /* New margin. */
                // params.bottomMargin = - params.height + (int) ((params.height) * interpolatedTime);
                params.height = (height * interpolatedTime).toInt()

                /* Invalidating the layout, to see the made changes. */
                actionBar.requestLayout()

                /*
                 * Make sure entire item is visible while opening the quick bar.
                 * Does not work well when flinging the list,
                 * as the item keeps getting scrolled to.
                 */
                // gesturedListView.smoothScrollToPosition(itemPosition);
            }
        }.apply {
            interpolator = OPEN_INTERPOLATOR
            duration = animationDuration.toLong()

            animations.addAnimation(this)
        }

        actionBar.startAnimation(animations)
    }

    fun setFlipperAnimation(flipperView: ViewFlipper, direction: Int) {
        /*
         * TODO: If container is closed, we don't want flipper animation.
         * Appearance of the container itself will be animated.
         */
        if (false) { // isContainerViewGone()
            flipperView.inAnimation = null
            flipperView.outAnimation = null

        } else {
            when (direction) {
                -1 -> {
                    flipperView.setOutAnimation(context, R.anim.slide_out_to_left)
                    flipperView.setInAnimation(context, R.anim.slide_in_from_right)

                }

                1 -> {
                    flipperView.setOutAnimation(context, R.anim.slide_out_to_right)
                    flipperView.setInAnimation(context, R.anim.slide_in_from_left)

                }

                else -> {
                    flipperView.inAnimation = null
                    flipperView.outAnimation = null
                }
            }
        }
    }

    fun close(actionBar: ViewGroup, animate: Boolean) {
        if (state != State.CLOSING && state != State.CLOSED) {
            val layoutParams = actionBar.layoutParams as ViewGroup.MarginLayoutParams

            if (animate) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Closing with animation")

                state = State.CLOSING
                ViewCompat.setHasTransientState(actionBar, true)

                val animations = createAnimations(actionBar, layoutParams)

                actionBar.startAnimation(animations)

            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Closing without animations")
                closeContainer(actionBar, layoutParams)
            }

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Already CLOSED or CLOSING")
        }
    }

    private fun createAnimations(
            actionBar: ViewGroup, layoutParams: ViewGroup.MarginLayoutParams): AnimationSet {

        val animations = AnimationSet(false)

        animations.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                closeContainer(actionBar, layoutParams)
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Quick bar closed - animation ended")
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        /* Fade out animation. */
        AnimationUtils.loadAnimation(context, R.anim.fade_out).apply {
            interpolator = CLOSE_INTERPOLATOR
            duration = animationDuration.toLong()

            animations.addAnimation(this)
        }

        /* Margin change animation. */

        object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)

                /* New margin. */
                // params.bottomMargin = - (int) ((params.height) * interpolatedTime);
                layoutParams.height = (height - height * interpolatedTime).toInt()

                /* Invalidating the layout, to see the made changes. */
                actionBar.requestLayout()

                /* Make sure entire item is visible while opening the quick bar. */
                // ListView.this.smoothScrollToPosition(itemPosition);

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Closing quick bar (" + layoutParams.height + ")")
            }
        }.apply {
            interpolator = CLOSE_INTERPOLATOR
            duration = animationDuration.toLong()

            animations.addAnimation(this)
        }

        return animations
    }

    private fun closeContainer(actionBar: ViewGroup, params: ViewGroup.MarginLayoutParams) {
        state = State.CLOSED

        actionBar.visibility = View.GONE
        params.height = 0

        actionBar.requestLayout()

        ViewCompat.setHasTransientState(actionBar, false)
    }


    private enum class State {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING
    }

    companion object {
        private val TAG = QuickBarAnimator::class.java.name

        private val OPEN_INTERPOLATOR = DecelerateInterpolator()
        private val CLOSE_INTERPOLATOR = DecelerateInterpolator()
    }
}
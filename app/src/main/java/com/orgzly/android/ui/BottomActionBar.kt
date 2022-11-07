package com.orgzly.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.appcompat.widget.Toolbar

object BottomActionBar {
    private const val ANIMATION_DURATION = 200L

    @JvmStatic
    fun showBottomBar(view: Toolbar, clickListener: Callback) {
        view.menu.clear()
        clickListener.onInflateBottomActionMode(view)

        view.setOnMenuItemClickListener { menuItem ->
            clickListener.onBottomActionItemClicked(menuItem.itemId)
            true
        }

        view.visibility = View.VISIBLE

        view.animate()
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                    }
                })
    }

    @JvmStatic
    fun hideBottomBar(view: Toolbar) {
        if (view.visibility == View.GONE) {
            return
        }

        view.animate()
                .translationY(view.height.toFloat())
                .setDuration(ANIMATION_DURATION)
                .setListener(object : AnimatorListenerAdapter() {
                    var isCanceled = false

                    override fun onAnimationCancel(animation: Animator) {
                        isCanceled = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (!isCanceled) {
                            view.visibility = View.GONE
                        }
                    }
                })
    }

    interface Callback {
        fun onInflateBottomActionMode(toolbar: Toolbar)

        fun onBottomActionItemClicked(id: Int)
    }
}
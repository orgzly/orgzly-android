package com.orgzly.android.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.appcompat.widget.Toolbar

object BottomActionBar {
    @JvmStatic
    fun showBottomBar(view: Toolbar, clickListener: Callback) {
        view.menu.clear()
        clickListener.onInflateBottomActionMode(view)

        view.setOnMenuItemClickListener { menuItem ->
            clickListener.onBottomActionItemClicked(menuItem.itemId)
            true
        }

        view.animate()
                .translationY(0f)
                .alpha(1.0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        view.visibility = View.VISIBLE
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
                .alpha(0.0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        view.visibility = View.GONE
                    }
                })
    }


    interface Callback {
        fun onInflateBottomActionMode(toolbar: Toolbar)

        fun onBottomActionItemClicked(id: Int)
    }
}
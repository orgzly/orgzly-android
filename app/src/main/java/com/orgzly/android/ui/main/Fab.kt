package com.orgzly.android.ui.main

import androidx.fragment.app.FragmentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.orgzly.R

object Fab {
    private fun fab(activity: FragmentActivity): FloatingActionButton? {
        return activity.findViewById(R.id.fab)
    }

    @JvmStatic
    fun hide(activity: FragmentActivity) {
        fab(activity)?.hide()
    }

    @JvmStatic
    fun show(activity: FragmentActivity) {
        fab(activity)?.show()
    }
}
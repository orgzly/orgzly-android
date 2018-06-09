package com.orgzly.android.ui

import android.support.design.widget.FloatingActionButton
import android.support.v4.app.FragmentActivity
import com.orgzly.R
import com.orgzly.android.ui.util.ActivityUtils

object MainFab {

    /**
     * Update floating action button's look and action
     */
    @JvmStatic
    fun updateFab(activity: FragmentActivity, fragmentTag: String, selectionCount: Int) {
        val fab = activity.findViewById(R.id.fab) as FloatingActionButton

        val fragment = activity.supportFragmentManager.findFragmentByTag(fragmentTag)

        if (fragment == null) {
            fab.hide()
            return
        }

        /* Hide FAB if there are selected notes. */
        if (selectionCount > 0) {
            fab.hide()
            return
        }

        if (fragment is Fab) {
            val fabAction = fragment.fabAction
            val resources = ActivityUtils.FragmentResources(activity, fragmentTag)

            if (resources.fabDrawable != null && fabAction != null) {
                fab.show()

                fab.setImageDrawable(resources.fabDrawable)

                fab.setOnClickListener { fabAction.run() }

            } else {
                fab.hide()
            }

        } else {
            fab.hide()
        }
    }
}

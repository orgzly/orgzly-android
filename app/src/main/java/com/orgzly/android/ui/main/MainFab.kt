package com.orgzly.android.ui.main

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.orgzly.R
import com.orgzly.android.ui.Fab
import com.orgzly.android.ui.books.BooksFragment
import com.orgzly.android.ui.notes.book.BookFragment
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment

object MainFab {

    /**
     * Update floating action button's look and action
     */
    @JvmStatic
    fun updateFab(activity: androidx.fragment.app.FragmentActivity, fragmentTag: String, selectionCount: Int) {
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
            val resources = FragmentResources(activity, fragmentTag)

            if (resources.fabDrawable != null && fabAction != null) {
                // Added for https://issuetracker.google.com/issues/111316656
                fab.hide()

                fab.setImageDrawable(resources.fabDrawable)

                fab.setOnClickListener { fabAction.run() }

                fab.show()

            } else {
                fab.hide()
            }

        } else {
            fab.hide()
        }
    }

    private class FragmentResources(context: Context, fragmentTag: String) {
        val fabDrawable: Drawable?

        init {
            val fabAttr = when (fragmentTag) {
                SavedSearchesFragment.FRAGMENT_TAG -> R.attr.ic_add_24dp
                BooksFragment.FRAGMENT_TAG   -> R.attr.ic_add_24dp
                BookFragment.FRAGMENT_TAG    -> R.attr.ic_add_24dp
                BookFragment.FRAGMENT_TAG    -> R.attr.ic_add_24dp
                else -> 0
            }

            val typedArray = context.obtainStyledAttributes(intArrayOf(fabAttr))
            fabDrawable = typedArray.getDrawable(0)
            typedArray.recycle()
        }
    }
}

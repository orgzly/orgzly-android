@file:JvmName("AppSnackbarUtils")

package com.orgzly.android.ui

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.snackbar.Snackbar
import com.orgzly.R
import com.orgzly.android.ui.util.styledAttributes

@JvmOverloads
fun Activity.showSnackbar(@StringRes resId: Int, @StringRes actionResId: Int? = null, action: (() -> Unit)? = null) {
    showSnackbar(getString(resId), actionResId, action)
}

@JvmOverloads
fun Activity.showSnackbar(msg: String?, @StringRes actionResId: Int? = null, action: (() -> Unit)? = null) {
    AppSnackbar.showSnackbar(this, msg, actionResId, action)
}

object AppSnackbar {
    private var snackbar: Snackbar? = null

    fun showSnackbar(activity: Activity, msg: String?, @StringRes actionResId: Int? = null, action: (() -> Unit)? = null) {
        if (msg != null) {
            val view = activity.findViewById<View>(R.id.main_content) ?: return

            val snack = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)

            if (actionResId != null && action != null) {
                snack.setAction(actionResId) {
                    action()
                }
            }

            snack.anchorView = anchorView(activity)

            showSnackbar(activity, snack)
        }
    }

    private fun anchorView(activity: Activity): View? {
        activity.findViewById<View>(R.id.fab)?.run {
            if (visibility == View.VISIBLE) {
                return this
            }
        }

        activity.findViewById<View>(R.id.bottom_toolbar)?.run {
            if (visibility == View.VISIBLE) {
                return this
            }
        }

        return null
    }

    private fun showSnackbar(activity: Activity, snack: Snackbar) {
        // Dismiss previous snackbar
        dismiss()

        // Close drawer before displaying snackbar
        activity.findViewById<DrawerLayout>(R.id.drawer_layout)?.closeDrawer(GravityCompat.START)

        // On dismiss
        snack.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                snackbar = null
            }
        })

        // Show the snackbar
        snackbar = snack.apply {
            show()
        }
    }

    fun dismiss() {
        snackbar?.dismiss()
    }
}
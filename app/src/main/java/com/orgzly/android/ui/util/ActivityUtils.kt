package com.orgzly.android.ui.util

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.Toolbar
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.util.LogUtils

object ActivityUtils {
    private val TAG = ActivityUtils::class.java.name

    @JvmStatic
    fun closeSoftKeyboard(activity: Activity?) {
        if (activity != null) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Hiding keyboard in activity $activity")

            // If no view currently has focus, create a new one to grab a window token from it
            val view = activity.currentFocus ?: View(activity)

            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun openSoftKeyboard(
            activity: Activity?,
            view: View?,
            scrollView: ScrollView? = null,
            scrollToTopOfView: View? = null) {

        openSoftKeyboardWithDelay(activity, view, 0, scrollView, scrollToTopOfView)
    }

    // TODO: Remove, open immediately when ready
    @JvmStatic
    @JvmOverloads
    fun openSoftKeyboardWithDelay(
            activity: Activity?,
            viewToFocus: View?,
            delay: Long = 200,
            scrollView: ScrollView? = null,
            scrollToTopOfView: View? = null) {

        if (activity != null) {

            // Focus on view or use currently focused view
            val focusedView = if (viewToFocus != null) {
                if (viewToFocus.requestFocus()) {
                    viewToFocus
                } else {
                    null // Failed to get focus
                }
            } else {
                activity.currentFocus
            }

            if (focusedView != null) {
                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "Showing keyboard for view $focusedView in activity $activity")

                doOpenSoftKeyboard(activity, focusedView, delay, scrollView, scrollToTopOfView)

            } else {
                Log.w(TAG, "Can't open keyboard because view " + viewToFocus +
                        " failed to get focus in activity " + activity)
            }
        }
    }

    private fun doOpenSoftKeyboard(
            activity: Activity,
            view: View,
            delay: Long,
            scrollView: ScrollView?,
            scrollToTopOfView: View? = null) {

        val listener = if (scrollView != null && scrollToTopOfView != null) {
            // Keep scrolling the view as the keyboard opens
            ViewTreeObserver.OnGlobalLayoutListener {
                scrollView.scrollTo(0, scrollToTopOfView.top)
            }
        } else {
            null
        }

        scrollView?.viewTreeObserver?.addOnGlobalLayoutListener(listener)

        Handler().postDelayed({
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, delay)

        if (scrollView != null) {
            Handler().postDelayed({
                scrollView.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
            }, delay + 500)
        }
    }

    /**
     * Open "App info" settings, where permissions can be granted.
     */
    fun openAppInfoSettings(activity: Activity) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

        intent.data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)

        activity.startActivity(intent)
    }

    @JvmStatic
    fun mainActivityPendingIntent(context: Context, bookId: Long, noteId: Long): PendingIntent {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookId, noteId)

        val intent = Intent.makeRestartActivityTask(ComponentName(context, MainActivity::class.java))

        intent.putExtra(AppIntent.EXTRA_BOOK_ID, bookId)
        intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)

        return PendingIntent.getActivity(
                context,
                noteId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @JvmStatic
    fun keepScreenOnToggle(activity: Activity?, item: MenuItem): AlertDialog? {
        activity ?: return null

        val flags = activity.window.attributes.flags
        val keepScreenOnEnabled = flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

        if (!keepScreenOnEnabled) {
            return AlertDialog.Builder(activity)
                    .setTitle(R.string.keep_screen_on)
                    .setMessage(R.string.keep_screen_on_desc)
                    .setPositiveButton(android.R.string.yes) { dialog, _ ->
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        item.isChecked = true
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            item.isChecked = false
            return null
        }
    }

    @JvmStatic
    fun keepScreenOnUpdateMenuItem(activity: Activity?, menu: Menu, item: MenuItem?) {
        if (activity != null && item != null) {
            if (AppPreferences.keepScreenOnMenuItem(activity)) {
                val flags = activity.window.attributes.flags
                val keepScreenOnEnabled = flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
                item.isChecked = keepScreenOnEnabled

            } else {
                menu.removeItem(item.itemId)
            }
        }
    }

    @JvmStatic
    fun keepScreenOnClear(activity: Activity?) {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun distributeToolbarItems(activity: Activity?, toolbar: Toolbar) {
        if (activity != null) {
            val display = activity.windowManager.defaultDisplay

            val metrics = DisplayMetrics().also {
                display.getMetrics(it)
            }

            val screenWidth = metrics.widthPixels

            for (i in 0 until toolbar.childCount) {
                val childView = toolbar.getChildAt(i)

                if (childView is ViewGroup) {
                    val innerChildCount = childView.childCount

                    val itemWidth = screenWidth / innerChildCount

                    for (j in 0 until innerChildCount) {
                        val grandChild = childView.getChildAt(j)

                        if (grandChild is ActionMenuItemView) {
                            grandChild.layoutParams.width = itemWidth
                        }
                    }
                }
            }
        }
    }
}

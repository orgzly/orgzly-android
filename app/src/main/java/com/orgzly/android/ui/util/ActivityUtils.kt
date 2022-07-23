package com.orgzly.android.ui.util

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Hiding keyboard, current focus ${activity.currentFocus}")

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
        viewToFocus: View?,
        delay: Long = 0,
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

        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        showSoftInput(imm, view, delay, 0) {
            if (scrollView != null) {
                Handler().postDelayed({
                    scrollView.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
                }, 500)
            }
        }
    }

    // With animations enabled, keyboard is not shown immediately. Try a few times
    // TODO: Display keyboard when ready
    private fun showSoftInput(imm: InputMethodManager, view: View, delay: Long, attempt: Int = 0, onShow: () -> Unit) {
        Handler().postDelayed({
            val shown = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

            if (BuildConfig.LOG_DEBUG) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    LogUtils.d(TAG, "Keyboard shown: $shown (view attached:${view.isAttachedToWindow} has-focus:${view.hasFocus()})")
                }
            }

            if (shown) {
                onShow()

            } else {
                if (attempt < 3) {
                    showSoftInput(imm, view, delay + 100, attempt + 1, onShow)
                } else {
                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Failed to show keyboard after $attempt retries")
                }
            }

        }, delay)
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
            immutable(PendingIntent.FLAG_UPDATE_CURRENT))
    }

    fun keepScreenOnToggle(activity: Activity?, item: MenuItem): AlertDialog? {
        activity ?: return null

        if (!isKeepScreenOn(activity)) {
            return MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.keep_screen_on)
                .setMessage(R.string.keep_screen_on_desc)
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    keepScreenOnSet(activity)
                    item.isChecked = true
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            keepScreenOnClear(activity)
            item.isChecked = false
            return null
        }
    }

    fun keepScreenOnUpdateMenuItem(activity: Activity?, menu: Menu) {
        val item = menu.findItem(R.id.keep_screen_on)
        if (activity != null && item != null) {
            if (AppPreferences.keepScreenOnMenuItem(activity)) {
                item.isChecked = isKeepScreenOn(activity)
            } else {
                menu.removeItem(item.itemId)
            }
        }
    }

    fun keepScreenOnClear(activity: Activity?) {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun keepScreenOnSet(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun isKeepScreenOn(activity: Activity): Boolean {
        val flags = activity.window.attributes.flags
        return flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

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

                    /*
                     * Use 1 less pixel for item width to avoid exception in tests:
                     * Caused by: java.lang.RuntimeException: Action will not be performed because the target view does not match one or more of the following constraints:
                     * at least 90 percent of the view's area is displayed to the user.
                     */
                    val itemWidth = screenWidth / innerChildCount - 1

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

    @JvmStatic
    fun immutable(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags or PendingIntent.FLAG_IMMUTABLE
        } else {
            flags
        }
    }

    @JvmStatic
    fun mutable(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags or PendingIntent.FLAG_MUTABLE
        } else {
            flags
        }
    }
}

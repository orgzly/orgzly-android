package com.orgzly.android.ui.util

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

object KeyboardUtils {
    private val TAG = KeyboardUtils::class.java.name

    private const val TIMES_TO_TRY_OPEN = 3

    @JvmStatic
    fun closeSoftKeyboard(activity: Activity?) {
        if (activity != null) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Hiding the keyboard, current focus ${activity.currentFocus}")

            // If no view currently has focus, create a new one to grab a window token from it
            val view = activity.currentFocus ?: View(activity)

            activity.getInputMethodManager().hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun openSoftKeyboard(view: View, onShow: (() -> Unit)? = null) {
        if (view.requestFocus()) {
            doOpenSoftKeyboard(view, onShow)
        } else {
            Log.w(TAG, "Can't open the keyboard because view $view failed to get focus")
        }
    }

    private fun doOpenSoftKeyboard(view: View, onShow: (() -> Unit)?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Showing the keyboard for view $view")

        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            if (onShow != null) {
                onShow()
            }
        }

        view.rootView.viewTreeObserver?.addOnGlobalLayoutListener(listener)
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Listener added")

        showSoftInput(view.context.getInputMethodManager(), view, 0, 0, onShow)

        Handler(Looper.getMainLooper()).postDelayed({
            view.rootView.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Listener removed")
        }, 500)
    }

    /**
     * Sometimes keyboard is not shown immediately. Try a few times.
     */
    private fun showSoftInput(
        imm: InputMethodManager, view: View, delay: Long, attempt: Int = 0, onShow: (() -> Unit)?) {

        Handler(Looper.getMainLooper()).postDelayed({
            val shown = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)

            if (BuildConfig.LOG_DEBUG) {
                LogUtils.d(TAG, "Keyboard shown: $shown "
                        + "(view attached:${view.isAttachedToWindow} has-focus:${view.hasFocus()})")
            }

            if (!shown) {
                if (attempt < TIMES_TO_TRY_OPEN) {
                    showSoftInput(imm, view, delay + 100, attempt + 1, onShow)
                } else {
                    if (BuildConfig.LOG_DEBUG)
                        LogUtils.d(TAG, "Failed to show keyboard after $attempt tries")
                }
            }
        }, delay)
    }
}
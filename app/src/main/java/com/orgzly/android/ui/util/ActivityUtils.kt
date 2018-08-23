package com.orgzly.android.ui.util

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.ui.MainActivity
import com.orgzly.android.ui.fragments.BookFragment
import com.orgzly.android.ui.fragments.BooksFragment
import com.orgzly.android.ui.fragments.FiltersFragment
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
    fun openSoftKeyboard(activity: Activity?, view: View) {
        if (activity != null) {
            if (view.requestFocus()) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Showing keyboard for view $view in activity $activity")

                Handler().postDelayed({
                    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }, 200)

            } else {
                Log.w(TAG, "Can't open keyboard because view " + view +
                           " failed to get focus in activity " + activity)
            }
        }
    }

    class FragmentResources(context: Context, fragmentTag: String) {
        val fabDrawable: Drawable?

        init {
            val fabAttr = when (fragmentTag) {
                FiltersFragment.FRAGMENT_TAG -> R.attr.ic_add_24dp
                BooksFragment.FRAGMENT_TAG   -> R.attr.ic_add_24dp
                BookFragment.FRAGMENT_TAG    -> R.attr.ic_add_24dp
                else -> 0
            }

            val typedArray = context.obtainStyledAttributes(intArrayOf(fabAttr))
            fabDrawable = typedArray.getDrawable(0)
            typedArray.recycle()
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
}

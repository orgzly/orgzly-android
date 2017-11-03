package com.orgzly.android.ui

import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.dialogs.WhatsNewDialog
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils

/**
 * Inherited by every activity in the app.
 */
abstract class CommonActivity : AppCompatActivity() {

    private var snackbar: Snackbar? = null

    private var whatsNewDialog: AlertDialog? = null

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Received broadcast: $intent")

            when (intent.action) {
                AppIntent.ACTION_DB_UPGRADE_STARTED ->
                    if (whatsNewDialog != null) {
                        whatsNewDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.running_database_update)
                        whatsNewDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
                        whatsNewDialog!!.setCancelable(false)
                    }

                AppIntent.ACTION_DB_UPGRADE_ENDED ->
                    if (whatsNewDialog != null) {
                        whatsNewDialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.ok)
                        whatsNewDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
                        whatsNewDialog!!.setCancelable(true)
                    }

                AppIntent.ACTION_BOOK_LOADED ->
                    showSimpleSnackbarLong(R.string.notebook_loaded)
            }
        }
    }

    protected var actionAfterPermissionGrant: Runnable? = null

    private val snackbarBackgroundColor: Int
        get() {
            val arr = obtainStyledAttributes(R.styleable.ColorScheme)
            val color = arr.getColor(R.styleable.ColorScheme_snackbar_bg_color, 0)
            arr.recycle()
            return color
        }

    private fun dismissSnackbar() {
        if (snackbar != null) {
            snackbar!!.dismiss()
            snackbar = null
        }
    }

    fun showSimpleSnackbarLong(resId: Int) {
        showSimpleSnackbarLong(getString(resId))
    }

    fun showSimpleSnackbarLong(message: String) {
        val view = findViewById(R.id.main_content)
        if (view != null) {
            showSnackbar(Snackbar.make(view, message, Snackbar.LENGTH_LONG))
        }
    }

    fun showSnackbar(s: Snackbar) {
        dismissSnackbar()

        /* Close drawer before displaying snackbar. */
        val drawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout != null) {
            (drawerLayout as DrawerLayout).closeDrawer(GravityCompat.START)
        }

        snackbar = s

        /* Set background color from attribute. */
        val bgColor = snackbarBackgroundColor
        snackbar!!.view.setBackgroundColor(bgColor)

        snackbar!!.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        dismissSnackbar()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val consumed = super.dispatchTouchEvent(ev)

        if (ev.action == MotionEvent.ACTION_UP) {
            dismissSnackbar()
        }

        return consumed
    }

    /**
     * Set theme and styles.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        setupTheme()

        setupLayoutDirection()

        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(AppIntent.ACTION_DB_UPGRADE_STARTED)
        intentFilter.addAction(AppIntent.ACTION_DB_UPGRADE_ENDED)
        intentFilter.addAction(AppIntent.ACTION_BOOK_LOADED)
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()

        /* Dismiss What's new dialog. */
        if (whatsNewDialog != null) {
            whatsNewDialog!!.dismiss()
            whatsNewDialog = null
        }
    }

    protected fun displayWhatsNewDialog() {
        if (whatsNewDialog != null) {
            whatsNewDialog!!.dismiss()
        }

        whatsNewDialog = WhatsNewDialog.create(this)
        whatsNewDialog!!.setOnDismissListener { dialog -> whatsNewDialog = null }
        whatsNewDialog!!.show()
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)
    }

    private fun setupLayoutDirection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val setting = AppPreferences.layoutDirection(this)

            var layoutDirection = View.LAYOUT_DIRECTION_LOCALE

            if (getString(R.string.pref_value_layout_direction_ltr) == setting) {
                layoutDirection = View.LAYOUT_DIRECTION_LTR

            } else if (getString(R.string.pref_value_layout_direction_rtl) == setting) {
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

            window.decorView.layoutDirection = layoutDirection
        }
    }

    private fun setupTheme() {
        /*
         * Set theme - color scheme.
         */
        val colorScheme = AppPreferences.colorScheme(this)

        if (getString(R.string.pref_value_color_scheme_dark) == colorScheme) {
            setTheme(R.style.AppDarkTheme_Dark)

        } else if (getString(R.string.pref_value_color_scheme_black) == colorScheme) {
            setTheme(R.style.AppDarkTheme_Black)

        } else {
            setTheme(R.style.AppLightTheme_Light)
        }

        /*
         * Apply font style based on preferences.
         */
        val fontSizePref = AppPreferences.fontSize(this)

        if (getString(R.string.pref_value_font_size_large) == fontSizePref) {
            theme.applyStyle(R.style.FontSize_Large, true)
        } else if (getString(R.string.pref_value_font_size_small) == fontSizePref) {
            theme.applyStyle(R.style.FontSize_Small, true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            AppPermissions.FOR_BOOK_EXPORT -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (actionAfterPermissionGrant != null) {
                        actionAfterPermissionGrant!!.run()
                        actionAfterPermissionGrant = null
                    }
                }
            }
        }
    }

    fun popBackStackAndCloseKeyboard() {
        supportFragmentManager.popBackStack()
        ActivityUtils.closeSoftKeyboard(this)
    }

    companion object {
        private val TAG = CommonActivity::class.java.name
    }
}

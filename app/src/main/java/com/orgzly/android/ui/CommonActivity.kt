package com.orgzly.android.ui

import android.app.AlertDialog
import android.app.ProgressDialog
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
import android.content.SharedPreferences
import android.preference.PreferenceManager


/**
 * Inherited by every activity in the app.
 */
abstract class CommonActivity : AppCompatActivity() {

    /* Dialogs and Snackbars. */
    private var snackbar: Snackbar? = null
    private var whatsNewDialog: AlertDialog? = null
    private var promptDialog: AlertDialog? = null
    private var progressDialog: ProgressDialog? = null

    /* Actions. */
    private var restartActivity = false
    @JvmField protected var clearFragmentBackstack = false

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Received broadcast: $intent")

            when (intent.action) {
                AppIntent.ACTION_DB_UPGRADE_STARTED -> {
                    whatsNewDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setText(R.string.running_database_update)
                    whatsNewDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
                    whatsNewDialog?.setCancelable(false)
                }

                AppIntent.ACTION_DB_UPGRADE_ENDED -> {
                    whatsNewDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setText(R.string.ok)
                    whatsNewDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = true
                    whatsNewDialog?.setCancelable(true)
                }

                AppIntent.ACTION_BOOK_IMPORTED ->
                    showSimpleSnackbarLong(R.string.notebook_imported)

                AppIntent.ACTION_DB_CLEARED ->
                    clearFragmentBackstack = true

                AppIntent.ACTION_REPARSING_NOTES_STARTED -> {
                    progressDialog = ProgressDialog(this@CommonActivity)
                    progressDialog?.setMessage(resources.getString(R.string.updating_notes))
                    progressDialog?.isIndeterminate = true
                    progressDialog?.show()
                }

                AppIntent.ACTION_REPARSING_NOTES_ENDED ->
                    progressDialog?.dismiss()

                AppIntent.ACTION_DISPLAY_MESSAGE ->
                    showSimpleSnackbarLong(intent.getStringExtra(AppIntent.EXTRA_MESSAGE))
            }
        }
    }

    private val settingsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Schedule recreate of activities when any preference is changed
        restartActivity = true

        // Some preferences require Settings activity to be recreated immediately
        if (key in PREFS_REQUIRE_IMMEDIATE_ACTIVITY_RECREATE.map { getString(it) }) {
            recreateActivityForSettingsChange()
        }
    }

    open fun recreateActivityForSettingsChange() {
    }

    private val snackbarBackgroundColor: Int
        get() {
            val arr = obtainStyledAttributes(R.styleable.ColorScheme)
            val color = arr.getColor(R.styleable.ColorScheme_snackbar_bg_color, 0)
            arr.recycle()
            return color
        }

    private fun dismissSnackbar() {
        snackbar?.let {
            it.dismiss()
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

        /* Set background color from attribute. */
        val bgColor = snackbarBackgroundColor
        s.view.setBackgroundColor(bgColor)

        s.show()

        snackbar = s
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
        intentFilter.addAction(AppIntent.ACTION_BOOK_IMPORTED)
        intentFilter.addAction(AppIntent.ACTION_DB_CLEARED)
        intentFilter.addAction(AppIntent.ACTION_REPARSING_NOTES_STARTED)
        intentFilter.addAction(AppIntent.ACTION_REPARSING_NOTES_ENDED)
        intentFilter.addAction(AppIntent.ACTION_DISPLAY_MESSAGE)
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, intentFilter)

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    override fun onResume() {
        super.onResume()

        if (restartActivity) {
            recreate()
            restartActivity = false
        }
    }

    override fun onPause() {
        super.onPause()

        /* Dismiss dialogs. */

        whatsNewDialog?.let {
            it.dismiss()
            whatsNewDialog = null
        }

        promptDialog?.let {
            it.dismiss()
            promptDialog = null
        }

    }

    protected fun displayWhatsNewDialog() {
        whatsNewDialog?.dismiss()

        val dialog = WhatsNewDialog.create(this)
        dialog.setOnDismissListener { _ -> whatsNewDialog = null }
        dialog.show()

        whatsNewDialog = dialog
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    private fun setupLayoutDirection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val setting = AppPreferences.layoutDirection(this)

            val layoutDirection = when (setting) {
                getString(R.string.pref_value_layout_direction_ltr) -> View.LAYOUT_DIRECTION_LTR
                getString(R.string.pref_value_layout_direction_rtl) -> View.LAYOUT_DIRECTION_RTL
                else -> View.LAYOUT_DIRECTION_LOCALE
            }

            window.decorView.layoutDirection = layoutDirection
        }
    }

    private fun setupTheme() {
        /*
         * Set theme - color scheme.
         */
        val colorScheme = AppPreferences.colorScheme(this)

        when (colorScheme) {
            getString(R.string.pref_value_color_scheme_dark)  -> setTheme(R.style.AppDarkTheme_Dark)
            getString(R.string.pref_value_color_scheme_black) -> setTheme(R.style.AppDarkTheme_Black)
            else -> setTheme(R.style.AppLightTheme_Light)
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


    private var runAfterPermissionGrant: Runnable? = null

    fun runWithPermission(usage: AppPermissions.Usage, runnable: Runnable) {
        runAfterPermissionGrant = runnable

        /* Check for permission. */
        val isGranted = AppPermissions.isGrantedOrRequest(this, usage)

        if (isGranted) {
            runAfterPermissionGrant?.run()
            runAfterPermissionGrant = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            in AppPermissions.Usage.values().map { it.ordinal } -> {
                /* If request is cancelled, the result arrays are empty. */
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runAfterPermissionGrant?.let {
                        it.run()
                        runAfterPermissionGrant = null
                    }
                }
            }
        }
    }


    fun popBackStackAndCloseKeyboard() {
        supportFragmentManager.popBackStack()
        ActivityUtils.closeSoftKeyboard(this)
    }


    fun promptUser(title: Int, message: String, positiveAction: Runnable) {
        promptDialog?.dismiss()

        promptDialog = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, { _, _ -> positiveAction.run() })
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    companion object {
        private val TAG = CommonActivity::class.java.name

        private val PREFS_REQUIRE_IMMEDIATE_ACTIVITY_RECREATE = listOf(
                R.string.pref_key_font_size,
                R.string.pref_key_color_scheme,
                R.string.pref_key_layout_direction
        )
    }
}

package com.orgzly.android.ui

import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.sync.AutoSync
import com.orgzly.android.ui.dialogs.WhatsNewDialog
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import java.io.File
import java.util.*
import javax.inject.Inject


/**
 * Inherited by every activity in the app.
 */
abstract class CommonActivity : AppCompatActivity() {

    /* Dialogs to be dismissed onPause. */
    private var whatsNewDialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null

    /* Any dialog displayed from activities. */
    var alertDialog: AlertDialog? = null

    /* Actions. */
    private var restartActivity = false
    @JvmField protected var clearFragmentBackstack = false

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var autoSync: AutoSync

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Received broadcast: $intent")

            when (intent.action) {
                AppIntent.ACTION_BOOK_IMPORTED ->
                    showSnackbar(R.string.notebook_imported)

                AppIntent.ACTION_DB_CLEARED -> {
                    clearFragmentBackstack = true
                }

                AppIntent.ACTION_UPDATING_NOTES_STARTED -> {
                    progressDialog?.dismiss()
                    progressDialog = progressDialogBuilder(R.string.updating_notes).show()
                }

                AppIntent.ACTION_UPDATING_NOTES_ENDED ->
                    progressDialog?.dismiss()

                AppIntent.ACTION_SHOW_SNACKBAR ->
                    showSnackbar(intent.getStringExtra(AppIntent.EXTRA_MESSAGE))
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


    override fun onBackPressed() {
        super.onBackPressed()
        AppSnackbar.dismiss()
    }

    var runOnTouchEvent: Runnable? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val consumed = super.dispatchTouchEvent(ev)

        if (ev.action == MotionEvent.ACTION_UP) {
            AppSnackbar.dismiss()

        } else if (ev.action == MotionEvent.ACTION_DOWN) {
            runOnTouchEvent?.run()
        }

        return consumed
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(baseContext(newBase))
    }

    private fun baseContext(newBase: Context): Context {
        var context = newBase

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val config = Configuration(newBase.resources.configuration)

            if (AppPreferences.ignoreSystemLocale(context)) {
                config.setLocale(Locale.US)
            } else {
                config.setLocale(Locale.getDefault())
            }

            context = context.createConfigurationContext(config)
        }

        return context
    }

    /**
     * Set theme and styles.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState)

        setupTheme()

        // Required to immediately change layout direction after locale change
        window.decorView.layoutDirection = baseContext.resources.configuration.layoutDirection

        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter()
        intentFilter.addAction(AppIntent.ACTION_BOOK_IMPORTED)
        intentFilter.addAction(AppIntent.ACTION_DB_CLEARED)
        intentFilter.addAction(AppIntent.ACTION_UPDATING_NOTES_STARTED)
        intentFilter.addAction(AppIntent.ACTION_UPDATING_NOTES_ENDED)
        intentFilter.addAction(AppIntent.ACTION_SHOW_SNACKBAR)
        LocalBroadcastManager.getInstance(this).registerReceiver(actionReceiver, intentFilter)

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    override fun onResume() {
        super.onResume()

        if (restartActivity) {
            Handler().post(::recreate)

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

        progressDialog?.let {
            it.dismiss()
            progressDialog = null
        }

        alertDialog?.let {
            it.dismiss()
            alertDialog = null
        }
    }

    protected fun displayWhatsNewDialog() {
        whatsNewDialog?.dismiss()

        whatsNewDialog = WhatsNewDialog.create(this).apply {
            setOnDismissListener {
                whatsNewDialog = null
            }

            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(actionReceiver)

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    private fun setupTheme() {
        setColorScheme()
        applyFontStyle()
    }

    private fun setColorScheme() {
        when (AppPreferences.colorTheme(this)) {
            "light" ->
                setLightScheme()

            "dark" ->
                setDarkScheme()

            else -> { // "system"
                if ("day" == theme.resources.getString(R.string.day_night)) {
                    setLightScheme()
                } else {
                    setDarkScheme()
                }
            }
        }
    }

    private fun setLightScheme() {
        when (AppPreferences.lightColorScheme(this)) {
            "dynamic" -> setTheme(R.style.AppLightTheme)
            "light" -> setTheme(R.style.AppLightTheme_Light)
        }
    }

    private fun setDarkScheme() {
        when (AppPreferences.darkColorScheme(this)) {
            "dynamic" -> setTheme(R.style.AppDarkTheme)
            "dark" -> setTheme(R.style.AppDarkTheme_Dark)
            "black" -> setTheme(R.style.AppDarkTheme_Black)
        }
    }

    private fun applyFontStyle() {
        when (AppPreferences.fontSize(this)) {
            getString(R.string.pref_value_font_size_large) ->
                theme.applyStyle(R.style.FontSize_Large, true)

            getString(R.string.pref_value_font_size_small) ->
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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

    fun progressDialogBuilder(title: Int, message: String? = null): MaterialAlertDialogBuilder {
        val view = View.inflate(this, R.layout.dialog_progress_bar, null)

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)

        if (message != null) {
            builder.setMessage(message)
        }

        return builder
    }

    // TODO: Move these to to main activity
    fun openFileIfExists(file: File) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, file)

        if (file.exists()) {
            runWithPermission(
                AppPermissions.Usage.EXTERNAL_FILES_ACCESS,
                Runnable {
                    try {
                        openFile(file)
                    } catch (e: Exception) {
                        showSnackbar(getString(
                            R.string.failed_to_open_linked_file_with_reason,
                            e.localizedMessage))
                    }
                })
        } else {
            showSnackbar(getString(R.string.file_does_not_exist, file.canonicalFile))
        }
    }

    private fun openFile(file: File) {
        val contentUri = FileProvider.getUriForFile(
            this, BuildConfig.APPLICATION_ID + ".fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW, contentUri)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Added for support on API 16
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Try to start an activity for opening the file
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showSnackbar(R.string.external_file_no_app_found)
        }
    }

    companion object {
        private val TAG = CommonActivity::class.java.name

        private val PREFS_REQUIRE_IMMEDIATE_ACTIVITY_RECREATE = listOf(
            R.string.pref_key_font_size,
            R.string.pref_key_color_theme,
            R.string.pref_key_light_color_scheme,
            R.string.pref_key_dark_color_scheme,
            R.string.pref_key_ignore_system_locale
        )
    }
}

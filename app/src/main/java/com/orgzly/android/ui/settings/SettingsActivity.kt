package com.orgzly.android.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ActionService
import com.orgzly.android.AppIntent
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.settings.SettingsFragment.SettingsFragmentListener
import com.orgzly.android.util.LogUtils


class SettingsActivity : CommonActivity(), SettingsFragmentListener {
    override fun onWhatsNewDisplayRequest() {
        displayWhatsNewDialog()
    }

    override fun onNotesUpdateRequest(action: String) {
        AlertDialog.Builder(this)
                .setTitle(R.string.notes_update_needed_dialog_title)
                .setMessage(R.string.notes_update_needed_dialog_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    ActionService.enqueueWork(this, action)
                }
                .setNegativeButton(R.string.not_now, null)
                .show()
    }

    /**
     * Wipe database, after prompting user for confirmation.
     */
    override fun onDatabaseClearRequest() {
        AlertDialog.Builder(this)
                .setTitle(R.string.clear_database)
                .setMessage(R.string.clear_database_dialog_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    ActionService.enqueueWork(this, AppIntent.ACTION_CLEAR_DATABASE)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onGettingStartedNotebookReloadRequest() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        ActionService.enqueueWork(this, AppIntent.ACTION_IMPORT_GETTING_STARTED_NOTEBOOK)
    }

    override fun onPreferenceScreen(resource: String) {
        val fragment = SettingsFragment.getInstance(resource)

        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit, R.anim.fragment_enter, R.anim.fragment_exit)
                .addToBackStack(null)
                .replace(R.id.activity_settings_container, fragment)
                .commit()
    }

    override fun onTitleChange(title: CharSequence?) {
        setTitle(title ?: getText(R.string.settings))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setupActionBar(R.string.settings)

        if (savedInstanceState == null) {
            val fragment = SettingsFragment.getInstance()

            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.activity_settings_container, fragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun recreateActivityForSettingsChange() {
        recreate()
    }

    companion object {
        val TAG: String = SettingsActivity::class.java.name
    }
}
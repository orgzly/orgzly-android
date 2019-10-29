package com.orgzly.android.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.settings.SettingsFragment.Listener
import com.orgzly.android.usecase.BookImportGettingStarted
import com.orgzly.android.usecase.DatabaseClear
import com.orgzly.android.usecase.UseCase
import com.orgzly.android.usecase.UseCaseRunner


class SettingsActivity : CommonActivity(), Listener {
    override fun onWhatsNewDisplayRequest() {
        displayWhatsNewDialog()
    }

    override fun onNotesUpdateRequest(action: UseCase) {
        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.notes_update_needed_dialog_title)
                .setMessage(R.string.notes_update_needed_dialog_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    UseCaseRunner.enqueue(action)
                }
                .setNegativeButton(R.string.not_now, null)
                .show()
    }

    /**
     * Wipe database, after prompting user for confirmation.
     */
    override fun onDatabaseClearRequest() {
        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.clear_database)
                .setMessage(R.string.clear_database_dialog_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    App.EXECUTORS.diskIO().execute {
                        UseCaseRunner.run(DatabaseClear())

                        App.EXECUTORS.mainThread().execute {
                            showSnackbar(R.string.clear_database_performed)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onGettingStartedNotebookReloadRequest() {
        UseCaseRunner.enqueue(BookImportGettingStarted())
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
        supportActionBar?.title = title ?: getText(R.string.settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
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
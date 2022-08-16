package com.orgzly.android.ui.settings

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.settings.SettingsFragment.Listener
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.usecase.*
import com.orgzly.databinding.ActivitySettingsBinding

class SettingsActivity : CommonActivity(), Listener {
    private lateinit var binding: ActivitySettingsBinding

    override fun onWhatsNewDisplayRequest() {
        displayWhatsNewDialog()
    }

    override fun onNotesUpdateRequest(action: UseCase) {
        alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notes_update_needed_dialog_title)
                .setMessage(R.string.notes_update_needed_dialog_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    UseCaseWorker.schedule(this, action)
                }
                .setNegativeButton(R.string.not_now, null)
                .show()
    }

    /**
     * Wipe database, after prompting user for confirmation.
     */
    override fun onDatabaseClearRequest() {
        alertDialog = MaterialAlertDialogBuilder(this)
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
        UseCaseWorker.schedule(this, BookImportGettingStarted())
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
        binding.topToolbar.title = title ?: getText(R.string.settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (savedInstanceState == null) {
            val fragment = SettingsFragment.getInstance()

            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.activity_settings_container, fragment)
                    .commit()
        }

        binding.topToolbar.run {
            setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    override fun recreateActivityForSettingsChange() {
        recreate()
    }

    companion object {
        val TAG: String = SettingsActivity::class.java.name
    }
}
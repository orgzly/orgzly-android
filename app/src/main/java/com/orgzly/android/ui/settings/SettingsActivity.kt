package com.orgzly.android.ui.settings

import android.os.Bundle
import android.view.MenuItem
import com.orgzly.R
import com.orgzly.android.ui.CommonActivity

class SettingsActivity : CommonActivity(), NewSettingsFragment.NewSettingsFragmentListener {
    override fun onStateKeywordsPreferenceChanged() {
        TODO("not implemented")
    }

    override fun onDatabaseClearRequest() {
        TODO("not implemented")
    }

    override fun onGettingStartedNotebookReloadRequest() {
        TODO("not implemented")
    }

    override fun onWhatsNewDisplayRequest() {
        TODO("not implemented")
    }

    override fun onPreferenceScreen(resource: String) {
        val fragment = NewSettingsFragment.getInstance(resource)

        supportFragmentManager
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.activity_settings_container, fragment)
                .commit()
    }

    companion object {
        val TAG: String = CommonActivity::class.java.name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // TODO Set action bar and status bar colors

        if (savedInstanceState == null) {
            val fragment = NewSettingsFragment.getInstance()

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
}
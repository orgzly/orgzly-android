package com.orgzly.android.ui.repo.webdav

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.orgzly.R
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.WebdavRepo.Companion.PASSWORD_PREF_KEY
import com.orgzly.android.repos.WebdavRepo.Companion.USERNAME_PREF_KEY
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.databinding.ActivityRepoWebdavBinding
import javax.inject.Inject

class WebdavRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoWebdavBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var viewModel: WebdavRepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repo_webdav)

        setupActionBar(R.string.webdav)

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        if (repoId != 0L) {
            val prefs = RepoPreferences.fromId(this, repoId, dataRepository)

            binding.activityRepoWebdavUrl.setText(prefs.repoUri.toString())

            val username = prefs.getStringValue(USERNAME_PREF_KEY, "")
            binding.activityRepoWebdavUsername.setText(username)

            val password = prefs.getStringValue(PASSWORD_PREF_KEY, "")
            binding.activityRepoWebdavPassword.setText(password)
        }

        binding.activityRepoWebdavTestButton.setOnClickListener {
            testConnection()
        }

        val factory = WebdavRepoViewModelFactory.getInstance(dataRepository, repoId)

        viewModel = ViewModelProviders.of(this, factory).get(WebdavRepoViewModel::class.java)

        viewModel.finishEvent.observeSingle(this, Observer {
            val username = getUsername()
            val password = getPassword()

            // TODO: Move to RepoPreferences
            val editor: SharedPreferences.Editor = RepoPreferences.fromId(this, viewModel.repoId, dataRepository).repoPreferences.edit()
            editor.putString(USERNAME_PREF_KEY, username)
            editor.putString(PASSWORD_PREF_KEY, password)
            editor.apply()

            finish()
        })

        viewModel.alreadyExistsEvent.observeSingle(this, Observer {
            showSnackbar(R.string.repository_url_already_exists)
        })


        viewModel.errorEvent.observeSingle(this, Observer { error ->
            if (error != null) {
                showSnackbar((error.cause ?: error).localizedMessage)
            }
        })

        viewModel.connectionTestStatus.observe(this, Observer {
            binding.activityRepoWebdavTestResult.text =
                    when (it) {
                        is WebdavRepoViewModel.ConnectionResult.InProgress -> {
                            binding.activityRepoWebdavTestButton.isEnabled = false

                            getString(it.msg)
                        }

                        is WebdavRepoViewModel.ConnectionResult.Success -> {
                            binding.activityRepoWebdavTestButton.isEnabled = true

                            val bookCountMsg = resources.getQuantityString(
                                    R.plurals.found_number_of_notebooks, it.bookCount, it.bookCount)

                            "${getString(R.string.connection_successful)}\n$bookCountMsg"
                        }

                        is WebdavRepoViewModel.ConnectionResult.Error -> {
                            binding.activityRepoWebdavTestButton.isEnabled = true

                            when (it.msg) {
                                is Int -> getString(it.msg)
                                is String -> it.msg
                                else -> null
                            }
                        }
                    }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.done, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                saveAndFinish()
                true
            }

            android.R.id.home -> {
                finish()
                true
            }

            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun saveAndFinish() {
        if (isInputValid()) {
            val uriString = getUrl()

            viewModel.saveRepo(uriString)
        }
    }

    private fun getUrl(): String {
        return binding.activityRepoWebdavUrl.text.toString().trim { it <= ' ' }
    }

    private fun getUsername(): String {
        return binding.activityRepoWebdavUsername.text.toString().trim { it <= ' ' }
    }

    private fun getPassword(): String {
        return binding.activityRepoWebdavPassword.text.toString().trim { it <= ' ' }
    }

    private fun isInputValid(): Boolean {
        val url = getUrl()
        val username = getUsername()
        val password = getPassword()

        binding.activityRepoWebdavUrlLayout.error = when {
            TextUtils.isEmpty(url) -> getString(R.string.can_not_be_empty)
            !WEB_DAV_SCHEME_REGEX.matches(url) -> getString(R.string.invalid_url)
            else -> null
        }

        binding.activityRepoWebdavUsernameLayout.error = when {
            TextUtils.isEmpty(username) -> getString(R.string.can_not_be_empty)
            else -> null
        }

        binding.activityRepoWebdavPasswordLayout.error = when {
            TextUtils.isEmpty(password) -> getString(R.string.can_not_be_empty)
            else -> null
        }

        return binding.activityRepoWebdavUrlLayout.error == null
                && binding.activityRepoWebdavUsernameLayout.error == null
                && binding.activityRepoWebdavPasswordLayout.error == null
    }

    private fun testConnection() {
        ActivityUtils.closeSoftKeyboard(this)

        if (!isInputValid()) {
            return
        }

        val uriString = getUrl()
        val username = getUsername()
        val password = getPassword()

        viewModel.testConnection(uriString, username, password)
    }

    companion object {
        private const val ARG_REPO_ID = "repo_id"

        private val WEB_DAV_SCHEME_REGEX = Regex("^(webdav|dav|http)s?://.+\$")

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setClass(activity, WebdavRepoActivity::class.java)
                    .putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }
}

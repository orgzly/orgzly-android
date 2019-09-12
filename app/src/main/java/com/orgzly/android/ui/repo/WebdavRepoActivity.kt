package com.orgzly.android.ui.repo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.orgzly.R
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.WebdavRepo
import com.orgzly.android.repos.WebdavRepo.Companion.PASSWORD_PREF_KEY
import com.orgzly.android.repos.WebdavRepo.Companion.USERNAME_PREF_KEY
import com.orgzly.android.ui.CommonActivity
import com.orgzly.databinding.ActivityRepoWebdavBinding
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import javax.inject.Inject

class WebdavRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoWebdavBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private var repoId: Long = 0

    private var webdavUrlRegex = Regex("^(webdav|http)(s)?://.+\$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repo_webdav)

        setupActionBar(R.string.webdav)

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        if (repoId != 0L) {
            val prefs = RepoPreferences.fromId(this, repoId, dataRepository)
            binding.activityRepoWebdavUrl.setText(prefs.repoUri.toString())
            val username = prefs.getStringValue(USERNAME_PREF_KEY, "")
            binding.activityRepoWebdavUsername.setText(username)
            val password = prefs.getStringValue(PASSWORD_PREF_KEY, "")
            binding.activityRepoWebdavPassword.setText(password)
        }

        binding.activityRepoWebdavTestButton.setOnClickListener { testRepo() }
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
        if (!isInputValid()) {
            return
        }

        val uriString = getUriString()
        val username = binding.activityRepoWebdavUsername.text.toString().trim { it <= ' ' }
        val password = binding.activityRepoWebdavPassword.text.toString().trim { it <= ' ' }

        repoId = if (repoId == 0L) {
            dataRepository.createRepo(uriString)
        } else {
            dataRepository.updateRepo(repoId, uriString)
            dataRepository.getRepo(uriString)!!.id
        }

        val editor: SharedPreferences.Editor = RepoPreferences.fromId(this, repoId, dataRepository).repoPreferences.edit()
        editor.putString(USERNAME_PREF_KEY, username)
        editor.putString(PASSWORD_PREF_KEY, password)
        editor.apply()

        finish()

    }

    private fun getUriString(): String {
        return binding.activityRepoWebdavUrl.text.toString().trim { it <= ' ' }.let {
            if (it.startsWith("http")) {
                it.replaceFirst("http", "webdav")
            } else {
                it
            }
        }
    }

    private fun isInputValid(): Boolean {
        val url = binding.activityRepoWebdavUrl.text.toString().trim()
        val username = binding.activityRepoWebdavUsername.text.toString().trim()
        val password = binding.activityRepoWebdavPassword.text.toString().trim()

        binding.activityRepoWebdavUrlLayout.error = when {
            TextUtils.isEmpty(url) -> getString(R.string.can_not_be_empty)
            !webdavUrlRegex.matches(url) -> getString(R.string.invalid_url)
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

    private fun testRepo() {
        if (!isInputValid()) {
            return
        }

        val uriString = getUriString()
        val username = binding.activityRepoWebdavUsername.text.toString().trim { it <= ' ' }
        val password = binding.activityRepoWebdavPassword.text.toString().trim { it <= ' ' }

        binding.activityRepoWebdavTestButton.isEnabled = false

        RepoTester().execute(uriString, username, password)
    }

    private fun setTestResultText(message: String) {
        handler.post {
            binding.activityRepoWebdavTestResult.text = message
        }
    }

    private inner class RepoTester : AsyncTask<String, Void, Void>() {
        override fun doInBackground(vararg params: String?): Void? {

            try {
                binding.activityRepoWebdavTestResult.text = ""

                val uriString = params[0]
                val username = params[1]
                val password = params[2]

                val uri = Uri.parse(uriString)

                val bookCount = WebdavRepo(uri, username, password).run {
                    books.size
                }

                val bookCountMsg = resources.getQuantityString(
                        R.plurals.found_number_of_notebooks, bookCount, bookCount)

                setTestResultText(getString(R.string.connection_successful) + "\n" + bookCountMsg)

            } catch (e: Exception) {
                when (e) {
                    is SardineException -> {
                        when (e.statusCode) {
                            401 -> setTestResultText(
                                    getString(R.string.webdav_test_error_auth))

                            else -> setTestResultText(
                                    arrayOf(e.statusCode, ":", e.responsePhrase).joinToString(" "))
                        }

                    }
                    else -> setTestResultText(
                            e.message ?: getString(R.string.webdav_test_error_unknown))

                }
            } finally {
                // Avoiding bug described here: https://stackoverflow.com/a/10687660
                handler.post {
                    binding.activityRepoWebdavTestButton.isEnabled = true
                }
                return null
            }
        }
    }

    companion object {
        private const val ARG_REPO_ID = "repo_id"
        private val handler = Handler()

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

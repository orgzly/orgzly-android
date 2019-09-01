package com.orgzly.android.ui.repo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.orgzly.R
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.WebdavRepo.Companion.PASSWORD_PREF_KEY
import com.orgzly.android.repos.WebdavRepo.Companion.USERNAME_PREF_KEY
import com.orgzly.android.ui.CommonActivity
import com.orgzly.databinding.ActivityRepoWebdavBinding
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
        val uriString = binding.activityRepoWebdavUrl.text.toString().trim { it <= ' ' }.let {
            if (it.startsWith("http")) {
                it.replaceFirst("http", "webdav")
            }
            it
        }
        val username = binding.activityRepoWebdavUsername.text.toString().trim { it <= ' ' }
        val password = binding.activityRepoWebdavPassword.text.toString().trim { it <= ' ' }

        val urlError = getWebdavUrlError(uriString)
        binding.activityRepoWebdavUrlLayout.error = urlError
        if (urlError != null) {
            return
        }

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

    private fun getWebdavUrlError(url: String): String? {
        return when {
            TextUtils.isEmpty(url) -> getString(R.string.can_not_be_empty)
            !webdavUrlRegex.matches(url) -> getString(R.string.invalid_repo_url)
            else -> null
        }
    }

    companion object {
        private const val ARG_REPO_ID = "repo_id"

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

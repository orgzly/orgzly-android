package com.orgzly.android.ui.repo.webdav

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.RepoType
import com.orgzly.android.repos.WebdavRepo.Companion.CERTIFICATES_PREF_KEY
import com.orgzly.android.repos.WebdavRepo.Companion.PASSWORD_PREF_KEY
import com.orgzly.android.repos.WebdavRepo.Companion.USERNAME_PREF_KEY
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.ui.util.KeyboardUtils
import com.orgzly.android.util.UriUtils
import com.orgzly.databinding.ActivityRepoWebdavBinding
import com.orgzly.databinding.DialogCertificatesBinding
import javax.inject.Inject

class WebdavRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoWebdavBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var viewModel: WebdavRepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = ActivityRepoWebdavBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.activityRepoWebdavCertificates.setOnClickListener {
            editCertificates()
        }

        binding.activityRepoWebdavTestButton.setOnClickListener {
            testConnection()
        }

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)
        val factory = WebdavRepoViewModelFactory.getInstance(dataRepository, repoId)

        viewModel = ViewModelProvider(this, factory).get(WebdavRepoViewModel::class.java)

        viewModel.finishEvent.observeSingle(this, Observer {
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

        viewModel.certificates.observe(this, Observer { str ->
            binding.activityRepoWebdavCertificates.text = getString(if (str.isNullOrEmpty()) {
                R.string.add_trusted_certificates_optional
            } else {
                R.string.edit_trusted_certificates
            })
        })

        if (viewModel.repoId != 0L) { // Editing existing
            viewModel.loadRepoProperties()?.let { repoWithProps ->
                binding.activityRepoWebdavUrl.setText(repoWithProps.repo.url)

                binding.activityRepoWebdavUsername.setText(repoWithProps.props[USERNAME_PREF_KEY])
                binding.activityRepoWebdavPassword.setText(repoWithProps.props[PASSWORD_PREF_KEY])
                viewModel.certificates.value = repoWithProps.props[CERTIFICATES_PREF_KEY]
            }
        }

        binding.topToolbar.run {
            setNavigationOnClickListener {
                finish()
            }
        }

        binding.fab.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun saveAndFinish() {
        if (isInputValid()) {
            val uriString = getUrl()
            val username = getUsername()
            val password = getPassword()
            val certificates = getCertificates()

            val props = mutableMapOf(
                    USERNAME_PREF_KEY to username,
                    PASSWORD_PREF_KEY to password)

            if (certificates != null) {
                props[CERTIFICATES_PREF_KEY] = certificates
            }

            if (UriUtils.isUrlSecure(uriString)) {
                viewModel.saveRepo(RepoType.WEBDAV, uriString, props)

            } else {
                // Warn about clear-text traffic
                alertDialog = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.cleartext_traffic)
                        .setMessage(R.string.cleartext_traffic_message)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.saveRepo(RepoType.WEBDAV, uriString, props)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            }
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

    private fun getCertificates(): String? {
        return viewModel.certificates.value
    }

    private fun isInputValid(): Boolean {
        val url = getUrl()
        val username = getUsername()
        val password = getPassword()

        binding.activityRepoWebdavUrlLayout.error = when {
            TextUtils.isEmpty(url) -> getString(R.string.can_not_be_empty)
            !WEB_DAV_SCHEME_REGEX.matches(url) -> getString(R.string.invalid_url)
            UriUtils.containsUser(url) -> getString(R.string.credentials_in_url_not_supported)
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

    private fun editCertificates() {
        val dialogBinding = DialogCertificatesBinding.inflate(layoutInflater).apply {
            certificates.setText(viewModel.certificates.value)
        }

        alertDialog = MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.trusted_certificates))
                .setPositiveButton(R.string.set) { _, _ ->
                    viewModel.certificates.value = dialogBinding.certificates.text.toString()
                }
                .setNeutralButton(R.string.clear) { _, _ ->
                    viewModel.certificates.value = null
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    // Cancel
                }
                .setView(dialogBinding.root)
                .show()
                .apply {
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
    }

    private fun testConnection() {
        KeyboardUtils.closeSoftKeyboard(this)

        if (!isInputValid()) {
            return
        }

        val uriString = getUrl()
        val username = getUsername()
        val password = getPassword()
        val certificates = getCertificates()

        viewModel.testConnection(uriString, username, password, certificates)
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

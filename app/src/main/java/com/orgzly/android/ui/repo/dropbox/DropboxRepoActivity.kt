package com.orgzly.android.ui.repo.dropbox

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.repos.DropboxClient
import com.orgzly.android.repos.DropboxRepo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.RepoType
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.repo.RepoViewModel
import com.orgzly.android.ui.repo.RepoViewModelFactory
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.android.util.UriUtils
import com.orgzly.databinding.ActivityRepoDropboxBinding
import javax.inject.Inject


class DropboxRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoDropboxBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var client: DropboxClient

    private lateinit var viewModel: RepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repo_dropbox)

        setupActionBar(R.string.dropbox)

        /* Dropbox link / unlink button. */
        binding.activityRepoDropboxLinkButton.setOnClickListener {
            if (isDropboxLinked()) {
                toggleLinkAfterConfirmation()
            } else {
                toggleLink()
            }
        }

        binding.activityRepoDropboxLinkButton.setOnLongClickListener {
            editAccessToken()
            true
        }

        // Not working when done in XML
        binding.activityRepoDropboxDirectory.apply {
            setHorizontallyScrolling(false)

            maxLines = 3

            setOnEditorActionListener { _, _, _ ->
                saveAndFinish()
                finish()
                true
            }
        }

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        val factory = RepoViewModelFactory.getInstance(dataRepository, repoId)

        viewModel = ViewModelProvider(this, factory).get(RepoViewModel::class.java)

        if (viewModel.repoId != 0L) { // Editing existing
            viewModel.loadRepoProperties()?.let { repoWithProps ->
                val path = Uri.parse(repoWithProps.repo.url).path

                binding.activityRepoDropboxDirectory.setText(path)
            }
        }

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

        MiscUtils.clearErrorOnTextChange(
                binding.activityRepoDropboxDirectory,
                binding.activityRepoDropboxDirectoryInputLayout)

        ActivityUtils.openSoftKeyboardWithDelay(this, binding.activityRepoDropboxDirectory)

        client = DropboxClient(applicationContext, repoId)
    }

    private fun editAccessToken() {
        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.dialog_simple_one_liner, null, false)

        val editView = view.findViewById<EditText>(R.id.dialog_input).apply {
            setSelectAllOnFocus(true)

            setHint(R.string.access_token)

            client.token?.let {
                setText(it)
            }
        }

        alertDialog = AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.access_token)
                .setPositiveButton(R.string.set) { _, _ ->
                    editView.text.toString().let { value ->
                        if (TextUtils.isEmpty(value)) {
                            client.unlink()
                        } else {
                            client.setToken(value)
                        }
                    }
                    updateDropboxLinkUnlinkButton()
                }
                .setNeutralButton(R.string.clear) { _, _ ->
                    client.unlink()
                    updateDropboxLinkUnlinkButton()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .create().apply {
                    setOnShowListener {
                        ActivityUtils.openSoftKeyboard(this@DropboxRepoActivity, editView)
                    }

                    show()
                }
    }

    public override fun onResume() {
        super.onResume()

        dropboxCompleteAuthentication()

        updateDropboxLinkUnlinkButton()
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
        val directory = binding.activityRepoDropboxDirectory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(directory)) {
            binding.activityRepoDropboxDirectoryInputLayout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            binding.activityRepoDropboxDirectoryInputLayout.error = null
        }

        val url = UriUtils.uriFromPath(DropboxRepo.SCHEME, directory).toString()

        val repo = try {
            viewModel.validate(RepoType.DROPBOX, url)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.activityRepoDropboxDirectoryInputLayout.error =
                    getString(R.string.repository_not_valid_with_reason, e.message)
            return
        }

        viewModel.saveRepo(RepoType.DROPBOX, repo.uri.toString())
    }

    private fun toggleLinkAfterConfirmation() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                toggleLink()
            }
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.confirm_unlinking_from_dropbox_title)
                .setMessage(R.string.confirm_unlinking_from_dropbox_message)
                .setPositiveButton(R.string.unlink, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show()
    }

    private fun toggleLink() {
        if (onDropboxLinkToggleRequest()) { // Unlinked
            updateDropboxLinkUnlinkButton()
        } // Else - Linking process started - button should stay the same.
    }

    /**
     * Toggle Dropbox link. Link to Dropbox or unlink from it, depending on current state.
     *
     * @return true if there was a change (Dropbox has been unlinked).
     */
    private fun onDropboxLinkToggleRequest(): Boolean {
        return if (client.isLinked) {
            client.unlink()
            showSnackbar(R.string.message_dropbox_unlinked)
            true

        } else {
            client.beginAuthentication(this)
            false
        }
    }

    /**
     * Complete Dropbox linking.
     * After starting Dropbox authentication, user will return to activity.
     * We need to finish the process of authentication.
     */
    private fun dropboxCompleteAuthentication() {
        if (!client.isLinked) {
            if (client.finishAuthentication()) {
                showSnackbar(R.string.message_dropbox_linked)
            }
        }
    }

    private fun updateDropboxLinkUnlinkButton() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val resources = styledAttributes(R.styleable.Icons) { typedArray ->
            if (isDropboxLinked()) {
                Pair(
                        getString(R.string.repo_dropbox_button_linked),
                        typedArray.getResourceId(R.styleable.Icons_oic_dropbox_linked, 0))
            } else {
                Pair(
                        getString(R.string.repo_dropbox_button_not_linked),
                        typedArray.getResourceId(R.styleable.Icons_oic_dropbox_not_linked, 0))
            }
        }

        binding.activityRepoDropboxLinkButton.text = resources.first

        if (resources.second != 0) {
            binding.activityRepoDropboxIcon.setImageResource(resources.second)
        }
    }

    private fun isDropboxLinked(): Boolean {
        return client.isLinked
    }

    companion object {
        private val TAG: String = DropboxRepoActivity::class.java.name

        private const val ARG_REPO_ID = "repo_id"

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setClass(activity, DropboxRepoActivity::class.java)
                    .putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }
}

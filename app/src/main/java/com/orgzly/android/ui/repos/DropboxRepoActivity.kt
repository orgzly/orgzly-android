package com.orgzly.android.ui.repos

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.provider.clients.ReposClient
import com.orgzly.android.repos.DropboxClient
import com.orgzly.android.repos.DropboxRepo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.android.util.UriUtils
import kotlinx.android.synthetic.main.activity_repo_dropbox.*

class DropboxRepoActivity : RepoActivity() {

    private lateinit var client: DropboxClient

    private var repoId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_repo_dropbox)

        setupActionBar(R.string.dropbox)

        /* Dropbox link / unlink button. */
        activity_repo_dropbox_link_button.setOnClickListener {
            if (isDropboxLinked()) {
                toggleLinkAfterConfirmation()
            } else {
                toogleLink()
            }
        }

        // Not working when done in XML
        activity_repo_dropbox_directory.setHorizontallyScrolling(false)
        activity_repo_dropbox_directory.maxLines = 3

        activity_repo_dropbox_directory.setOnEditorActionListener { _, _, _ ->
            saveAndFinish()
            finish()
            true
        }

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        // Set directory value for existing repository being edited
        if (repoId != 0L) {
            val repoUri = Uri.parse(ReposClient.getUrl(this, repoId))
            activity_repo_dropbox_directory.setText(repoUri.path)
        }

        MiscUtils.clearErrorOnTextChange(activity_repo_dropbox_directory, activity_repo_dropbox_directory_input_layout)

        // Open keyboard after activity has been fully displayed
        Handler().postDelayed({ ActivityUtils.openSoftKeyboard(this, activity_repo_dropbox_directory) }, 150)

        client = DropboxClient(applicationContext)
    }

    public override fun onResume() {
        super.onResume()

        dropboxCompleteAuthentication()

        updateDropboxLinkUnlinkButton()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.close_done, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                saveAndFinish()
                true
            }

            R.id.close, android.R.id.home -> {
                finish()
                true
            }

            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun saveAndFinish() {
        val directory = activity_repo_dropbox_directory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(directory)) {
            activity_repo_dropbox_directory_input_layout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            activity_repo_dropbox_directory_input_layout.error = null
        }

        val uri = UriUtils.uriFromPath(DropboxRepo.SCHEME, directory)

        val repo = RepoFactory.getFromUri(this, uri)

        if (repo == null) {
            activity_repo_dropbox_directory_input_layout.error = getString(R.string.invalid_repo_url, uri)
            return
        }

        if (repoId != 0L) { // Update existing repository
            updateRepoUrl(repoId, repo.uri.toString())

        } else { // Add new repository
            addRepoUrl(repo.uri.toString())
        }

        finish()
    }

    private fun toggleLinkAfterConfirmation() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                toogleLink()
            }
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.confirm_unlinking_from_dropbox_title)
                .setMessage(R.string.confirm_unlinking_from_dropbox_message)
                .setPositiveButton(R.string.unlink, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show()
    }

    private fun toogleLink() {
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

        val typedArray = obtainStyledAttributes(R.styleable.Icons)

        val text: String
        val imageResource: Int

        if (isDropboxLinked()) {
            text = getString(R.string.repo_dropbox_button_linked)
            imageResource = typedArray.getResourceId(R.styleable.Icons_oic_dropbox_linked, 0)
        } else {
            text = getString(R.string.repo_dropbox_button_not_linked)
            imageResource = typedArray.getResourceId(R.styleable.Icons_oic_dropbox_not_linked, 0)
        }

        typedArray.recycle()

        activity_repo_dropbox_link_button.text = text

        if (imageResource != 0) {
            activity_repo_dropbox_icon.setImageResource(imageResource)
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

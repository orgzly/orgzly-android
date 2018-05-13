package com.orgzly.android.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TextInputLayout
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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

class DropboxRepoActivity : RepoActivity() {
    companion object {
        private val TAG: String = DropboxRepoActivity::class.java.name

        private const val ARG_REPO_ID = "repo_id"

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClass(activity, DropboxRepoActivity::class.java)
            intent.putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }

    private lateinit var icon: ImageView
    private lateinit var button: Button
    private lateinit var directoryInputLayout: TextInputLayout
    private lateinit var directory: EditText

    private lateinit var client: DropboxClient

    private var repoId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        setContentView(R.layout.activity_repo_dropbox)

        val myToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setTitle(R.string.dropbox)

        // supportActionBar?.setTitle(R.string.repositories)

        /* Dropbox link / unlink button. */
        button = findViewById(R.id.fragment_repo_dropbox_link_button)
        button.setOnClickListener {
            if (isDropboxLinked()) {
                areYouSureYouWantToUnlink()
            } else {
                toogleLink()
            }
        }

        icon = findViewById(R.id.fragment_repo_dropbox_icon)

        directory = findViewById(R.id.fragment_repo_dropbox_directory)

        // Not working when done in XML
        directory.setHorizontallyScrolling(false)
        directory.maxLines = 3

        directory.setOnEditorActionListener { _, _, _ ->
            save()
            finish()
            true
        }

        // Set directory value for existing repository being edited
        if (repoId != 0L) {
            val repoUri = Uri.parse(ReposClient.getUrl(this, repoId))
            directory.setText(repoUri.path)
        }

        directoryInputLayout = findViewById(R.id.fragment_repo_dropbox_directory_input_layout)

        MiscUtils.clearErrorOnTextChange(directory, directoryInputLayout)

        // Open keyboard after activity has been fully displayed
        Handler().postDelayed({ ActivityUtils.openSoftKeyboard(this, directory) }, 150)

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

    /**
     * Callback for options menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                save()
                finish()
                true
            }

            R.id.close, R.id.home -> {
                finish()
                true
            }

            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun save() {
        val directory = directory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(directory)) {
            directoryInputLayout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            directoryInputLayout.error = null
        }

        val uri = UriUtils.uriFromPath(DropboxRepo.SCHEME, directory)

        val repo = RepoFactory.getFromUri(this, uri)

        if (repo == null) {
            directoryInputLayout.error = getString(R.string.invalid_repo_url, uri)
            return
        }

        if (repoId != 0L) { // Update existing repository
            updateRepoUrl(repoId, repo.uri.toString())

        } else { // Add new repository
            addRepoUrl(repo.uri.toString())
        }
    }

    private fun areYouSureYouWantToUnlink() {
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
            showSimpleSnackbarLong(R.string.message_dropbox_unlinked)
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
                showSimpleSnackbarLong(R.string.message_dropbox_linked)
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

        button.text = text

        if (imageResource != 0) {
            icon.setImageResource(imageResource)
        }
    }

    private fun isDropboxLinked(): Boolean {
        return client.isLinked
    }
}

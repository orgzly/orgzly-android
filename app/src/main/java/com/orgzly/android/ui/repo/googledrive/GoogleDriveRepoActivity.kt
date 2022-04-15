package com.orgzly.android.ui.repo.googledrive

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.repos.GoogleDriveClient
import com.orgzly.android.repos.GoogleDriveRepo
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
import com.orgzly.databinding.ActivityRepoGoogleDriveBinding
import javax.inject.Inject

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;

import com.google.api.services.drive.DriveScopes;

import androidx.annotation.NonNull;

import android.util.Log;
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider

class GoogleDriveRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoGoogleDriveBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var client: GoogleDriveClient

    private val REQUEST_CODE_SIGN_IN = 1

    private lateinit var gsiClient: GoogleSignInClient

    private lateinit var viewModel: RepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repo_google_drive)

        setupActionBar(R.string.google_drive)

        /* Google Drive link / unlink button. */
        binding.activityRepoGoogleDriveLinkButton.setOnClickListener {
            if (isGoogleDriveLinked()) {
                toggleLinkAfterConfirmation()
            } else {
                toggleLink()
            }
        }

        /* TODO: Google Drive create folder button. */
        binding.activityRepoGoogleDriveCreateDirectoryButton.setOnClickListener {
            // TODO need a check to see if client is logged in; or hide the button when they're not
            client.createDirectory()
            showSnackbar("Folder created")
        }

        // No need for editAccessToken() logic

        // Not working when done in XML
        binding.activityRepoGoogleDriveDirectory.apply {
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

                binding.activityRepoGoogleDriveDirectory.setText(path)
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
                binding.activityRepoGoogleDriveDirectory,
                binding.activityRepoGoogleDriveDirectoryInputLayout)

        ActivityUtils.openSoftKeyboardWithDelay(this, binding.activityRepoGoogleDriveDirectory)

        client = GoogleDriveClient(this, repoId)
    }

    // TODO remove
    override fun onActivityResult(requestCode:Int, resultCode:Int, resultData:Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        handleSignInResult(requestCode, resultData)
    }

    // TODO remove
    fun handleSignInResult(requestCode:Int, result:Intent?) {
        if (requestCode == REQUEST_CODE_SIGN_IN)
        {
            GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener({ googleAccount->
                                            Log.d(TAG, "Signed in as " + googleAccount.getEmail())
                                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                                        // Its instantiation is required before handling any onClick actions.
                                        showSnackbar(R.string.message_google_drive_linked)
                })
                .addOnFailureListener({ exception-> Log.d(TAG, "Unable to sign in." + exception) })
        }
    }

    public override fun onResume() {
        super.onResume()

        // There is no need for googleDriveCompleteAuthentication()

        updateGoogleDriveLinkUnlinkButton()
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
        val directory = binding.activityRepoGoogleDriveDirectory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(directory)) {
            binding.activityRepoGoogleDriveDirectoryInputLayout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            binding.activityRepoGoogleDriveDirectoryInputLayout.error = null
        }

        val url = UriUtils.uriFromPath(GoogleDriveRepo.SCHEME, directory).toString()

        val repo = try {
            viewModel.validate(RepoType.GOOGLE_DRIVE, url)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.activityRepoGoogleDriveDirectoryInputLayout.error =
                    getString(R.string.repository_not_valid_with_reason, e.message)
            return
        }

        viewModel.saveRepo(RepoType.GOOGLE_DRIVE, repo.uri.toString())
    }

    private fun toggleLinkAfterConfirmation() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                toggleLink()
            }
        }

        alertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.confirm_unlinking_from_google_drive_title)
                .setMessage(R.string.confirm_unlinking_from_google_drive_message)
                .setPositiveButton(R.string.unlink, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show()
    }

    private fun toggleLink() {
        if (onGoogleDriveLinkToggleRequest()) { // Unlinked
            updateGoogleDriveLinkUnlinkButton()
        } // Else - Linking process started - button should stay the same.
    }

    /**
     * Toggle Google Drive link. Link to Google Drive or unlink from it, depending on current state.
     *
     * @return true if there was a change (Google Drive has been unlinked).
     */
    private fun onGoogleDriveLinkToggleRequest(): Boolean {
        return if (isGoogleDriveLinked()) {
            // note that this is async
            client.unlink(this).addOnCompleteListener {
                showSnackbar(R.string.message_google_drive_unlinked)
            }
            true
        } else {
            intent = client.beginAuthentication(this)
            val resultLauncher = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data.
                    data.
                    showSnackbar(R.string.message_google_drive_linked)
                }
            }
            resultLauncher.launch(intent)
            false
        }
    }

    // no need for googleDriveCompleteAuthentication()

    private fun updateGoogleDriveLinkUnlinkButton() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val resources = styledAttributes(R.styleable.Icons) { typedArray ->
            if (isGoogleDriveLinked()) {
                Pair(
                        getString(R.string.repo_google_drive_button_linked),
                        typedArray.getResourceId(R.styleable.Icons_oic_dropbox_linked, 0))
            } else {
                Pair(
                        getString(R.string.repo_google_drive_button_not_linked),
                        typedArray.getResourceId(R.styleable.Icons_oic_dropbox_not_linked, 0))
            }
        }

        binding.activityRepoGoogleDriveLinkButton.text = resources.first

        if (resources.second != 0) {
            binding.activityRepoGoogleDriveIcon.setImageResource(resources.second)
        }
    }

    private fun isGoogleDriveLinked(): Boolean {
        return client.isLinked
    }

    companion object {
        private val TAG: String = GoogleDriveRepoActivity::class.java.name

        private const val ARG_REPO_ID = "repo_id"

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setClass(activity, GoogleDriveRepoActivity::class.java)
                    .putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }
}

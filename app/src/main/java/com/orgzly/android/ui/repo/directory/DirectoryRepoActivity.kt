package com.orgzly.android.ui.repo.directory

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.repos.ContentRepo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.RepoType
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.repo.BrowserActivity
import com.orgzly.android.ui.repo.RepoViewModel
import com.orgzly.android.ui.repo.RepoViewModelFactory
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils
import com.orgzly.databinding.ActivityRepoDirectoryBinding
import javax.inject.Inject

class DirectoryRepoActivity : CommonActivity() {
    private lateinit var binding: ActivityRepoDirectoryBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var viewModel: RepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repo_directory)

        setupActionBar(R.string.directory)

        // Not working when done in XML
        binding.activityRepoDirectory.apply {
            setHorizontallyScrolling(false)
            maxLines = 3

            setOnEditorActionListener { _, _, _ ->
                saveAndFinish()
                true
            }
        }

        MiscUtils.clearErrorOnTextChange(
                binding.activityRepoDirectory, binding.activityRepoDirectoryInputLayout)

        binding.activityRepoDirectoryBrowseButton.setOnClickListener { startFileBrowser() }

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        val factory = RepoViewModelFactory.getInstance(dataRepository, repoId)

        viewModel = ViewModelProvider(this, factory).get(RepoViewModel::class.java)

        if (viewModel.repoId != 0L) { // Editing existing
            viewModel.loadRepoProperties()?.let { repoWithProps ->
                binding.activityRepoDirectory.setText(repoWithProps.repo.url)
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
    }

    private fun startFileBrowser() {
        var browserStarted = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            /*
             * Apparently some devices do not handle this intent.
             * Fallback to internal browser.
             */
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

                /*
                 * Try to show internal storage by default.
                 * https://stackoverflow.com/a/31334967/2515600
                 *
                 * Stopped using it as some devices would still not show
                 * it *and* would not display the option to do so.
                 */
                // intent.putExtra("android.content.extra.SHOW_ADVANCED", true)

                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION)

                browserStarted = true

            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }

        if (!browserStarted) {
            runWithPermission(AppPermissions.Usage.LOCAL_REPO, Runnable { startLocalFileBrowser() })
        }
    }

    private fun startLocalFileBrowser() {
        val intent = Intent(Intent.ACTION_VIEW).setClass(this, BrowserActivity::class.java)

        if (!TextUtils.isEmpty(binding.activityRepoDirectory.text)) {
            val uri = binding.activityRepoDirectory.text.toString()
            val path = Uri.parse(uri).path
            intent.putExtra(BrowserActivity.ARG_STARTING_DIRECTORY, path)
        }

        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, requestCode, resultCode, data)

        when (requestCode) {
            ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data

                    if (uri != null) {
                        persistPermissions(uri)

                        binding.activityRepoDirectory.setText(uri.toString())
                    }
                }
        }
    }

    private fun persistPermissions(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && ContentRepo.SCHEME == uri.scheme) {
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)
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
        val url = binding.activityRepoDirectory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(url)) {
            binding.activityRepoDirectoryInputLayout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            binding.activityRepoDirectoryInputLayout.error = null
        }

        val repoType = when {
            url.startsWith("file:") ->
                RepoType.DIRECTORY

            url.startsWith("content:") ->
                RepoType.DOCUMENT

            else -> {
                binding.activityRepoDirectoryInputLayout.error =
                        getString(R.string.invalid_repo_url, url)
                return
            }
        }


        val repo = try {
            viewModel.validate(repoType, url)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.activityRepoDirectoryInputLayout.error =
                    getString(R.string.repository_not_valid_with_reason, e.message)
            return
        }

        val finalize = Runnable {
            viewModel.saveRepo(repoType, repo.uri.toString())
        }

        if (repoType == RepoType.DIRECTORY) { // Make sure repo has Storage permission
            runWithPermission(AppPermissions.Usage.LOCAL_REPO, finalize)
        } else {
            finalize.run()
        }
    }

    companion object {
        private val TAG = DirectoryRepoActivity::class.java.name

        private const val ARG_REPO_ID = "repo_id"

        const val ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION = 0

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setClass(activity, DirectoryRepoActivity::class.java)
                    .putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }
}

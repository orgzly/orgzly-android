package com.orgzly.android.ui.repos

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.provider.clients.ReposClient
import com.orgzly.android.repos.DirectoryRepo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.MiscUtils

class DirectoryRepoActivity : RepoActivity() {

    private lateinit var directoryInputLayout: TextInputLayout
    private lateinit var directory: EditText

    private var repoId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)


        setContentView(R.layout.activity_repo_directory)

        val myToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setTitle(R.string.directory)


        directoryInputLayout = findViewById(R.id.fragment_repo_directory_input_layout)

        directory = findViewById(R.id.fragment_repo_directory)

        // Not working when done in XML
        directory.setHorizontallyScrolling(false)
        directory.maxLines = 3

        directory.setOnEditorActionListener { v, _, _ ->
            saveAndFinish()
            true
        }

        MiscUtils.clearErrorOnTextChange(directory, directoryInputLayout)

        findViewById<View>(R.id.fragment_repo_directory_browse_button)
                .setOnClickListener { _ -> startFileBrowser() }


        /* Set directory value for existing repository being edited. */
        if (repoId != 0L) {
            val uri = ReposClient.getUrl(this, repoId)
            directory.setText(uri)
        }
    }

    private fun startFileBrowser() {
        var browserStarted = false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            /*
             * Apparently some devices do not handle this intent.
             * Fallback to internal browser.
             */
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
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

        if (!TextUtils.isEmpty(directory.text)) {
            val uri = directory.text.toString()
            val path = Uri.parse(uri).path
            intent.putExtra(BrowserActivity.ARG_STARTING_DIRECTORY, path)
        }

        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION)
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
        val uriString = directory.text.toString().trim { it <= ' ' }

        if (TextUtils.isEmpty(uriString)) {
            directoryInputLayout.error = getString(R.string.can_not_be_empty)
            return
        } else {
            directoryInputLayout.error = null
        }

        val uri = Uri.parse(uriString)

        val repo = RepoFactory.getFromUri(this, uri)

        if (repo == null) {
            directoryInputLayout.error = getString(R.string.invalid_repo_url, uri)
            return
        }

        val finalize = Runnable {

            if (repoId != 0L) { // Update existing repository
                updateRepoUrl(repoId, repo.uri.toString())

            } else { // Add new repository
                addRepoUrl(repo.uri.toString())
            }

            finish()
        }

        if (repo is DirectoryRepo) { // Make sure "file:"-type repo has a storage permission
            runWithPermission(AppPermissions.Usage.LOCAL_REPO, finalize)
        } else {
            finalize.run()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(RepoActivity.TAG, requestCode, resultCode, data)

        when (requestCode) {
            ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data

                    if (uri != null) {
                        persistPermissions(uri)

                        updateUri(uri)
                    }
                }
        }
    }

    override fun updateUri(uri: Uri) {
        directory.setText(uri.toString())
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

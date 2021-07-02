package com.orgzly.android.ui.repos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.db.entity.Repo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.repos.RepoType
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.repo.directory.DirectoryRepoActivity
import com.orgzly.android.ui.repo.dropbox.DropboxRepoActivity
import com.orgzly.android.ui.repo.git.GitRepoActivity
import com.orgzly.android.ui.repo.webdav.WebdavRepoActivity
import com.orgzly.databinding.ActivityReposBinding
import javax.inject.Inject

/**
 * List of user-configured repositories.
 */
class ReposActivity : CommonActivity(), AdapterView.OnItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback  {
    private lateinit var binding: ActivityReposBinding

    @Inject
    lateinit var repoFactory: RepoFactory

    private lateinit var listAdapter: ArrayAdapter<Repo>

    lateinit var viewModel: ReposViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_repos)

        setupActionBar(R.string.repositories)

        setupNoReposButtons()

        listAdapter = object : ArrayAdapter<Repo>(this, R.layout.item_repo, R.id.item_repo_url) {
            override fun getItemId(position: Int): Long {
                return getItem(position)?.id ?: 0
            }
        }

        val factory = ReposViewModelFactory.getInstance(dataRepository)
        viewModel = ViewModelProvider(this, factory).get(ReposViewModel::class.java)

        viewModel.repos.observe(this, Observer { repos ->
            listAdapter.clear()
            listAdapter.addAll(repos)
            listAdapter.notifyDataSetChanged() // FIXME

            binding.activityReposFlipper.displayedChild =
                    if (repos != null && repos.isNotEmpty()) 0 else 1

            invalidateOptionsMenu()
        })

        viewModel.openRepoRequestEvent.observeSingle(this, Observer { repo ->
            if (repo != null) {
                openRepo(repo)
            }
        })

        viewModel.errorEvent.observeSingle(this, Observer { error ->
            if (error != null) {
                showSnackbar((error.cause ?: error).localizedMessage)
            }
        })

        binding.list.let {
            it.onItemClickListener = this
            it.adapter = listAdapter
            registerForContextMenu(it)
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.openRepo(id)
    }

    private fun setupNoReposButtons() {
        binding.activityReposDropbox.let { button ->
            if (BuildConfig.IS_DROPBOX_ENABLED) {
                button.setOnClickListener {
                    startRepoActivity(R.id.repos_options_menu_item_new_dropbox)
                }
            } else {
                button.visibility = View.GONE
            }
        }

        binding.activityReposGit.let { button ->
            if (BuildConfig.IS_GIT_ENABLED) {
                button.setOnClickListener {
                    startRepoActivity(R.id.repos_options_menu_item_new_git)
                }
            } else {
                button.visibility = View.GONE
            }
        }

        binding.activityReposWebdav.setOnClickListener {
            startRepoActivity(R.id.repos_options_menu_item_new_webdav)
        }

        binding.activityReposDirectory.setOnClickListener {
            startRepoActivity(R.id.repos_options_menu_item_new_directory)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menuInflater.inflate(R.menu.repos_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo

        return when (item.itemId) {
            R.id.repos_context_menu_delete -> {
                deleteRepo(info.id)
                true
            }

            else -> super.onContextItemSelected(item)
        }
    }

    private fun deleteRepo(id: Long) {
        viewModel.deleteRepo(id)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Do not display add icon if there are no repositories - large repo buttons will be shown
        if (listAdapter.count > 0) {
            menuInflater.inflate(R.menu.repos_actions, menu)

            val newRepos = menu.findItem(R.id.repos_options_menu_item_new).subMenu

            if (!BuildConfig.IS_DROPBOX_ENABLED) {
                newRepos.removeItem(R.id.repos_options_menu_item_new_dropbox)
            }

            if (!BuildConfig.IS_GIT_ENABLED) {
                newRepos.removeItem(R.id.repos_options_menu_item_new_git)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.repos_options_menu_item_new_dropbox -> {
                startRepoActivity(item.itemId)
                return true
            }

            R.id.repos_options_menu_item_new_git -> {
                startRepoActivity(item.itemId)
                return true
            }

            R.id.repos_options_menu_item_new_webdav -> {
                startRepoActivity(item.itemId)
                return true
            }

            R.id.repos_options_menu_item_new_directory -> {
                startRepoActivity(item.itemId)
                return true
            }

            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            ACTIVITY_REQUEST_CODE_FOR_READ_WRITE_EXTERNAL_STORAGE -> {
                val granted = grantResults.zip(permissions)
                        .find { (_, perm) -> perm == READ_WRITE_EXTERNAL_STORAGE }
                        ?.let { (grantResult, _) -> grantResult == PackageManager.PERMISSION_GRANTED }
                if (granted == true) {
                    GitRepoActivity.start(this)
                }
            }
        }
    }

    private fun startRepoActivity(id: Int) {
        when (id) {
            R.id.repos_options_menu_item_new_dropbox -> {
                DropboxRepoActivity.start(this)
                return
            }

            R.id.repos_options_menu_item_new_git -> {
                if (ContextCompat.checkSelfPermission(this, READ_WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    GitRepoActivity.start(this)
                } else {
                    // TODO: Show explanation why possibly, if ActivityCompat.shouldShowRequestPermissionRationale() says so?
                    ActivityCompat.requestPermissions(this, arrayOf(READ_WRITE_EXTERNAL_STORAGE), ACTIVITY_REQUEST_CODE_FOR_READ_WRITE_EXTERNAL_STORAGE)
                }
                return
            }

            R.id.repos_options_menu_item_new_webdav -> {
                WebdavRepoActivity.start(this)
                return
            }

            R.id.repos_options_menu_item_new_directory -> {
                DirectoryRepoActivity.start(this)
                return
            }

            else -> throw IllegalArgumentException("Unknown repo menu item clicked: $id")
        }
    }

    private fun openRepo(repoEntity: Repo) {
        // Validate before opening
        try {
            dataRepository.getRepoInstance(repoEntity.id, repoEntity.type, repoEntity.url)
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackbar(getString(R.string.repository_not_valid_with_reason, e.message))
            return
        }

        when (repoEntity.type) {
            RepoType.MOCK -> // TODO: Remove
                DropboxRepoActivity.start(this, repoEntity.id)

            RepoType.DROPBOX ->
                DropboxRepoActivity.start(this, repoEntity.id)

            RepoType.DIRECTORY ->
                DirectoryRepoActivity.start(this, repoEntity.id)

            RepoType.DOCUMENT ->
                DirectoryRepoActivity.start(this, repoEntity.id)

            RepoType.WEBDAV ->
                WebdavRepoActivity.start(this, repoEntity.id)

            RepoType.GIT ->
                GitRepoActivity.start(this, repoEntity.id)
        }
    }

    companion object {
        val TAG: String = ReposActivity::class.java.name

        const val READ_WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val ACTIVITY_REQUEST_CODE_FOR_READ_WRITE_EXTERNAL_STORAGE = 0
    }
}

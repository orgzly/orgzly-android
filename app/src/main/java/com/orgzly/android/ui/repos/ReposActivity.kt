package com.orgzly.android.ui.repos

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.widget.SimpleCursorAdapter
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.orgzly.BuildConfig

import com.orgzly.R
import com.orgzly.android.Shelf
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.clients.ReposClient
import com.orgzly.android.repos.ContentRepo
import com.orgzly.android.repos.DirectoryRepo
import com.orgzly.android.repos.DropboxRepo
import com.orgzly.android.repos.GitRepo
import com.orgzly.android.repos.MockRepo
import com.orgzly.android.repos.RepoFactory
import com.orgzly.android.ui.Loaders
import com.orgzly.android.util.LogUtils
import kotlinx.android.synthetic.main.activity_repos.*

/**
 * Configuring repositories.
 */
class ReposActivity :
        RepoActivity(),
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var listAdapter: SimpleCursorAdapter

    private lateinit var mShelf: Shelf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_repos)

        setupActionBar(R.string.repositories)

        mShelf = Shelf(applicationContext)

        setupNoReposButtons()

        list.onItemClickListener = this

        listAdapter = setupAdapter()
        list.adapter = listAdapter
        registerForContextMenu(list)

        supportLoaderManager?.initLoader(Loaders.REPOS_FRAGMENT, null, this)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onRepoEditRequest(id)
    }

    private fun setupAdapter(): SimpleCursorAdapter {
        /* Column field names to be bound. */
        val columns = arrayOf(ProviderContract.Repos.Param.REPO_URL)

        /* Views which the data will be bound to. */
        val to = intArrayOf(R.id.item_repo_url)

        /* Create adapter using Cursor. */
        val adapter = SimpleCursorAdapter(
                this,
                R.layout.item_repo, null,
                columns,
                to,
                0)

        adapter.setViewBinder { view, cursor, columnIndex ->
            val textView: TextView

            when (view.id) {
                R.id.item_repo_url -> {
                    if (!cursor.isNull(columnIndex)) {
                        textView = view as TextView
                        textView.text = cursor.getString(columnIndex)
                    }
                    true
                }

                else ->
                    false
            }
        }

        return adapter
    }

    private fun setupNoReposButtons() {
        activity_repos_dropbox.let {
            if (BuildConfig.IS_DROPBOX_ENABLED) {
                it.setOnClickListener {
                    startRepoActivity(R.id.repos_options_menu_item_new_dropbox)
                }
            } else {
                it.visibility = View.GONE
            }
        }

        activity_repos_git.let {
            if (BuildConfig.IS_GIT_ENABLED) {
                it.setOnClickListener {
                    startRepoActivity(R.id.repos_options_menu_item_new_git)
                }
            } else {
                it.visibility = View.GONE
            }
        }

        activity_repos_directory.setOnClickListener {
            startRepoActivity(R.id.repos_options_menu_item_new_external_storage_directory)
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

            R.id.repos_options_menu_item_new_external_storage_directory -> {
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

    private fun startRepoActivity(id: Int) {
        when (id) {
            R.id.repos_options_menu_item_new_dropbox -> {
                DropboxRepoActivity.start(this)
                return
            }

            R.id.repos_options_menu_item_new_git -> {
                GitRepoActivity.start(this)
                return
            }

            R.id.repos_options_menu_item_new_external_storage_directory -> {
                DirectoryRepoActivity.start(this)
                return
            }

            else -> throw IllegalArgumentException("Unknown repo menu item clicked: $id")
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun deleteRepo(id: Long) {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                mShelf.deleteRepo(id)
                return null
            }
        }.execute()
    }

    private fun onRepoEditRequest(id: Long) {
        val url = ReposClient.getUrl(this, id)

        val repo = RepoFactory.getFromUri(this, url)

        if (repo is DropboxRepo || repo is MockRepo) {  // TODO: Remove Mock from here
            DropboxRepoActivity.start(this, id)

        } else if (repo is DirectoryRepo || repo is ContentRepo) {
            DirectoryRepoActivity.start(this, id)

        } else if (repo is GitRepo) {
            GitRepoActivity.start(this, id)

        } else {
            showSnackbar(R.string.message_unsupported_repository_type)
        }
    }


    override fun onCreateLoader(id: Int, args: Bundle?): android.support.v4.content.Loader<Cursor> {
        return CursorLoader(
                this,
                ProviderContract.Repos.ContentUri.repos(),
                null,
                null,
                null,
                ProviderContract.Repos.Param.REPO_URL)
    }

    override fun onLoadFinished(loader: android.support.v4.content.Loader<Cursor>, data: Cursor?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, loader)

        listAdapter.swapCursor(data)

        if (listAdapter.count > 0) {
            activity_repos_flipper.displayedChild = 0
        } else {
            activity_repos_flipper.displayedChild = 1
        }

        invalidateOptionsMenu()
    }

    override fun onLoaderReset(loader: android.support.v4.content.Loader<Cursor>) {
        listAdapter.changeCursor(null)
    }

    companion object {
        val TAG: String = ReposActivity::class.java.name
    }
}

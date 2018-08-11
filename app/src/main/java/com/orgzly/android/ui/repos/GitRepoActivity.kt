package com.orgzly.android.ui.repos


import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.text.TextUtils
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import com.orgzly.R
import com.orgzly.android.git.GitPreferences
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.provider.clients.ReposClient
import com.orgzly.android.repos.GitRepo
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.MiscUtils
import kotlinx.android.synthetic.main.activity_repo_git.*
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.IOException

class GitRepoActivity : RepoActivity(), GitPreferences {
    private lateinit var fields: Array<Field>

    private var repoId: Long = 0

    data class Field(var editText: EditText, var layout: TextInputLayout, var preference: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_repo_git)

        setupActionBar(R.string.git)

        fields = arrayOf(
                Field(
                        activity_repo_git_directory,
                        activity_repo_git_directory_layout,
                        R.string.pref_key_git_repository_filepath),
                Field(
                        activity_repo_git_ssh_key,
                        activity_repo_git_ssh_key_layout,
                        R.string.pref_key_git_ssh_key_path),
                Field(
                        activity_repo_git_author,
                        activity_repo_git_author_layout,
                        R.string.pref_key_git_author),
                Field(
                        activity_repo_git_email,
                        activity_repo_git_email_layout,
                        R.string.pref_key_git_email),
                Field(
                        activity_repo_git_branch,
                        activity_repo_git_branch_layout,
                        R.string.pref_key_git_branch_name))


        /* Clear error after field value has been modified. */
        MiscUtils.clearErrorOnTextChange(activity_repo_git_url, activity_repo_git_url_layout)
        fields.forEach {
            MiscUtils.clearErrorOnTextChange(it.editText, it.layout)
        }

        activity_repo_git_directory_browse.setOnClickListener {
            startLocalFileBrowser(activity_repo_git_directory, ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION)
        }

        activity_repo_git_ssh_key_browse.setOnClickListener {
            startLocalFileBrowser(activity_repo_git_ssh_key, ACTIVITY_REQUEST_CODE_FOR_SSH_KEY_SELECTION, true)
        }

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        /* Set directory value for existing repository being edited. */
        if (repoId != 0L) {
            val uri = ReposClient.getUrl(this, repoId)
            activity_repo_git_url.setText(uri)
            setFromPreferences()
        }
    }

    private fun setFromPreferences() {
        val prefs = RepoPreferences(this, repoId)
        for (field in fields) {
            setTextFromPrefKey(prefs, field.editText, field.preference)
        }
    }

    private fun setTextFromPrefKey(prefs: RepoPreferences, editText: EditText, prefKey: Int) {
        if (editText.length() < 1) {
            editText.setText(prefs.getStringValue(prefKey, ""))
        }
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
                saveAndFinish()
                return true
            }

            R.id.close, android.R.id.home -> {
                finish()
                true
            }

            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        menuInflater.inflate(R.menu.repos_context, menu)
    }

    private fun saveAndFinish() {
        if (validateFields()) {
            RepoCloneTask(this).execute()
        }
    }

    private fun repoCheckComplete(e: IOException?) {
        if (e == null) {
            save()

        } else {
            e.printStackTrace()
            showSnackbar(e.toString())
        }
    }

    private fun saveToPreferences(id: Long): Boolean {
        val editor: SharedPreferences.Editor = RepoPreferences(this, id).repoPreferences.edit()

        for (field in fields) {
            val settingName = getSettingName(field.preference)
            val value = field.editText.text.toString()
            if (value.isNotEmpty()) {
                editor.putString(settingName, value)
            } else {
                editor.remove(settingName)
            }
        }

        return editor.commit()
    }

    private fun save() {
        val remoteUriString = remoteUri().toString()
        if (repoId == 0L) {
            ReposClient.insert(this, remoteUriString)
            repoId = ReposClient.getId(this, remoteUriString)
        }
        if (ReposClient.getUrl(this, repoId) !== remoteUriString) {
            ReposClient.updateUrl(this, repoId, remoteUriString)
        }

        saveToPreferences(repoId)

        runWithPermission(AppPermissions.Usage.LOCAL_REPO, Runnable { finish() })
    }

    private fun validateFields(): Boolean {
        var hasEmptyFields = false

        if (errorIfEmpty(activity_repo_git_url, activity_repo_git_url_layout)) {
            hasEmptyFields = true
        }

        for (field in fields) {
            if (errorIfEmpty(field.editText, field.layout)) {
                hasEmptyFields = true
            }
        }

        return !hasEmptyFields
    }

    private fun errorIfEmpty(editText: EditText, layout: TextInputLayout): Boolean {
        val isEmpty = TextUtils.isEmpty(editText.text)
        if (isEmpty) {
            layout.error = getString(R.string.can_not_be_empty)
        }
        return isEmpty
    }

    private fun getSettingName(setting: Int): String {
        return resources.getString(setting)
    }

    private fun withDefault(v: String?, selector: Int): String {
        return if (v != null && v.isNotEmpty()) {
            v
        } else AppPreferences.getStateSharedPreferences(this).getString(getSettingName(selector), "")
    }

    override fun sshKeyPathString(): String {
        return withDefault(activity_repo_git_ssh_key.text.toString(), R.string.pref_key_git_ssh_key_path)
    }

    override fun getAuthor(): String {
        return withDefault(activity_repo_git_author.text.toString(), R.string.pref_key_git_author)
    }

    override fun getEmail(): String {
        return withDefault(activity_repo_git_email.text.toString(), R.string.pref_key_git_email)
    }

    override fun repositoryFilepath(): String {
        val v = activity_repo_git_directory.text.toString()
        return if (v.isNotEmpty()) {
            v
        } else {
            AppPreferences.repositoryStoragePathForUri(this, remoteUri())
        }
    }

    override fun remoteName(): String {
        // TODO: Update this if remote selection is ever allowed.
        return withDefault("", R.string.pref_key_git_remote_name)
    }

    override fun branchName(): String {
        return withDefault(activity_repo_git_branch.text.toString(), R.string.pref_key_git_branch_name)
    }

    override fun remoteUri(): Uri {
        val remoteUriString = activity_repo_git_url.text.toString()
        return Uri.parse(remoteUriString)
    }

    private fun startLocalFileBrowser(editText: EditText, requestCode: Int, isFileSelectable: Boolean = false) {
        val intent = Intent(Intent.ACTION_VIEW).setClass(this, BrowserActivity::class.java)

        if (!TextUtils.isEmpty(editText.text)) {
            val uri = editText.text.toString()
            val path = Uri.parse(uri).path
            intent.putExtra(BrowserActivity.ARG_STARTING_DIRECTORY, path)
        }

        if (isFileSelectable) {
            intent.putExtra(BrowserActivity.ARG_IS_FILE_SELECTABLE, true)
        }

        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data
                    activity_repo_git_directory.setText(uri.toString())
                }
            ACTIVITY_REQUEST_CODE_FOR_SSH_KEY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data
                    activity_repo_git_ssh_key.setText(uri.toString())
                }
        }
    }

    internal inner class CloneProgressUpdate(var amount: Int, var setMax: Boolean)

    internal inner class RepoCloneTask(var fragment: GitRepoActivity) : AsyncTask<Void, CloneProgressUpdate, IOException>(), ProgressMonitor {
        var progressDialog: ProgressDialog = ProgressDialog(this@GitRepoActivity)

        override fun onPreExecute() {
            progressDialog.setMessage("Ensuring repository settings will work.")
            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void): IOException? {
            try {
                GitRepo.ensureRepositoryExists(fragment, true, this)
            } catch (e: IOException) {
                return e
            }

            return null
        }

        override fun onProgressUpdate(vararg updates: CloneProgressUpdate) {
            for (i in updates.indices) {
                val u = updates[i]
                if (u.setMax) {
                    progressDialog.setMessage("Cloning repository")
                    progressDialog.hide()
                    progressDialog.isIndeterminate = false
                    progressDialog.show()
                    progressDialog.max = u.amount
                } else {
                    progressDialog.incrementProgressBy(u.amount)
                }
            }
        }

        override fun onCancelled() {
            progressDialog.dismiss()
        }

        override fun onPostExecute(e: IOException) {
            progressDialog.dismiss()
            fragment.repoCheckComplete(e)
        }


        override fun start(totalTasks: Int) {
            publishProgress(CloneProgressUpdate(totalTasks, true))
        }

        override fun beginTask(title: String, totalWork: Int) {

        }

        override fun update(completed: Int) {
            publishProgress(CloneProgressUpdate(completed, false))
        }

        override fun endTask() {

        }
    }

    companion object {
        private val TAG = GitRepoActivity::class.java.name

        private const val ARG_REPO_ID = "repo_id"

        const val ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION = 0
        const val ACTIVITY_REQUEST_CODE_FOR_SSH_KEY_SELECTION = 1

        @JvmStatic
        @JvmOverloads
        fun start(activity: Activity, repoId: Long = 0) {
            val intent = Intent(Intent.ACTION_VIEW)
                    .setClass(activity, GitRepoActivity::class.java)
                    .putExtra(ARG_REPO_ID, repoId)

            activity.startActivity(intent)
        }
    }
}

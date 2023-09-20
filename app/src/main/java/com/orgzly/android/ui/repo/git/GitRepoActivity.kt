package com.orgzly.android.ui.repo.git


import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.git.GitPreferences
import com.orgzly.android.git.GitSshKeyTransportSetter
import com.orgzly.android.git.GitTransportSetter
import com.orgzly.android.git.HTTPSTransportSetter
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.repos.GitRepo
import com.orgzly.android.repos.RepoType
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.repo.BrowserActivity
import com.orgzly.android.ui.repo.RepoViewModel
import com.orgzly.android.ui.repo.RepoViewModelFactory
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.MiscUtils
import com.orgzly.databinding.ActivityRepoGitBinding
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class GitRepoActivity : CommonActivity(), GitPreferences {
    private lateinit var binding: ActivityRepoGitBinding

    private lateinit var fields: Array<Field>

    data class Field(var editText: EditText, var layout: TextInputLayout, var preference: Int, var allowEmpty: Boolean = false)

    private lateinit var viewModel: RepoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = ActivityRepoGitBinding.inflate(layoutInflater)

        setContentView(binding.root)

        fields = arrayOf(
                Field(
                        binding.activityRepoGitDirectory,
                        binding.activityRepoGitDirectoryLayout,
                        R.string.pref_key_git_repository_filepath),
                Field(
                        binding.activityRepoGitHttpsUsername,
                        binding.activityRepoGitHttpsUsernameLayout,
                        R.string.pref_key_git_https_username,
                        true
                ),
                Field(
                        binding.activityRepoGitHttpsPassword,
                        binding.activityRepoGitHttpsPasswordLayout,
                        R.string.pref_key_git_https_password,
                        true
                ),
                Field(
                        binding.activityRepoGitAuthor,
                        binding.activityRepoGitAuthorLayout,
                        R.string.pref_key_git_author),
                Field(
                        binding.activityRepoGitEmail,
                        binding.activityRepoGitEmailLayout,
                        R.string.pref_key_git_email),
                Field(
                        binding.activityRepoGitBranch,
                        binding.activityRepoGitBranchLayout,
                        R.string.pref_key_git_branch_name))

        /* Clear error after field value has been modified. */
        MiscUtils.clearErrorOnTextChange(binding.activityRepoGitUrl, binding.activityRepoGitUrlLayout)
        fields.forEach {
            MiscUtils.clearErrorOnTextChange(it.editText, it.layout)
        }

        binding.activityRepoGitDirectoryBrowse.setOnClickListener {
            startLocalFileBrowser(binding.activityRepoGitDirectory, ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION)
        }

        val repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        val factory = RepoViewModelFactory.getInstance(dataRepository, repoId)

        viewModel = ViewModelProvider(this, factory).get(RepoViewModel::class.java)

        if (repoId != 0L) {
            /* Set directory value for existing repository being edited. */
            dataRepository.getRepo(repoId)?.let { repo ->
                binding.activityRepoGitUrl.setText(repo.url)
                setFromPreferences()
            }
        } else {
            /* Set default values for new repo being added. */
            createDefaultRepoFolder()
            binding.activityRepoGitAuthor.setText("Orgzly")
            binding.activityRepoGitBranch.setText(R.string.git_default_branch)
            val userDeviceName: String = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            } else {
                Settings.Secure.getString(contentResolver, "bluetooth_name")
            }
            binding.activityRepoGitEmail.setText(String.format("orgzly@%s", userDeviceName))
        }

        viewModel.finishEvent.observeSingle(this, Observer {
            saveToPreferences(viewModel.repoId)

            // TODO: Check permission on start
            runWithPermission(AppPermissions.Usage.LOCAL_REPO, Runnable { finish() })
        })

        viewModel.alreadyExistsEvent.observeSingle(this, Observer {
            showSnackbar(R.string.repository_url_already_exists)
        })


        viewModel.errorEvent.observeSingle(this, Observer { error ->
            if (error != null) {
                showSnackbar((error.cause ?: error).localizedMessage)
            }
        })

        binding.activityRepoGitUrl.setOnFocusChangeListener { _view, _hasFocus ->
            updateAuthVisibility()
            setHttpsAuthFromUri()
        }

        updateAuthVisibility()

        binding.topToolbar.run {
            setNavigationOnClickListener {
                finish()
            }
        }

        binding.fab.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun getRepoScheme(): String {
        return try {
            Uri.parse(binding.activityRepoGitUrl.text.toString()).scheme.orEmpty()
        } catch(_: Throwable) {
            ""
        }
    }

    private fun setHttpsAuthFromUri() {
        val repoScheme = getRepoScheme()
        if ("https" != repoScheme) {
            return
        }
        val userInfo = Uri.parse(binding.activityRepoGitUrl.text.toString()).userInfo.orEmpty()
        // We don't want the password visible if it was copy-pasted to the remote address field
        var repoUrl = binding.activityRepoGitUrl.text.toString()
        repoUrl.replaceFirst(userInfo, "")
        binding.activityRepoGitUrl.setText(repoUrl)
        val splitInfo = userInfo.split(":")
        val username = splitInfo[0]
        val password = splitInfo.asIterable().elementAtOrElse(1) { "" }
        binding.activityRepoGitHttpsUsername.setText(username)
        binding.activityRepoGitHttpsPassword.setText(password)
    }

    private fun updateAuthVisibility() {
        val repoScheme = getRepoScheme()
        if ("https" == repoScheme) {
            binding.activityRepoGitHttpsAuthInfo.visibility = View.VISIBLE
            binding.activityRepoGitHttpsUsernameLayout.visibility = View.VISIBLE
            binding.activityRepoGitHttpsPasswordLayout.visibility = View.VISIBLE
        } else {
            binding.activityRepoGitHttpsAuthInfo.visibility = View.GONE
            binding.activityRepoGitHttpsUsernameLayout.visibility = View.GONE
            binding.activityRepoGitHttpsPasswordLayout.visibility = View.GONE
        }
    }

    // TODO: Since we can create multiple syncs, this folder might be re-used, do we want to create
    //       a new one if this directory is already used up?
    private fun createDefaultRepoFolder() {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return
        }
        val externalPath = Environment.getExternalStorageDirectory().path
        val orgzlyGitPath = File("$externalPath/orgzly-git/")
        var success = false
        try {
            success = orgzlyGitPath.mkdirs()
        } catch(error: SecurityException) {}
        if (success || (orgzlyGitPath.exists() && orgzlyGitPath.list().size == 0)) {
            binding.activityRepoGitDirectory.setText(orgzlyGitPath.path)
        }
    }

    private fun setFromPreferences() {
        val prefs = RepoPreferences.fromId(this, viewModel.repoId, dataRepository)
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

        menuInflater.inflate(R.menu.done, menu)

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

            android.R.id.home -> {
                finish()
                true
            }

            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        menuInflater.inflate(R.menu.repos_cab, menu)
    }

    private fun saveAndFinish() {
        if (validateFields()) {
            val repoId = intent.getLongExtra(ARG_REPO_ID, 0)
            if (repoId != 0L) {
                save()
            } else {
                val targetDirectory = File(binding.activityRepoGitDirectory.text.toString())
                if (targetDirectory.list()!!.isNotEmpty()) {
                    binding.activityRepoGitDirectoryLayout.error = getString(R.string.git_clone_error_target_not_empty)
                } else {
                    // TODO: If this fails we should notify the user in a nice way and mark the git repo field as bad
                    RepoCloneTask(this).execute()
                }
            }
        }
    }

    private fun repoCheckComplete(e: IOException?) {
        if (e == null) {
            save()
        } else {
            val error = when (e.cause) {
                is NoRemoteRepositoryException -> R.string.git_clone_error_invalid_repo
                is TransportException -> {
                    // JGit's catch-all "remote hung up unexpectedly" message is not very useful.
                    if (Regex("hung up unexpectedly").containsMatchIn(e.cause!!.message!!)) {
                        String.format(getString(R.string.git_clone_error_ssh), e.cause!!.cause!!.message)
                    } else {
                        String.format(getString(R.string.git_clone_error_ssh), e.cause!!.message)
                    }
                }
                // TODO: This should be checked when the user enters a directory by hand
                is FileNotFoundException -> R.string.git_clone_error_invalid_target_dir
                is GitRepo.DirectoryNotEmpty -> R.string.git_clone_error_target_not_empty
                is NotSupportedException -> R.string.git_clone_error_uri_not_supported
                else -> R.string.git_clone_error_unknown
            }
            when (error) {
                is Int -> { showSnackbar(error) }
                is String -> { showSnackbar(error) }
            }
            e.printStackTrace()
        }
    }

    private fun saveToPreferences(id: Long): Boolean {
        val editor: SharedPreferences.Editor = RepoPreferences.fromId(this, id, dataRepository).repoPreferences.edit()

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

        viewModel.saveRepo(RepoType.GIT, remoteUriString /* TODO: preferences. */)
    }

    private fun validateFields(): Boolean {
        var hasEmptyFields = false

        if (errorIfEmpty(binding.activityRepoGitUrl, binding.activityRepoGitUrlLayout)) {
            hasEmptyFields = true
        }

        val targetDirectory = File(binding.activityRepoGitDirectory.text.toString())
        if (!targetDirectory.exists()) {
            binding.activityRepoGitDirectoryLayout.error = getString(R.string.git_clone_error_invalid_target_dir)
        }

        for (field in fields) {
            if (field.layout.visibility == View.GONE || field.allowEmpty) {
                continue;
            }
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
        } else {
            AppPreferences.getStateSharedPreferences(this)?.getString(getSettingName(selector), "").orEmpty()
        }
    }

    // TODO: This is pretty much a duplication of GitPreferencesFromRepoPrefs, it would be nice
    //  to join them somehow.
    override fun createTransportSetter(): GitTransportSetter {
        val scheme = remoteUri().scheme
        if ("https" == scheme) {
            val username = withDefault(binding.activityRepoGitHttpsUsername.text.toString(), R.string.pref_key_git_https_username)
            val password = withDefault(binding.activityRepoGitHttpsPassword.text.toString(), R.string.pref_key_git_https_password)
            return HTTPSTransportSetter(username, password)
        } else {
            return GitSshKeyTransportSetter()
        }
    }

    override fun getAuthor(): String {
        return withDefault(binding.activityRepoGitAuthor.text.toString(), R.string.pref_key_git_author)
    }

    override fun getEmail(): String {
        return withDefault(binding.activityRepoGitEmail.text.toString(), R.string.pref_key_git_email)
    }

    override fun repositoryFilepath(): String {
        val v = binding.activityRepoGitDirectory.text.toString()
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
        return withDefault(binding.activityRepoGitBranch.text.toString(), R.string.pref_key_git_branch_name)
    }

    override fun remoteUri(): Uri {
        val remoteUriString = binding.activityRepoGitUrl.text.toString()
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
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ACTIVITY_REQUEST_CODE_FOR_DIRECTORY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data
                    binding.activityRepoGitDirectory.setText(uri?.path)
                }
        }
    }

    internal inner class CloneProgressUpdate(var amount: Int, var setMax: Boolean)

    internal inner class RepoCloneTask(var fragment: GitRepoActivity) : AsyncTask<Void, CloneProgressUpdate, IOException>(), ProgressMonitor {
        var progressDialog: ProgressDialog = ProgressDialog(this@GitRepoActivity)

        override fun onPreExecute() {
            val message = fragment.resources.getString(R.string.git_verifying_settings)
            progressDialog.setMessage(message)
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
                    val message = fragment.resources.getString(R.string.git_clone_progress)
                    progressDialog.setMessage(message)
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

        override fun onPostExecute(e: IOException?) {
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

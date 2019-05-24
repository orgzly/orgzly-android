package com.orgzly.android.ui.repo.git


import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.git.GitPreferences
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.prefs.RepoPreferences
import com.orgzly.android.repos.GitRepo
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.repo.BrowserActivity
import com.orgzly.android.ui.views.ArrayPagerAdapter
import com.orgzly.android.ui.views.AdaptableHeightViewPager
import com.orgzly.android.usecase.RepoCreate
import com.orgzly.android.usecase.RepoUpdate
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.MiscUtils
import kotlinx.android.synthetic.main.activity_repo_git.*
import org.eclipse.jgit.errors.NoRemoteRepositoryException
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.Charset

class GitRepoActivity : CommonActivity(), GitPreferences {
    private lateinit var fields: Array<Field>

    private var repoId: Long = 0
    private lateinit var authFragments: Array<AuthConfigFragment>

    data class Field(var editText: EditText, var layout: TextInputLayout, var preference: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_repo_git)

        setupActionBar(R.string.git)

        setupAuthPager()

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

        activity_repo_git_ssh_key_generate.setOnClickListener {
            val hasSSHKey = !applicationContext.fileList().find { it == SSH_PUBLIC_KEY_FILENAME }.isNullOrEmpty()
            if (hasSSHKey) {
                Toast.makeText(this, "Using key generated earlier", Toast.LENGTH_LONG)
                        .show()
                // TODO: If we want to keep this the path should be kept relative in the settings we save somehow
                activity_repo_git_ssh_key.setText(applicationContext.getFileStreamPath(".ssh_key").path)
                activity_repo_git_ssh_key_copy.visibility = View.VISIBLE
            } else {
                SSHKeypairGenerationTask(this).execute()
            }
        }

        activity_repo_git_ssh_key_copy.setOnClickListener {
            val fileStream = applicationContext.openFileInput(SSH_PUBLIC_KEY_FILENAME)
            val pubKey = String(fileStream.readBytes(), Charset.defaultCharset())
            val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("pubkey", pubKey)
            clipBoard.primaryClip = clip
            // TODO: Make this a string resource instead
            Toast.makeText(this, "Public key copied to clipboard", Toast.LENGTH_SHORT)
                    .show()
        }

        repoId = intent.getLongExtra(ARG_REPO_ID, 0)

        /* Set directory value for existing repository being edited. */
        if (repoId != 0L) {
            Log.d("mylog", "restoring from repoId: $repoId")
            dataRepository.getRepo(repoId)?.let { repo ->
                activity_repo_git_url.setText(repo.url)
                setFromPreferences()
            }
        } else {
            createDefaultRepoFolder();
        }
    }

    private fun setupAuthPager() {
        val authPager = findViewById<AdaptableHeightViewPager>(R.id.activity_repo_git_auth_pager)
        val authTabs = findViewById<TabLayout>(R.id.activity_repo_git_auth_tabs)

        val adapter = ArrayPagerAdapter(supportFragmentManager, arrayOf(SSHAuthConfigFragment(), HTTPSAuthConfigFragment()))
        authPager.adapter = adapter
        authPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(authTabs))
        authTabs.addOnTabSelectedListener(AdaptableHeightViewPager.TabListener(authPager))
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
            activity_repo_git_directory.setText(orgzlyGitPath.path)
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
            val setting = prefs.getStringValue(prefKey, "")
            Log.d("mylog", "setting field to $setting")
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
        menuInflater.inflate(R.menu.repos_context, menu)
    }

    private fun saveAndFinish() {
        if (validateFields()) {
            // TODO: If this fails we should notify the user in a nice way and mark the git repo field as bad
            RepoCloneTask(this).execute()
        }
    }

    private fun repoCheckComplete(e: IOException?) {
        if (e == null) {
            save()
        } else {
            val errorId = when {
                e.cause is NoRemoteRepositoryException -> R.string.git_clone_error_invalid_repo
                // TODO: This should be checked when the user enters a directory by hand
                e.cause is FileNotFoundException -> R.string.git_clone_error_invalid_target_dir
                e.cause is GitRepo.DirectoryNotEmpty -> R.string.git_clone_error_target_not_empty
                e.cause is NotSupportedException -> R.string.git_clone_error_uri_not_supported
                else -> R.string.git_clone_error_unknown
            }
            showSnackbar(errorId)
            e.printStackTrace()
        }
    }

    // TODO: Finish this function
//    private fun sshKeyGenerated(e: IOException?) {
//        if (e == null) {
//            // show the copy button and fill in the path to the file next to browse
//            activity_repo_git_ssh_key_copy.visibility = View.VISIBLE
//            activity_repo_git_ssh_key
//        } else {
//            e.printStackTrace()
//            showSnackbar(e.toString())
//        }
//    }

    private fun saveToPreferences(id: Long): Boolean {
        val editor: SharedPreferences.Editor = RepoPreferences(this, id).repoPreferences.edit()

        Log.d("mylog", "saveToPreferences id: $id")
        for (field in fields) {
            val settingName = getSettingName(field.preference)
            Log.d("mylog", "setting name: $settingName")
            val value = field.editText.text.toString()
            if (value.isNotEmpty()) {
                Log.d("mylog", "saved $settingName")
                editor.putString(settingName, value)
            } else {
                editor.remove(settingName)
            }
        }

        return editor.commit()
    }

    private fun save() {
        val remoteUriString = remoteUri().toString()

        val useCase = if (repoId != 0L) {
            RepoUpdate(repoId, remoteUriString)
        } else {
            RepoCreate(remoteUriString)
        }

        App.EXECUTORS.diskIO().execute {
            val result = UseCaseRunner.run(useCase)
            if (repoId == 0L) {
                repoId = result.userData as Long
            }

            App.EXECUTORS.mainThread().execute {
                saveToPreferences(repoId)
                runWithPermission(AppPermissions.Usage.LOCAL_REPO, Runnable { finish() })
            }
        }
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
                    activity_repo_git_directory.setText(uri.path)
                }
            ACTIVITY_REQUEST_CODE_FOR_SSH_KEY_SELECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val uri = data.data
                    activity_repo_git_ssh_key.setText(uri.path)
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

    internal inner class SSHKeypairGenerationTask(var fragment: GitRepoActivity) : AsyncTask<Void, Void, IOException>(), ProgressMonitor {
        private var progressDialog: ProgressDialog = ProgressDialog(this@GitRepoActivity)

        override fun onPreExecute() {
            progressDialog.setMessage("Generating SSH keypair with 4096 as keysize and no passphrase.")
            progressDialog.isIndeterminate = true
            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void): IOException? {
            // TODO: Ask the user for a passphrase and size of the key we generate
            return try {
                val jsch = JSch()
                val keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096)

                val privKeyFile = fragment.applicationContext.openFileOutput(SSH_PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE)
                keyPair.writePrivateKey(privKeyFile)
                val pubKeyFile = fragment.applicationContext.openFileOutput(SSH_PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE)
                keyPair.writePublicKey(pubKeyFile, "Generated by Orgzly")

                null
            } catch (e: IOException) {
                e
            }
        }

        override fun onCancelled() {
            progressDialog.dismiss()
        }

        override fun onPostExecute(e: IOException?) {
            progressDialog.dismiss()
//            fragment.sshKeyGenerated(e)
        }


        override fun start(totalTasks: Int) {
        }

        override fun beginTask(title: String, totalWork: Int) {
        }

        override fun update(completed: Int) {
        }

        override fun endTask() {
        }
    }

    companion object {
        private val TAG = GitRepoActivity::class.java.name

        private const val SSH_PUBLIC_KEY_FILENAME = "id_rsa.pub"
        private const val SSH_PRIVATE_KEY_FILENAME = "id_rsa"

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

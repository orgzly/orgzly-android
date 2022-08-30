package com.orgzly.android.ui.repo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.LocalStorage
import com.orgzly.android.repos.DirectoryRepo
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.dialogs.SimpleOneLinerDialog
import com.orgzly.android.ui.showSnackbar
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UriUtils
import com.orgzly.databinding.ActivityBrowserBinding
import java.io.File
import java.io.FilenameFilter
import java.util.*

// TODO: Rewrite or remove
class BrowserActivity :
        CommonActivity(),
        AdapterView.OnItemClickListener {

    private lateinit var binding: ActivityBrowserBinding

    private lateinit var listView: ListView

    private var itemList: MutableList<BrowserItem> = mutableListOf()

    private var currentItem: String? = null
    private var nextItem: String? = null

    private var isFileSelectable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = ActivityBrowserBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setupViews()

        setNextItemFromArguments(savedInstanceState)

        isFileSelectable = intent.getBooleanExtra(ARG_IS_FILE_SELECTABLE, false)

        loadFileListFromNext(true)

        supportFragmentManager.setFragmentResultListener("name-new-folder", this) { _, result ->
            val value = result.getString("value", "")

            val file = File(currentItem, value)

            if (file.mkdir()) {
                refresh()

            } else {
                val message =
                    resources.getString(R.string.failed_creating_directory, file.toString())
                showSnackbar(message)
            }
        }

        binding.topToolbar.run {
            menu.clear()
            inflateMenu(R.menu.browser)

            setNavigationOnClickListener {
                this@BrowserActivity.finish()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.open_home -> {
                        nextItem = defaultPath()
                        loadFileListFromNext(true)
                    }

                    R.id.new_folder -> {
                        create()
                    }
                }

                true
            }
        }

        binding.fab.setOnClickListener {
            useAndFinish(currentItem)
        }
    }

    private fun setupViews() {
        listView = binding.list
        listView.onItemClickListener = this
    }

    private fun setNextItemFromArguments(savedInstanceState: Bundle?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState, intent)

        nextItem = when {
            savedInstanceState?.containsKey(ARG_DIRECTORY) == true ->
                savedInstanceState.getString(ARG_DIRECTORY)

            intent.hasExtra(ARG_STARTING_DIRECTORY) ->
                intent.getStringExtra(ARG_STARTING_DIRECTORY)

            else ->
                defaultPath()
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, nextItem)
    }

    override fun onResume() {
        super.onResume()

        /* Check for permissions. */
        AppPermissions.isGrantedOrRequest(this, AppPermissions.Usage.LOCAL_REPO)
    }

    override fun onBackPressed() {
        // super.onBackPressed()

        // Up
        currentItem?.let {
            File(it).parentFile?.let { parentFile ->
                nextItem = parentFile.absolutePath
                loadFileListFromNext(false)
            }
        }
    }

    private fun create() {
        SimpleOneLinerDialog
            .getInstance("name-new-folder", R.string.new_folder, R.string.create, null)
            .show(supportFragmentManager, SimpleOneLinerDialog.FRAGMENT_TAG)
    }

    private fun refresh() {
        nextItem = currentItem
        loadFileListFromNext(false)
    }

    private fun useAndFinish(item: String?) {
        val uri = UriUtils.uriFromPath(DirectoryRepo.SCHEME, item)

        setResult(Activity.RESULT_OK, Intent().setData(uri))

        finish()
    }

    private fun defaultPath(): String? {
        return LocalStorage.storage(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, outState)

        outState.putString(ARG_DIRECTORY, currentItem)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = listView.getItemAtPosition(position) as BrowserItem

        val current = currentItem

        if (current != null) {
            if (item.isUp) {
                val path = File(current)

                path.parentFile?.let { parentFile ->
                    nextItem = parentFile.absolutePath
                    loadFileListFromNext(false)
                }

            } else {
                val sel = File(current, item.name)

                if (sel.isDirectory) {
                    nextItem = sel.absolutePath
                    loadFileListFromNext(false)

                } else if (isFileSelectable) {
                    useAndFinish(sel.absolutePath)
                }
            }

        } else {
            Log.e(TAG, "Clicked on " + item.name + " but there is no current directory set")
        }
    }

    private fun fileList(path: String?): Array<File>? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Trying to get a list of files in $path")

        if (path != null) {
            val file = File(path)

            if (file.exists()) {
                return file.listFiles(FILENAME_FILTER)
            }
        }

        return null
    }

    /**
     * Populates [listView] with non-hidden files and directories in [nextItem].
     * Creates a new adapter and uses it for list view.
     */
    private fun loadFileListFromNext(fallbackToDefaultOrRoot: Boolean) {
        fileList(fallbackToDefaultOrRoot)?.let {
            loadFileList(it)
            setupAdapter()
        }
    }

    private fun fileList(fallbackToDefaultOrRoot: Boolean): Array<File>? {
        var fileList: Array<File>?

        fileList = fileList(nextItem)

        if (fileList == null) { // Try default path

            /* Do not try alternative paths.
             * Used when clicking from already opened browser.
             * Don't do anything in that case.
             */
            if (!fallbackToDefaultOrRoot) {
                return null
            }

            nextItem = defaultPath()
            fileList = fileList(nextItem)

            if (fileList == null) { // Try root
                nextItem = "/"
                fileList = fileList(nextItem)

                if (fileList == null) {
                    fileList = emptyArray()
                }
            }
        }

        return fileList
    }

    private fun loadFileList(files: Array<File>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loading file list for $nextItem")

        nextItem?.let {
            val nextPath = File(it)

            val fileList = files.asList()
            Collections.sort(fileList, SortOrder())

            val icons = getIconResources()

            itemList.clear()

            itemList.add(BrowserItem("Up", icons.first, true))

            for (file in fileList) {
                if (File(nextPath, file.name).isDirectory) {
                    itemList.add(BrowserItem(file.name, icons.third))
                } else {
                    itemList.add(BrowserItem(file.name, icons.second))
                }
            }

            /* Current directory has been updated. */

            currentItem = it

            binding.topToolbar.title = nextPath.absolutePath
        }
    }

    internal inner class SortOrder : Comparator<File> {
        override fun compare(f1: File, f2: File): Int {
            return if (f1.isDirectory && f2.isDirectory || f1.isFile && f2.isFile) {
                // Same type of file by name
                String.CASE_INSENSITIVE_ORDER.compare(f1.name, f2.name)

            } else {
                // Directories first
                if (f1.isDirectory && f2.isFile) -1 else 1
            }
        }
    }

    private fun getIconResources(): Triple<Int, Int, Int> {
        return Triple(
            R.drawable.ic_keyboard_arrow_up,
            R.drawable.ic_insert_drive_file,
            R.drawable.ic_folder_open)
    }

    private fun setupAdapter() {
        // TODO: Must create every time, can we update itemList only?
        val adapter = object : ArrayAdapter<BrowserItem>(this, R.layout.item_browser, itemList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(
                        R.layout.item_browser, parent, false)

                view.findViewById<TextView>(R.id.item_browser_name)
                        .text = itemList[position].name

                view.findViewById<ImageView>(R.id.item_browser_icon)
                        .setImageResource(itemList[position].icon)

                return view
            }
        }

        listView.adapter = adapter
    }

    data class BrowserItem(val name: String, val icon: Int = 0, val isUp: Boolean = false)

    companion object {
        private val TAG: String = BrowserActivity::class.java.name

        const val ARG_STARTING_DIRECTORY = "starting_directory"
        const val ARG_DIRECTORY = "directory"
        const val ARG_IS_FILE_SELECTABLE = "is_file_selectable"

        private val FILENAME_FILTER = FilenameFilter { dir, name ->
            File(dir, name).let {
                (it.isFile || it.isDirectory) && !it.isHidden
            }
        }
    }
}

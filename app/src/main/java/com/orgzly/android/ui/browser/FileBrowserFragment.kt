package com.orgzly.android.ui.browser

import android.os.Bundle
import android.os.Environment
import android.support.annotation.StringRes
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils

import java.io.File
import java.io.FilenameFilter
import java.util.Arrays
import java.util.Collections
import java.util.Comparator

class FileBrowserFragment : BrowserFragment() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tryLoadingFileListFromNext(true)
    }

    override fun setupShortcuts(layout: LinearLayout) {
        layout.removeAllViews()

        // First shortcut changes current directory to default
        createShortcutButton(R.string.primary_storage).let { primaryStorageButton ->
            primaryStorageButton.setOnClickListener { _ ->
                nextItem = defaultPath()
                tryLoadingFileListFromNext(true)
            }

            layout.addView(primaryStorageButton)
        }
    }

    private fun createShortcutButton(@StringRes id: Int): Button {
        val button = Button(context)
        button.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        button.setText(id)

        return button
    }

    override fun onResume() {
        super.onResume()

        /* Check for permissions. */
        AppPermissions.isGrantedOrRequest(activity as CommonActivity, AppPermissions.Usage.LOCAL_REPO)
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val item = l?.getItemAtPosition(position) as BrowserItem

        if (currentItem != null) {
            if (item.isUp) {
                val path = File(currentItem)

                if (path.parentFile != null) {
                    nextItem = path.parentFile.absolutePath
                    tryLoadingFileListFromNext(false)
                }

            } else {
                val sel = File(currentItem, item.name)

                if (sel.isDirectory) {
                    nextItem = sel.absolutePath
                    tryLoadingFileListFromNext(false)
                }
            }

        } else {
            Log.e(TAG, "Clicked on " + item.name + " but there is no current directory set")
        }
    }

    private fun fileList(path: String?): Array<File>? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Trying to get a list of files in " + path!!)

        if (path != null) {
            val file = File(path)

            if (file.exists()) {
                return file.listFiles(FILENAME_FILTER)
            }
        }

        return null
    }

    /**
     * Populates [.mItemList] with non-hidden files and directories from [.mNextItem].
     * Creates a new adapter and uses it for list view.
     */
    private fun tryLoadingFileListFromNext(fallbackToDefaultOrRoot: Boolean) {
        var fileList: Array<File>?

        fileList = fileList(nextItem)

        if (fileList == null) { // Try default path

            /* Do not try alternative paths.
             * Used when clicking from already opened browser.
             * Don't do anything in that case.
             */
            if (!fallbackToDefaultOrRoot) {
                return
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

        doLoadFileListFromNext(fileList)

        setupAdapter()
    }

    private fun doLoadFileListFromNext(files: Array<File>) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loading file list for $nextItem")

        val nextPath = File(nextItem)

        val fileList = Arrays.asList(*files)
        Collections.sort(fileList, FileTypeComparator())

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

        /* Current item updated. */

        currentItemView.text = nextPath.absolutePath

        currentItem?.let { itemsHistory.add(it) }

        currentItem = nextItem
    }

    private fun getIconResources(): Triple<Int, Int, Int> {
        val typedArray = activity?.obtainStyledAttributes(R.styleable.Icons)

        return if (typedArray != null) {
            val triple = Triple(
                    typedArray.getResourceId(R.styleable.Icons_ic_keyboard_arrow_up_24dp, 0),
                    typedArray.getResourceId(R.styleable.Icons_ic_insert_drive_file_24dp, 0),
                    typedArray.getResourceId(R.styleable.Icons_ic_folder_open_24dp, 0)
            )

            typedArray.recycle()

            triple

        } else {
            Triple(0, 0, 0)
        }
    }

    private fun setupAdapter() {
        // TODO: Must create every time, can we update itemList only?
        val adapter = object : ArrayAdapter<BrowserItem>(activity!!, R.layout.item_browser, itemList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(
                        R.layout.item_browser, parent, false)

                val textView = view.findViewById<TextView>(R.id.item_browser_name)
                textView.text = itemList[position].name

                val imageView = view.findViewById<ImageView>(R.id.item_browser_icon)
                imageView.setImageResource(itemList[position].icon)

                return view
            }
        }

        listView.adapter = adapter
    }

    public override fun defaultPath(): String? {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            null
        }
    }

    fun refresh() {
        nextItem = currentItem
        tryLoadingFileListFromNext(false)
    }

    internal inner class FileTypeComparator : Comparator<File> {
        override fun compare(file1: File, file2: File): Int {

            /* Same type. */
            if (file1.isDirectory && file2.isDirectory || file1.isFile && file2.isFile) {
                return String.CASE_INSENSITIVE_ORDER.compare(file1.name, file2.name)
            }

            return if (file1.isDirectory && file2.isFile) -1 else 1
        }
    }

    companion object {
        private val TAG = FileBrowserFragment::class.java.name

        /** Name used for [android.app.FragmentManager]. */
        val FRAGMENT_TAG: String = FileBrowserFragment::class.java.name

        private val FILENAME_FILTER = FilenameFilter { dir, name ->
            File(dir, name).let {
                (it.isFile || it.isDirectory) && !it.isHidden
            }
        }

        fun getInstance(entry: String?): FileBrowserFragment {
            val fragment = FileBrowserFragment()

            if (entry != null) {
                fragment.init(entry)
            }

            return fragment
        }
    }
}

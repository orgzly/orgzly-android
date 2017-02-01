package com.orgzly.android.ui.fragments;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
public class FileBrowserFragment extends BrowserFragment {
    private static final String TAG = FileBrowserFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = FileBrowserFragment.class.getName();

    private static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File f = new File(dir, filename);
            return (f.isFile() || f.isDirectory()) && !f.isHidden();
        }
    };

    public static FileBrowserFragment getInstance(String entry) {
        FileBrowserFragment fragment =  new FileBrowserFragment();

        if (entry != null) {
            fragment.init(entry);
        }

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileBrowserFragment() {
    }

    public String defaultPath() {
        File file = Environment.getExternalStorageDirectory();

        boolean isRW = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Default path " + file + " is " + (isRW ? "" : "NOT ") + "MOUNTED");

        String path = null;

        if (isRW) {
            path = file.getAbsolutePath();
        }

        return path;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        tryLoadFileListFromNext(true);
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        /* Check for permissions. */
        AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Item item = (Item) l.getItemAtPosition(position);

        if (mCurrentItem != null) {
            if (item.isUp) {
                File path = new File(mCurrentItem);

                if (path.getParentFile() != null) {
                    mNextItem = path.getParentFile().getAbsolutePath();
                    tryLoadFileListFromNext(false);
                }

            } else {
                File sel = new File(mCurrentItem, item.name);

                if (sel.isDirectory()) {
                    mNextItem = sel.getAbsolutePath();
                    tryLoadFileListFromNext(false);
                }
            }

        } else {
            Log.e(TAG, "Clicked on " + item.name + " but there is no current directory set");
        }
    }

    private File[] fileList(String path) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Trying to get a list of files in " + path);

        if (path != null) {
            File file = new File(path);

            if (file.exists()) {
                return file.listFiles(FILENAME_FILTER);
            }
        }

        return null;
    }

    /**
     * Populates {@link #mItemList} with non-hidden files and directories from {@link #mNextItem}.
     * Creates a new adapter and uses it for list view.
     */
    private void tryLoadFileListFromNext(boolean fallbackToDefaultOrRoot) {
        File[] fileList;

        fileList = fileList(mNextItem);

        if (fileList == null) { /* Try default path. */

            /* Do not try alternative paths.
             * Used when clicking from already opened browser.
             * Don't do anything in that case.
             */
            if (! fallbackToDefaultOrRoot) {
                return;
            }

            mNextItem = defaultPath();
            fileList = fileList(mNextItem);

            if (fileList == null) { /* Try root. */
                mNextItem = "/";
                fileList = fileList(mNextItem);

                if (fileList == null) {
                    fileList = new File[0];
                }
            }
        }

        doLoadFileListFromNext(fileList);

        setupAdapter();
    }

    private void doLoadFileListFromNext(File[] files) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Loading file list for " + mNextItem);

        File nextPath = new File(mNextItem);

        List<File> list = Arrays.asList(files);
        Collections.sort(list, new FileTypeComparator());

        TypedArray typedArray = getActivity().obtainStyledAttributes(new int[]{
                R.attr.oic_browser_up, R.attr.oic_browser_file, R.attr.oic_browser_directory});

        mItemList = new Item[list.size() + 1];

        mItemList[0] = new Item(typedArray.getResourceId(0, 0));

        for (int i = 0; i < list.size(); i++) {
            mItemList[i+1] = new Item(typedArray.getResourceId(1, 0), list.get(i).getName());

            /* Update icon if it's a directory. */
            File sel = new File(nextPath, list.get(i).getName());
            if (sel.isDirectory()) {
                mItemList[i+1].icon = typedArray.getResourceId(2, 0);
            }
        }

        typedArray.recycle();
        /* Current item updated. */
        mCurrentItemView.setText(nextPath.getAbsolutePath());
        mItemHistory.add(mCurrentItem);
        mCurrentItem = mNextItem;
    }

    private void setupAdapter() {
        // TODO: Must create every time, can we update itemList only?
        ListAdapter adapter = new ArrayAdapter<Item>(getActivity(), R.layout.item_browser, mItemList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // View view = super.getView(position, convertView, parent);

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_browser, parent, false);
                }

                ImageView imageView = (ImageView) convertView.findViewById(R.id.browser_item_icon);
                TextView textView = (TextView) convertView.findViewById(R.id.browser_item_name);

                imageView.setImageResource(mItemList[position].icon);
                textView.setText(mItemList[position].name);

                return convertView;
            }
        };

        getListView().setAdapter(adapter);
    }

    public void refresh() {
        mNextItem = mCurrentItem;
        tryLoadFileListFromNext(false);
    }

    class FileTypeComparator implements Comparator<File> {
        @Override
        public int compare(File file1, File file2) {

            /* Same type. */
            if ((file1.isDirectory() && file2.isDirectory()) || (file1.isFile() && file2.isFile())) {
                return String.CASE_INSENSITIVE_ORDER.compare(file1.getName(), file2.getName());
            }

            if (file1.isDirectory() && file2.isFile()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

}

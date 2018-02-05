package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UriUtils;

/**
 * Displays user-configurable repositories.
 */
public class ReposFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ReposFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = ReposFragment.class.getName();

    private SimpleCursorAdapter mListAdapter;
    private ReposFragmentListener mListener;

    private ViewFlipper mViewFlipper;

    public static ReposFragment getInstance() {
        return new ReposFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReposFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);

        setupAdapter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (ReposFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + ReposFragmentListener.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repos, container, false);

        mViewFlipper = view.findViewById(R.id.fragment_repos_flipper);

        /* Hide or setup new Dropbox repo button. */
        View newDropboxRepoButton = view.findViewById(R.id.fragment_repos_dropbox);
        if (BuildConfig.IS_DROPBOX_ENABLED) {
            newDropboxRepoButton.setOnClickListener(v -> mListener.onRepoNewRequest(R.id.repos_options_menu_item_new_dropbox));
        } else {
            newDropboxRepoButton.setVisibility(View.GONE);
        }

        view.findViewById(R.id.fragment_repos_directory).setOnClickListener(v ->
                mListener.onRepoNewRequest(R.id.repos_options_menu_item_new_external_storage_directory));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* Request callbacks for Context menu. */
        registerForContextMenu(getListView());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mListener != null) {
            mListener.onRepoEditRequest(id);
        }
    }

    private void setupAdapter() {
        /* Column field names to be bound. */
        String[] columns = {
                ProviderContract.Repos.Param.REPO_URL
        };

        /* Views which the data will be bound to. */
        int[] to = {
                R.id.item_repo_url
        };

        /* Create adapter using Cursor. */
        mListAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.item_repo,
                null,
                columns,
                to,
                0);

        mListAdapter.setViewBinder((view, cursor, columnIndex) -> {
            TextView textView;

            switch (view.getId()) {
                case R.id.item_repo_url:
                    if (! cursor.isNull(columnIndex)) {
                        textView = (TextView) view;
                        textView.setText(cursor.getString(columnIndex));
                    }
                    return true;
            }
            return false;
        });

        setListAdapter(mListAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        /* Delay to avoid brief displaying of no-repos view. */
        new android.os.Handler().postDelayed(() -> {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.getSupportLoaderManager()
                        .initLoader(Loaders.REPOS_FRAGMENT, null, ReposFragment.this);
            }

        }, 100);
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        /* Close soft keyboard. Required when coming back from one of the RepoFragment fragments. */
        ActivityUtils.closeSoftKeyboard(getActivity());
    }

    /**
     * Callback for options menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        // Do not display add icon if there are no repositories - large repo buttons will be shown
        if (mListAdapter == null || mListAdapter.getCount() > 0) {
            inflater.inflate(R.menu.repos_actions, menu);

            // Remove Dropbox from the menu
            if (!BuildConfig.IS_DROPBOX_ENABLED) {
                menu.findItem(R.id.repos_options_menu_item_new).getSubMenu()
                        .removeItem(R.id.repos_options_menu_item_new_dropbox);
            }
        }
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.repos_options_menu_item_new_dropbox:
                mListener.onRepoNewRequest(item.getItemId());
                return true;

            case R.id.repos_options_menu_item_new_external_storage_directory:
                mListener.onRepoNewRequest(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

   /*
    * Context menu.
    */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.repos_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
		/* Get ID of the item. */
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case R.id.repos_context_menu_delete:
                mListener.onRepoDeleteRequest(info.id);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(
                getActivity(),
                ProviderContract.Repos.ContentUri.repos(),
                null,
                null,
                null,
                ProviderContract.Repos.Param.REPO_URL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        /*
         * Swapping instead of changing Cursor here, to keep the old one open.
         * Loader should release the old Cursor - see note in
         * {@link LoaderManager.LoaderCallbacks#onLoadFinished).
         */
        mListAdapter.swapCursor(cursor);

        if (mListAdapter.getCount() > 0) {
            mViewFlipper.setDisplayedChild(0);
        } else {
            mViewFlipper.setDisplayedChild(1);
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mListAdapter.changeCursor(null);
    }

    public interface ReposFragmentListener {
        void onRepoNewRequest(int id);
        void onRepoDeleteRequest(long id);
        void onRepoEditRequest(long id);
    }
}

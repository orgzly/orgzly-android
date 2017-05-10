package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ViewFlipper;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.Fab;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.Loaders;
import com.orgzly.android.ui.util.ListViewUtils;
import com.orgzly.android.util.LogUtils;

import java.util.Set;

/**
 * Displays and allows modifying saved filters.
 */
public class FiltersFragment extends ListFragment implements Fab, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = FiltersFragment.class.getName();

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = FiltersFragment.class.getName();

    private SimpleCursorAdapter mListAdapter;

    private FiltersFragmentListener mListener;

    private boolean mIsViewCreated = false;

    private ViewFlipper mViewFlipper;

    private ActionMode mActionMode;

    public static FiltersFragment getInstance() {
        return new FiltersFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FiltersFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupAdapter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (FiltersFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + FiltersFragmentListener.class);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filters, container, false);

        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_filters_flipper);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mIsViewCreated = true;

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new MyActionMode());
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onDestroyView();

        mIsViewCreated = false;

        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mListener != null) {
            mListener.onFilterEditRequest(id);
        }
    }

    private void setupAdapter() {
        /* Create adapter using Cursor. */
        mListAdapter = createFilterCursorAdapter(getActivity());

        setListAdapter(mListAdapter);
    }

    public static SimpleCursorAdapter createFilterCursorAdapter(Context context) {
        /* Column field names to be bound. */
        String[] columns = new String[] {
                ProviderContract.Filters.Param.NAME,
                ProviderContract.Filters.Param.QUERY,
                ProviderContract.Filters.Param.POSITION,
        };

        /* Views which the data will be bound to. */
        int[] to = new int[] {
                R.id.item_filter_name,
                R.id.item_filter_query,
                R.id.item_filter_position,
        };

        /* Create adapter using Cursor. */
        return new SimpleCursorAdapter(
                context,
                R.layout.item_filter,
                null,
                columns,
                to,
                0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        getActivity().getSupportLoaderManager().initLoader(Loaders.FILTERS_FRAGMENT, null, this);
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return FiltersClient.getCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (mIsViewCreated) {
            /**
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
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (mIsViewCreated) {
            mListAdapter.changeCursor(null);
        }
    }

    @Override
    public Runnable getFabAction() {
        return new Runnable() {
            @Override
            public void run() {
                mListener.onFilterNewRequest();
            }
        };
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    FRAGMENT_TAG,
                    getString(R.string.searches),
                    null,
                    getListView().getCheckedItemCount());
        }
    }

    public class MyActionMode implements AbsListView.MultiChoiceModeListener {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getListView().getCheckedItemCount());

            mActionMode = mode;

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.filters_cab, menu);

            /* Needed for after orientation change. */
            mode.setTitle(String.valueOf(getListView().getCheckedItemCount()));

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, getListView().getCheckedItemCount());

            if (mode.getTag() != null) {
                menu.findItem(R.id.filters_cab_move_up).setVisible(false);
                menu.findItem(R.id.filters_cab_move_down).setVisible(false);

            } else {
                menu.findItem(R.id.filters_cab_move_up).setVisible(true);
                menu.findItem(R.id.filters_cab_move_up).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

                menu.findItem(R.id.filters_cab_move_down).setVisible(true);
                menu.findItem(R.id.filters_cab_move_down).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Set<Long> ids = ListViewUtils.getCheckedIds(getListView());

            switch (item.getItemId()) {
                case R.id.filters_cab_move_up:
                    mListener.onFilterMoveUpRequest(ids.iterator().next());
                    break;

                case R.id.filters_cab_move_down:
                    mListener.onFilterMoveDownRequest(ids.iterator().next());
                    break;

                case R.id.filters_cab_delete:
                    mListener.onFilterDeleteRequest(ids);

                    /* Close action mode. */
                    mode.finish();

                    break;

                default:
                    return false; /* Not handled. */
            }

            return true; /* Handled. */
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getListView().clearChoices();

            announceChangesToActivity();

            mActionMode = null;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mode.setTitle(String.valueOf(getListView().getCheckedItemCount()));

            /*
             * Request onPrepareActionMode to be called if actions for
             * repositioning need to be added to removed.
             */
            if (getListView().getCheckedItemCount() > 1) {
                if (mode.getTag() == null) { /* Filter repositioning actions exist. */
                    mode.setTag(new Object());
                    mode.invalidate();
                }
            } else {
                if (mode.getTag() != null) { /* Filter repositioning actions do not exist. */
                    mode.setTag(null);
                    mode.invalidate();
                }
            }

            announceChangesToActivity();
        }
    }

    public interface FiltersFragmentListener extends FragmentListener {
        void onFilterNewRequest();
        void onFilterDeleteRequest(Set<Long> ids);
        void onFilterEditRequest(long id);
        void onFilterMoveUpRequest(long id);
        void onFilterMoveDownRequest(long id);
    }
}

package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ViewFlipper;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.filter.Filter;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

public class FilterFragment extends Fragment implements DrawerItem {
    private static final String TAG = FilterFragment.class.getName();

    private static final String ARG_ID = "id";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = FilterFragment.class.getName();

    private FilterFragmentListener mListener;

    private ViewFlipper mViewFlipper;

    private TextInputLayout nameInputLayout;
    private EditText mName;

    private TextInputLayout queryInputLayout;
    private EditText mQuery;

    public static FilterFragment getInstance() {
        return new FilterFragment();
    }

    public static FilterFragment getInstance(long id) {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_ID, id);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FilterFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);

        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_filter_flipper);
        nameInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_filter_name_input_layout);
        mName = (EditText) view.findViewById(R.id.fragment_filter_name);
        queryInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_filter_query_input_layout);
        mQuery = (EditText) view.findViewById(R.id.fragment_filter_query);

        setViewsFromArgument();

        return view;
    }

    private void setViewsFromArgument() {
        View viewToFocus = null;

        if (isEditingExistingFilter()) { /* Existing filter. */
            long id = getArguments().getLong(ARG_ID);

            Filter filter = FiltersClient.get(getActivity(), id);

            if (filter != null) {
                mName.setText(filter.getName());
                mQuery.setText(filter.getQuery());

                mViewFlipper.setDisplayedChild(0);

                viewToFocus = mQuery;

            } else {
                mViewFlipper.setDisplayedChild(1);
            }

        } else { /* New filter. */
            viewToFocus = mName;
        }

        /*
         * Open a soft keyboard.
         * For new filters focus on name, for existing focus on query.
         */
        if (viewToFocus != null && getActivity() != null) {
            ActivityUtils.openSoftKeyboard(getActivity(), viewToFocus);
        }
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        announceChangesToActivity();
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    FRAGMENT_TAG,
                    getString(isEditingExistingFilter() ? R.string.search : R.string.new_search),
                    null,
                    0);
        }
    }

    private boolean isEditingExistingFilter() {
        return getArguments() != null && getArguments().containsKey(ARG_ID);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (FilterFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + FilterFragmentListener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Callback for options menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.close_done, menu);

        /* Remove search item. */
        menu.removeItem(R.id.activity_action_search);
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                if (mListener != null) {
                    mListener.onFilterCancelRequest();
                }

                return true;

            case R.id.done:
                save();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Sends current values to listener.
     */
    private void save() {
        Filter filter = validateFields();

        if (filter != null) {
            if (isEditingExistingFilter()) {
                long id = getArguments().getLong(ARG_ID);
                if (mListener != null) {
                    mListener.onFilterUpdateRequest(id, filter);
                }
            } else {
                if (mListener != null) {
                    mListener.onFilterCreateRequest(filter);
                }
            }
        }
    }

    private Filter validateFields() {
        String name = mName.getText().toString().trim();
        String query = mQuery.getText().toString().trim();

        boolean isValid = true;

        /* Validate name. */
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError(getString(R.string.can_not_be_empty));
            isValid = false;
        } else if (sameNameFilterExists(name)) {
            nameInputLayout.setError(getString(R.string.filter_name_already_exists));
            isValid = false;
        } else {
            nameInputLayout.setError(null);
        }

        /* Validate query. */
        if (TextUtils.isEmpty(query)) {
            queryInputLayout.setError(getString(R.string.can_not_be_empty));
            isValid = false;
        } else {
            queryInputLayout.setError(null);
        }

        if (!isValid) {
            return null;
        }

        return new Filter(name, query);
    }

    /**
     * Checks if filter with the same name (ignoring case) already exists.
     */
    private boolean sameNameFilterExists(String name) {
        LongSparseArray<Filter> filters = FiltersClient.getByNameIgnoreCase(getContext(), name);

        if (isEditingExistingFilter()) {
            long id = getArguments().getLong(ARG_ID);

            for (int i = 0; i < filters.size(); i++) {
                long filterId = filters.keyAt(i);
                Filter filter = filters.get(filterId);

                // Ignore currently edited filter
                if (name.equalsIgnoreCase(filter.getName()) && id != filterId) {
                    return true;
                }
            }

            return false;

        } else { // New filter
            return filters.size() > 0;
        }
    }


    @Override
    public String getCurrentDrawerItemId() {
        return FiltersFragment.getDrawerItemId();
    }

    public interface FilterFragmentListener extends FragmentListener {
        void onFilterCreateRequest(Filter filter);
        void onFilterUpdateRequest(long id, Filter filter);
        void onFilterCancelRequest();
    }
}

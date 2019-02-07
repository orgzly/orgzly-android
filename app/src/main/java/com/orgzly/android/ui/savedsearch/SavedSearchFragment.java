package com.orgzly.android.ui.savedsearch;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.FragmentActivity;
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
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.main.SharedMainActivityViewModel;
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

public class SavedSearchFragment extends DaggerFragment implements DrawerItem {
    private static final String TAG = SavedSearchFragment.class.getName();

    private static final String ARG_ID = "id";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SavedSearchFragment.class.getName();

    private Listener mListener;

    private ViewFlipper mViewFlipper;

    private TextInputLayout nameInputLayout;
    private EditText mName;

    private TextInputLayout queryInputLayout;
    private EditText mQuery;

    private SavedSearch savedSearch;

    @Inject
    DataRepository dataRepository;

    private SharedMainActivityViewModel sharedMainActivityViewModel;

    public static SavedSearchFragment getInstance() {
        return new SavedSearchFragment();
    }

    public static SavedSearchFragment getInstance(long id) {
        SavedSearchFragment fragment = new SavedSearchFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_ID, id);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SavedSearchFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            sharedMainActivityViewModel = ViewModelProviders.of(activity).get(SharedMainActivityViewModel.class);
        } else {
            throw new IllegalStateException("No Activity");
        }

        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater  inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_search, container, false);

        mViewFlipper = (ViewFlipper) view.findViewById(R.id.fragment_saved_search_flipper);
        nameInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_saved_search_name_input_layout);
        mName = (EditText) view.findViewById(R.id.fragment_saved_search_name);
        queryInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_saved_search_query_input_layout);
        mQuery = (EditText) view.findViewById(R.id.fragment_saved_search_query);

        setViewsFromArgument();

        return view;
    }

    private void setViewsFromArgument() {
        View viewToFocus = null;

        if (isEditingExistingFilter()) { /* Existing filter. */
            long id = getArguments().getLong(ARG_ID);

            savedSearch = dataRepository.getSavedSearch(id);

            if (savedSearch != null) {
                mName.setText(savedSearch.getName());
                mQuery.setText(savedSearch.getQuery());

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
        sharedMainActivityViewModel.setFragment(
                FRAGMENT_TAG,
                getString(isEditingExistingFilter() ? R.string.search : R.string.new_search),
                null,
                0);
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
            mListener = (Listener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + Listener.class);
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
                    mListener.onSavedSearchCancelRequest();
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
        SavedSearch savedSearch = validateSavedSearch();
        if (savedSearch != null) {
            if (isEditingExistingFilter()) {
                if (mListener != null) {
                    mListener.onSavedSearchUpdateRequest(savedSearch);
                }
            } else {
                if (mListener != null) {
                    mListener.onSavedSearchCreateRequest(savedSearch);
                }
            }
        }
    }

    private SavedSearch validateSavedSearch() {
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

        if (isEditingExistingFilter()) {
            return new SavedSearch(savedSearch.getId(), name, query, savedSearch.getPosition());
        } else {
            return new SavedSearch(0, name, query, 0);
        }
    }

    /**
     * Checks if filter with the same name (ignoring case) already exists.
     */
    private boolean sameNameFilterExists(String name) {
        List<SavedSearch> savedSearches = dataRepository.getSavedSearchesByNameIgnoreCase(name);

        if (isEditingExistingFilter()) {
            long id = getArguments().getLong(ARG_ID);

            for (SavedSearch savedSearch: savedSearches) {
                long savedSearchId = savedSearch.getId();
                String savedSearchName = savedSearch.getName();

                // Ignore currently edited filter
                if (name.equalsIgnoreCase(savedSearchName) && id != savedSearchId) {
                    return true;
                }
            }

            return false;

        } else { // New filter
            return savedSearches.size() > 0;
        }
    }


    @Override
    public String getCurrentDrawerItemId() {
        return SavedSearchesFragment.getDrawerItemId();
    }

    public interface Listener {
        void onSavedSearchCreateRequest(SavedSearch savedSearch);
        void onSavedSearchUpdateRequest(SavedSearch savedSearch);
        void onSavedSearchCancelRequest();
    }
}

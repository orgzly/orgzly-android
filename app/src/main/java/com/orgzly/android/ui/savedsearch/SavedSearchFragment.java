package com.orgzly.android.ui.savedsearch;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.main.SharedMainActivityViewModel;
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.databinding.FragmentSavedSearchBinding;

import java.util.List;

import javax.inject.Inject;

public class SavedSearchFragment extends Fragment implements DrawerItem {
    private static final String TAG = SavedSearchFragment.class.getName();

    private static final String ARG_ID = "id";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = SavedSearchFragment.class.getName();

    private FragmentSavedSearchBinding binding;

    private Listener mListener;

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

        sharedMainActivityViewModel = new ViewModelProvider(requireActivity())
                .get(SharedMainActivityViewModel.class);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedSearchBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View viewToFocus = null;

        if (isEditingExistingFilter()) { /* Existing filter. */
            long id = getArguments().getLong(ARG_ID);

            savedSearch = dataRepository.getSavedSearch(id);

            if (savedSearch != null) {
                binding.fragmentSavedSearchName.setText(savedSearch.getName());
                binding.fragmentSavedSearchQuery.setText(savedSearch.getQuery());

                binding.fragmentSavedSearchFlipper.setDisplayedChild(0);

                viewToFocus = binding.fragmentSavedSearchQuery;

            } else {
                binding.fragmentSavedSearchFlipper.setDisplayedChild(1);
            }

        } else { /* New filter. */
            viewToFocus = binding.fragmentSavedSearchName;
        }

        /*
         * Open a soft keyboard.
         * For new filters focus on name, for existing focus on query.
         */
        Activity activity = getActivity();
        if (viewToFocus != null && activity != null) {
            ActivityUtils.openSoftKeyboardWithDelay(activity, viewToFocus);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

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

        App.appComponent.inject(this);

        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (Listener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity().toString() + " must implement " + Listener.class);
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

        // Remove search item.
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
        String name = binding.fragmentSavedSearchName.getText().toString().trim();
        String query = binding.fragmentSavedSearchQuery.getText().toString().trim();

        boolean isValid = true;

        /* Validate name. */
        if (TextUtils.isEmpty(name)) {
            binding.fragmentSavedSearchNameInputLayout.setError(getString(R.string.can_not_be_empty));
            isValid = false;
        } else if (sameNameFilterExists(name)) {
            binding.fragmentSavedSearchNameInputLayout.setError(getString(R.string.filter_name_already_exists));
            isValid = false;
        } else {
            binding.fragmentSavedSearchNameInputLayout.setError(null);
        }

        /* Validate query. */
        if (TextUtils.isEmpty(query)) {
            binding.fragmentSavedSearchQueryInputLayout.setError(getString(R.string.can_not_be_empty));
            isValid = false;
        } else {
            binding.fragmentSavedSearchQueryInputLayout.setError(null);
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

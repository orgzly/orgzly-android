package com.orgzly.android.ui.savedsearch;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.ui.CommonFragment;
import com.orgzly.android.ui.drawer.DrawerItem;
import com.orgzly.android.ui.main.SharedMainActivityViewModel;
import com.orgzly.android.ui.savedsearches.SavedSearchesFragment;
import com.orgzly.android.ui.util.KeyboardUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.databinding.FragmentSavedSearchBinding;

import java.util.List;

import javax.inject.Inject;

public class SavedSearchFragment extends CommonFragment implements DrawerItem {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSavedSearchBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        if (viewToFocus != null) {
            KeyboardUtils.openSoftKeyboard(viewToFocus);
        }

        topToolbarToDefault();
    }

    private void topToolbarToDefault() {
        binding.topToolbar.setNavigationOnClickListener(v -> close());

        binding.topToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.done:
                    save();
                    return true;
            }

            return false;
        });

        binding.topToolbar.setOnClickListener(v -> binding.scrollView.scrollTo(0, 0));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        sharedMainActivityViewModel.setCurrentFragment(FRAGMENT_TAG);

        sharedMainActivityViewModel.lockDrawer();
    }

    @Override
    public void onPause() {
        super.onPause();

        sharedMainActivityViewModel.unlockDrawer();
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

    private void close() {
        if (mListener != null) {
            mListener.onSavedSearchCancelRequest();
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

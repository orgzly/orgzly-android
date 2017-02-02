package com.orgzly.android.ui.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;


public class DirectoryRepoFragment extends RepoFragment {
    private static final String TAG = DirectoryRepoFragment.class.getName();

    private static final String ARG_REPO_ID = "repo_id";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = DirectoryRepoFragment.class.getName();

    private DirectoryRepoFragmentListener mListener;

    private Uri mSelectedUri;
    private TextInputLayout directoryInputLayout;
    private EditText mUriView;

    public static DirectoryRepoFragment getInstance() {
        return new DirectoryRepoFragment();
    }

    public static DirectoryRepoFragment getInstance(long repoId) {
        DirectoryRepoFragment fragment = new DirectoryRepoFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_REPO_ID, repoId);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DirectoryRepoFragment() {
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
        View view = inflater.inflate(R.layout.fragment_repo_directory, container, false);

        directoryInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_repo_directory_input_layout);

        mUriView = (EditText) view.findViewById(R.id.fragment_repo_directory);

        // Not working when done in XML
        mUriView.setHorizontallyScrolling(false);
        mUriView.setMaxLines(3);

        mUriView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                save();
                return true;
            }
        });

        MiscUtils.clearErrorOnTextChange(mUriView, directoryInputLayout);

        view.findViewById(R.id.fragment_repo_directory_browse_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Close the keyboard before opening the browser. */
                if (getActivity() != null) {
                    ActivityUtils.closeSoftKeyboard(getActivity());
                }

                /* Do not open the browser unless we have the storage permission. */
                if (AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO)) {
                    startBrowserDelayed();
                }
            }
        });

        if (savedInstanceState == null && TextUtils.isEmpty(mUriView.getText()) && mSelectedUri == null) {
            setFromArgument();
        }

        return view;
    }

    private void setFromArgument() {
        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) {
            long repoId = getArguments().getLong(ARG_REPO_ID);

            mSelectedUri = Uri.parse(ReposClient.getUrl(getActivity(), repoId));
        }
    }

    /**
     * Delay opening the browser.
     * Buttons would briefly appear in the middle of the screen
     * because of the opened keyboard.
     */
    private void startBrowserDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startBrowser();
            }
        }, 100);
    }

    private void startBrowser() {
        String uri = null;

        if (! TextUtils.isEmpty(mUriView.getText())) {
            uri = mUriView.getText().toString();
        }

        if (uri != null) {
            mListener.onBrowseDirectories(Uri.parse(uri).getPath());
        } else {
            mListener.onBrowseDirectories(null);
        }
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        /* Set directory view's value. */
        if (mSelectedUri != null) {
            mUriView.setText(mSelectedUri.toString());
            mSelectedUri = null;
        }

        /* Check for permissions. */
        AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (DirectoryRepoFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + DirectoryRepoFragmentListener.class);
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

        inflater.inflate(R.menu.done_or_close, menu);

        /* Remove search item. */
        // menu.removeItem(R.id.options_menu_item_search);
    }

    /**
     * Callback for options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                if (mListener != null) {
                    mListener.onRepoCancelRequest();
                }
                return true;

            case R.id.done:
                save();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void save() {
        /* Check for storage permission. */
        if (! AppPermissions.isGrantedOrRequest((CommonActivity) getActivity(), AppPermissions.FOR_LOCAL_REPO)) {
            return;
        }

        String uriString = mUriView.getText().toString().trim();

        if (TextUtils.isEmpty(uriString)) {
            directoryInputLayout.setError(getString(R.string.can_not_be_empty));
            return;
        } else {
            directoryInputLayout.setError(null);
        }

        Uri uri = Uri.parse(uriString);

        Repo repo = RepoFactory.getFromUri(getActivity(), uri);

        if (repo == null) {
            directoryInputLayout.setError(getString(R.string.invalid_repo_url, uri));
            return;
        }

        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) { // Existing repo
            long repoId = getArguments().getLong(ARG_REPO_ID);

            if (mListener != null) {
                mListener.onRepoUpdateRequest(repoId, repo);
            }

        } else {
            if (mListener != null) {
                mListener.onRepoCreateRequest(repo);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.repos_context, menu);
    }

    public void updateUri(Uri uri) {
        mSelectedUri = uri;
    }

    public interface DirectoryRepoFragmentListener extends RepoFragmentListener {
        void onBrowseDirectories(String dir);
    }
}

package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.DropboxRepo;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;

public class DropboxRepoFragment extends RepoFragment {
    private static final String TAG = DropboxRepoFragment.class.getName();

    private static final String ARG_REPO_ID = "repo_id";

    /** Name used for {@link android.app.FragmentManager}. */
    public static final String FRAGMENT_TAG = DropboxRepoFragment.class.getName();

    private DropboxRepoFragmentListener mListener;

    private ImageView mDropboxIcon;
    private Button mDropboxLinkUnlinkButton;

    private TextInputLayout directoryInputLayout;
    private EditText mDirectory;

    public static DropboxRepoFragment getInstance() {
        return new DropboxRepoFragment();
    }

    public static DropboxRepoFragment getInstance(long repoId) {
        DropboxRepoFragment fragment = new DropboxRepoFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_REPO_ID, repoId);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DropboxRepoFragment() {
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
        View view = inflater.inflate(R.layout.fragment_repo_dropbox, container, false);

        /* Dropbox link / unlink button. */
        mDropboxLinkUnlinkButton = (Button) view.findViewById(R.id.fragment_repo_dropbox_link_button);
        mDropboxLinkUnlinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener.isDropboxLinked()) {
                    areYouSureYouWantToUnlink();
                } else {
                    toogleLink();
                }
            }
        });

        mDropboxIcon = (ImageView) view.findViewById(R.id.fragment_repo_dropbox_icon);

        mDirectory = (EditText) view.findViewById(R.id.fragment_repo_dropbox_directory);

        // Not working when done in XML
        mDirectory.setHorizontallyScrolling(false);
        mDirectory.setMaxLines(3);

        mDirectory.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                save();
                return true;
            }
        });

        setDirectoryFromArgument();

        directoryInputLayout = (TextInputLayout) view.findViewById(R.id.fragment_repo_dropbox_directory_input_layout);

        MiscUtils.clearErrorOnTextChange(mDirectory, directoryInputLayout);

        /* Open a soft keyboard. */
        if (getActivity() != null) {
            ActivityUtils.openSoftKeyboard(getActivity(), mDirectory);
        }

        return view;
    }

    private void areYouSureYouWantToUnlink() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    toogleLink();
                }
            }
        };

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_unlinking_from_dropbox_title)
                .setMessage(R.string.confirm_unlinking_from_dropbox_message)
                .setPositiveButton(R.string.unlink, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show();
    }

    private void toogleLink() {
        if (mListener != null) {
            if (mListener.onDropboxLinkToggleRequest()) { // Unlinked
                updateDropboxLinkUnlinkButton();
            } // Else - Linking process started - button should stay the same.
        }
    }

    private void setDirectoryFromArgument() {
        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) {
            long repoId = getArguments().getLong(ARG_REPO_ID);

            Uri repoUri = Uri.parse(ReposClient.getUrl(getActivity(), repoId));

            mDirectory.setText(repoUri.getPath());
        }
    }

    @Override
    public void onResume() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);
        super.onResume();

        updateDropboxLinkUnlinkButton();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (DropboxRepoFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + DropboxRepoFragmentListener.class);
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
        String directory = mDirectory.getText().toString().trim();

        if (TextUtils.isEmpty(directory)) {
            directoryInputLayout.setError(getString(R.string.can_not_be_empty));
            return;
        } else {
            directoryInputLayout.setError(null);
        }

        Uri uri = UriUtils.uriFromPath(DropboxRepo.SCHEME, directory);

        Repo repo = RepoFactory.getFromUri(getActivity(), uri);

        if (repo == null) {
            directoryInputLayout.setError(getString(R.string.invalid_repo_url, uri));
            return;
        }

        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) {
            /* Edit existing repository. */
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

    public void updateDropboxLinkUnlinkButton() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        Activity activity = getActivity();

        if (mListener != null && activity != null) {
            TypedArray typedArray = activity.obtainStyledAttributes(R.styleable.Icons);

            String text;
            int imageResource;

            if (mListener.isDropboxLinked()) {
                text = getString(R.string.repo_dropbox_button_linked);
                imageResource = typedArray.getResourceId(R.styleable.Icons_oic_dropbox_linked, 0);
            } else {
                text = getString(R.string.repo_dropbox_button_not_linked);
                imageResource = typedArray.getResourceId(R.styleable.Icons_oic_dropbox_not_linked, 0);
            }

            typedArray.recycle();

            mDropboxLinkUnlinkButton.setText(text);

            if (imageResource != 0) {
                mDropboxIcon.setImageResource(imageResource);
            }
        }
    }


    public interface DropboxRepoFragmentListener extends RepoFragmentListener {
        boolean onDropboxLinkToggleRequest();
        boolean isDropboxLinked();
    }
}

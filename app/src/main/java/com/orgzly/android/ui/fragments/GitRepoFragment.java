package com.orgzly.android.ui.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.prefs.RepoPreferences;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

public class GitRepoFragment extends RepoFragment {
    private static final String TAG = DirectoryRepoFragment.class.getName();
    public static final String FRAGMENT_TAG = GitRepoFragment.class.getName();
    private static final String ARG_REPO_ID = "repo_id";

    private EditText uriText;
    private FileSelectionFragment directoryFragment;
    private FileSelectionFragment sshKeyFragment;
    private EditText gitAuthorText;
    private EditText gitEmailText;
    private View view;
    private long repoId;
    private RepoPreferences repoPreferences;
    private GitRepoFragmentListener mListener;

    public static GitRepoFragment getInstance() {
        return new GitRepoFragment();
    }

    public GitRepoFragment() {
        repoId = -1;
    }

    private void setFromArgument() {
        if (getArguments() != null && getArguments().containsKey(ARG_REPO_ID)) {
            repoId = getArguments().getLong(ARG_REPO_ID);
            uriText.setText(ReposClient.getUrl(getActivity(), repoId));
            setFromPreferences();
        }
    }

    private void setFromPreferences() {
        directoryFragment.setValue(
                repoPreferences().getStringValue(R.string.pref_key_git_repository_filepath, ""));
    }

    private RepoPreferences repoPreferences() {
        if (repoPreferences == null && repoId >= 0)
            repoPreferences = new RepoPreferences(getActivity().getApplicationContext(), repoId);
        return repoPreferences;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_repo_git, container, false);
        }

        uriText = (EditText) view.findViewById(R.id.fragment_repo_remote_address);
        gitAuthorText = (EditText) view.findViewById(R.id.fragment_repo_git_author);
        gitEmailText = (EditText) view.findViewById(R.id.fragment_repo_git_email);
        directoryFragment = (FileSelectionFragment) getChildFragmentManager().
                findFragmentById(R.id.fragment_repo_git_directory);
        sshKeyFragment = (FileSelectionFragment) getChildFragmentManager().
                findFragmentById(R.id.fragment_repo_ssh_key_filepath);

        directoryFragment.setHint(R.string.fragment_repo_external_storage_directory_desc);
        sshKeyFragment.setHint(R.string.fragment_repo_ssh_key_location_desc);

        if (savedInstanceState == null) {
            setFromArgument();
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, menu, inflater);

        inflater.inflate(R.menu.done_or_close, menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Would like to add items to the Options Menu.
         * Required (for fragments only) to receive onCreateOptionsMenu() call.
         */
        setHasOptionsMenu(true);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.repos_context, menu);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /* This makes sure that the container activity has implemented
         * the callback interface. If not, it throws an exception
         */
        try {
            mListener = (GitRepoFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement " + GitRepoFragmentListener.class);
        }
    }

    private void save() {
        String uri = uriText.getText().toString();
        if (repoId < 0) {
            if (uri.length() == 0) return;
            if (ReposClient.getId(getActivity(), uri) != 0)
                return; // TODO: throw an exception/handle this case
            ReposClient.insert(getActivity(), uri);
            repoId = ReposClient.getId(getActivity(), uri);
        }

        boolean commitSuccess =
                new PreferenceStringWriter(getActivity(), repoPreferences().getRepoPreferences().edit()).
                putString(R.string.pref_key_git_repository_filepath, directoryFragment.getValue()).
                putString(R.string.pref_key_git_ssh_key_path, sshKeyFragment.getValue()).
                putString(R.string.pref_key_git_author, gitAuthorText.getText().toString()).
                putString(R.string.pref_key_git_email, gitEmailText.getText().toString()).editor.commit();

        ReposClient.updateUrl(getActivity(), repoId, uri);
    }

    class PreferenceStringWriter {
        SharedPreferences.Editor editor;
        Context context;
        PreferenceStringWriter(Context c, SharedPreferences.Editor e) {
            context = c;
            editor = e;
        }

        PreferenceStringWriter putString(int preference, String value) {
            SharedPreferences.Editor newEditor;
            String setting = getSettingName(preference);
            if (value.length() > 0) {
                newEditor = editor.putString(setting, value);
            } else {
                newEditor = editor.remove(setting);
            }
            return new PreferenceStringWriter(context, newEditor);
        }

        private String getSettingName(int setting) {
            return context.getResources().getString(setting);
        }
    }

    public interface GitRepoFragmentListener extends RepoFragmentListener {}
}

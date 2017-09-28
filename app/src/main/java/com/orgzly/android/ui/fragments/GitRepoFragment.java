package com.orgzly.android.ui.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
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
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.git.GitSSHKeyTransportSetter;
import com.orgzly.android.git.GitTransportSetter;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.RepoPreferences;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.GitRepo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.AppPermissions;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GitRepoFragment extends RepoFragment implements GitPreferences {
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
                startSave();
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

    private void startSave() {
        new RepoCloneTask(this).execute();
    }

    private void repoCheckComplete(IOException e) {
        if (e == null) {
            save();
            return;
        }
        showError(e.toString());
        e.printStackTrace();
    }

    private void showError(String errorString) {
        Log.e("Clone", errorString);
        Snackbar.make(view, errorString, Snackbar.LENGTH_LONG);
    }

    private void save() {
        String remoteUriString = remoteUri().toString();
        if (repoId < 0) {
            if (ReposClient.getId(getActivity(), remoteUriString) != 0)
                return; // TODO: throw an exception/handle this case
            ReposClient.insert(getActivity(), remoteUriString);
            repoId = ReposClient.getId(getActivity(), remoteUriString);
        }

        boolean commitSuccess =
                new PreferenceStringWriter(
                        getActivity(), repoPreferences().getRepoPreferences().edit()).
                putString(R.string.pref_key_git_repository_filepath, directoryFragment.getValue()).
                putString(R.string.pref_key_git_ssh_key_path, sshKeyFragment.getValue()).
                putString(R.string.pref_key_git_author, gitAuthorText.getText().toString()).
                putString(R.string.pref_key_git_email, gitEmailText.getText().toString())
                        .editor.commit();

        ReposClient.updateUrl(getActivity(), repoId, remoteUriString);

        GitRepo repo;
        try {
            repo = GitRepo.buildFromUri(getContext(), remoteUri());
        } catch (IOException | URISyntaxException e) {
            showError(e.toString());
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

    private String withDefault(String v, int selector) {
        if (v != null && v.length() > 0) {
            return v;
        }
        return AppPreferences.getStateSharedPreferences(getContext()).
                getString(getResources().getString(selector), "");
    }

    @Override
    public String sshKeyPathString() {
        return withDefault(sshKeyFragment.getValue(), R.string.pref_key_git_ssh_key_path);
    }

    @Override
    public String getAuthor() {
        return withDefault(gitAuthorText.getText().toString(), R.string.pref_key_git_author);
    }

    @Override
    public String getEmail() {
        return withDefault(gitEmailText.getText().toString(), R.string.pref_key_git_email);
    }

    @Override
    public String repositoryFilepath() {
        String v = directoryFragment.getValue();
        if (v != null && v.length() > 0) {
            return v;
        }
        return AppPreferences.repositoryStoragePathForUri(getActivity(), remoteUri());
    }

    @Override
    public String remoteName() {
        // TODO: Update this if remote selection is ever allowed.
        return withDefault("", R.string.pref_key_git_remote_name);
    }

    @Override
    public Uri remoteUri() {
        String remoteUriString = uriText.getText().toString();
        return Uri.parse(remoteUriString);
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

    class CloneProgressUpdate {
        public int amount;
        public boolean setMax;

        CloneProgressUpdate(int a, boolean m) {
            amount = a;
            setMax = m;
        }
    }

    class RepoCloneTask extends AsyncTask<Void, CloneProgressUpdate, IOException> implements ProgressMonitor {

        GitRepoFragment fragment;
        ProgressDialog progressDialog;

        RepoCloneTask(GitRepoFragment f) {
            super();
            fragment = f;
            progressDialog = new ProgressDialog(fragment.getActivity());
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Ensuring repository settings will work.");
            progressDialog.show();
        }

        @Override
        protected void onCancelled() {
            progressDialog.dismiss();
        }

        @Override
        protected void onPostExecute(IOException e) {
            progressDialog.dismiss();
            fragment.repoCheckComplete(e);
        }

        @Override
        protected IOException doInBackground(Void... params) {
            try {
                GitRepo.ensureRepositoryExists(fragment, true, this);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(CloneProgressUpdate... updates) {
            for (int i = 0; i < updates.length; i++) {
                CloneProgressUpdate u = updates[i];
                if (u.setMax) {
                    progressDialog.setMessage("Cloning repository");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMax(u.amount);
                } else {
                    progressDialog.incrementProgressBy(u.amount);
                }
            }
        }

        @Override
        public void start(int totalTasks) {
            publishProgress(new CloneProgressUpdate(totalTasks, true));
        }

        @Override
        public void beginTask(String title, int totalWork) {

        }

        @Override
        public void update(int completed) {
            publishProgress(new CloneProgressUpdate(completed, false));
        }

        @Override
        public void endTask() {

        }
    }

    public interface GitRepoFragmentListener extends RepoFragmentListener {}
}

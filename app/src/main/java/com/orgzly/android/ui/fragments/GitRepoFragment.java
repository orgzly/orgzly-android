package com.orgzly.android.ui.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.git.GitPreferences;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.RepoPreferences;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.GitRepo;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;


public class GitRepoFragment extends RepoFragment implements GitPreferences {
    private static final String TAG = DirectoryRepoFragment.class.getName();
    public static final String FRAGMENT_TAG = GitRepoFragment.class.getName();
    private static final String ARG_REPO_ID = "repo_id";

    private EditText uriText;
    private FileSelectionFragment directoryFragment;
    private FileSelectionFragment sshKeyFragment;
    private EditText gitAuthorText;
    private EditText gitEmailText;
    private EditText gitBranchText;
    private View view;
    private long repoId;
    private RepoPreferences repoPreferences;
    private GitRepoFragmentListener mListener;

    private EditTextPreference[] editTextPreferences;

    public static GitRepoFragment getInstance() {
        return new GitRepoFragment();
    }

    public static GitRepoFragment getInstance(long repoId) {
        GitRepoFragment fragment = new GitRepoFragment();
        Bundle args = new Bundle();

        args.putLong(ARG_REPO_ID, repoId);

        fragment.setArguments(args);

        return fragment;
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
        for(EditTextPreference editTextPreference : editTextPreferences) {
            setTextFromPrefKey(editTextPreference.editText, editTextPreference.preference);
        }
    }

    private void setTextFromPrefKey(EditText editText, int prefKey) {
        editText.length();
        if (editText.length() < 1)
            editText.setText(
                    repoPreferences().getStringValue(prefKey, ""));
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
        gitBranchText = (EditText) view.findViewById(R.id.fragment_repo_git_branch);
        directoryFragment = (FileSelectionFragment) getChildFragmentManager().
                findFragmentById(R.id.fragment_repo_git_directory);
        sshKeyFragment = (FileSelectionFragment) getChildFragmentManager().
                findFragmentById(R.id.fragment_repo_ssh_key_filepath);
        directoryFragment.setHint(R.string.fragment_repo_external_storage_directory_desc);
        directoryFragment.allowFileSelection = false;
        sshKeyFragment.setHint(R.string.fragment_repo_ssh_key_location_desc);
        sshKeyFragment.allowFileSelection = true;

        editTextPreferences = new EditTextPreference[]{
                new EditTextPreference(
                        directoryFragment.getEditText(), R.string.pref_key_git_repository_filepath),
                new EditTextPreference(
                        sshKeyFragment.getEditText(), R.string.pref_key_git_ssh_key_path),
                new EditTextPreference(gitAuthorText, R.string.pref_key_git_author),
                new EditTextPreference(gitEmailText, R.string.pref_key_git_email),
                new EditTextPreference(gitBranchText, R.string.pref_key_git_branch_name)
        };

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
            throw new ClassCastException(
                    getActivity().toString() + " must implement " + GitRepoFragmentListener.class);
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
        ((CommonActivity) getActivity()).showSimpleSnackbarLong(errorString);
    }

    private boolean setPreferenceEdits() {
        SharedPreferences.Editor editor = repoPreferences().getRepoPreferences().edit();
        for(EditTextPreference editTextPreference : editTextPreferences) {
            String settingName = getSettingName(editTextPreference.preference);
            String value = editTextPreference.editText.getText().toString();
            if (value.length() > 0) {
                editor = editor.putString(settingName, value);
            } else {
                editor = editor.remove(settingName);
            }
        }
        return editor.commit();
    }

    private void save() {
        String remoteUriString = remoteUri().toString();
        if (repoId < 0) {
            ReposClient.insert(getActivity(), remoteUriString);
            repoId = ReposClient.getId(getActivity(), remoteUriString);
        }
        if (ReposClient.getUrl(getActivity(), repoId) != remoteUriString) {
            ReposClient.updateUrl(getActivity(), repoId, remoteUriString);
        }

        setPreferenceEdits();

        getActivity().getSupportFragmentManager().popBackStack();
        ActivityUtils.closeSoftKeyboard(getActivity());
    }

    private String getSettingName(int setting) {
        return getResources().getString(setting);
    }

    private String withDefault(String v, int selector) {
        if (v != null && v.length() > 0) {
            return v;
        }
        return AppPreferences.getStateSharedPreferences(getContext()).
                getString(getSettingName(selector), "");
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
    public String branchName() {
        return withDefault(gitBranchText.getText().toString(), R.string.pref_key_git_branch_name);
    }

    @Override
    public Uri remoteUri() {
        String remoteUriString = uriText.getText().toString();
        return Uri.parse(remoteUriString);
    }

    class EditTextPreference {
        public EditText editText;
        public int preference;

        EditTextPreference(EditText et, int p) {
            editText = et;
            preference = p;
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
                    progressDialog.hide();
                    progressDialog.setIndeterminate(false);
                    progressDialog.show();
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

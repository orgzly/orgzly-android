package com.orgzly.android.git;

import android.net.Uri;

import com.orgzly.R;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.RepoPreferences;
import com.orgzly.android.provider.clients.ReposClient;

public class GitPreferencesFromRepoPrefs implements GitPreferences {
    private RepoPreferences repoPreferences;

    public GitPreferencesFromRepoPrefs(RepoPreferences prefs) {
        repoPreferences = prefs;
    }

    public String sshKeyPathString() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_ssh_key_path, "orgzly");
    }

    public String getAuthor() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_author, "orgzly");
    }

    public String getEmail() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_email, "");
    }

    public String repositoryFilepath() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_repository_filepath,
                AppPreferences.repositoryStoragePathForUri(
                        repoPreferences.getContext(), remoteUri()));
    }

    public String remoteName() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_remote_name, "origin");
    }

    public String branchName() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_branch_name, "master");
    }

    public Uri remoteUri() {
        return Uri.parse(ReposClient.getUrl(
                repoPreferences.getContext(), repoPreferences.getRepoId()));
    }
}

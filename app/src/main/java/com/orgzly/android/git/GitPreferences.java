package com.orgzly.android.git;

import com.orgzly.R;
import com.orgzly.android.prefs.RepoPreferences;

// XXX: This should probably be an interface but it's unlikely there will be another impl.
public class GitPreferences {
    private RepoPreferences repoPreferences;

    public GitPreferences(RepoPreferences prefs) {
        repoPreferences = prefs;
    }

    public GitTransportSetter getTransportSetter() {
        return new GitSSHKeyTransportSetter(sshKeyPathString());
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
                R.string.pref_key_git_repository_filepath, defaultRepositoryPath());
    }

    public String remoteName() {
        return repoPreferences.getStringValueWithGlobalDefault(
                R.string.pref_key_git_remote_name, "origin");
    }

    private String defaultRepositoryPath() {
        // TODO: fix this
        return "";
    }
}

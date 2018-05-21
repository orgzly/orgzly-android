package com.orgzly.android.git;

import android.net.Uri;

public interface GitPreferences {
    public String sshKeyPathString();
    public String getAuthor();
    public String getEmail();
    public String repositoryFilepath();
    public String remoteName();
    public String branchName();
    public Uri remoteUri();
}

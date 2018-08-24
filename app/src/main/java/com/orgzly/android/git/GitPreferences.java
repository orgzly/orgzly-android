package com.orgzly.android.git;

import android.net.Uri;

public interface GitPreferences {
    String sshKeyPathString();

    String getAuthor();

    String getEmail();

    String repositoryFilepath();

    String remoteName();

    String branchName();

    Uri remoteUri();
}

package com.orgzly.android.repos;

import java.util.Collection;

public class RepoUtils {
    public static boolean requireConnection(Collection<SyncRepo> repos) {
        for (SyncRepo repo: repos) {
            if (repo.requiresConnection()) {
                return true;
            }
        }
        return false;
    }
}


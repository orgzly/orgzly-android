package com.orgzly.android.repos;

import java.util.Collection;

public class RepoUtils {
    /**
     * @return true if there is a repository that requires connection, false otherwise
     */
    public static boolean isConnectionRequired(Collection<SyncRepo> repos) {
        for (SyncRepo repo: repos) {
            if (repo.isConnectionRequired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if all repositories support auto-sync, false otherwise
     */
    public static boolean isAutoSyncSupported(Collection<SyncRepo> repos) {
        for (SyncRepo repo: repos) {
            if (!repo.isAutoSyncSupported()) {
                return false;
            }
        }
        return true;
    }
}


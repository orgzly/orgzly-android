package com.orgzly.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class RepoPreferences {
    private Context context;
    private long repoId;
    private Uri repoUri;

    public RepoPreferences(Context c, long rid, Uri uri) {
        repoId = rid;
        context = c;
        repoUri = uri;
    }

    private String getRepoPreferencesFilename() {
        return String.format("repo.%d", repoId);
    }

    public SharedPreferences getRepoPreferences() {
        return context.getSharedPreferences(getRepoPreferencesFilename(), Context.MODE_PRIVATE);
    }

    private SharedPreferences getAppPreferences() {
        return AppPreferences.getStateSharedPreferences(context);
    }

    private String getSelector(int selector) {
        return context.getResources().getString(selector);
    }

    public String getStringValue(int selector, String def) {
        return getStringValue(getSelector(selector), def);
    }

    public String getStringValue(String key, String def) {
        return getRepoPreferences().getString(key, def);
    }

    public String getStringValueWithGlobalDefault(String key, String def) {
        return getStringValue(key, getAppPreferences().getString(key, def));
    }

    public String getStringValueWithGlobalDefault(int selector, String def) {
        return getStringValueWithGlobalDefault(getSelector(selector), def);
    }

    public long getRepoId() {
        return repoId;
    }

    public Context getContext() {
        return context;
    }

    public Uri getRepoUri() { return repoUri; }
}

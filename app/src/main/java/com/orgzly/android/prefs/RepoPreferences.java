package com.orgzly.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.orgzly.R;
import com.orgzly.android.provider.clients.ReposClient;

public class RepoPreferences {
    private Context context;
    private long repoId;

    public static RepoPreferences fromString(Context c, String string) {
        long rid = ReposClient.getId(c, string);
        return new RepoPreferences(c, rid);
    }

    public static RepoPreferences fromUri(Context c, Uri uri) {
        return fromString(c, uri.toString());
    }

    public RepoPreferences(Context c, long rid) {
        repoId = rid;
        context = c;
    }

    private String getRepoPreferencesFilename() {
        return String.format("repo.%d.xml", repoId);
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
}

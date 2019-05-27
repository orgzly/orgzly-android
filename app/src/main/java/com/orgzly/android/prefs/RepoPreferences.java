package com.orgzly.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.orgzly.android.data.DataRepository;

import java.io.IOException;

public class RepoPreferences {
    private Context context;
    private long repoId;
    private Uri repoUri;

    public static RepoPreferences fromUri(Context c, Uri uri, DataRepository repo) throws IOException {
        return new RepoPreferences(c, repo.getRepo(uri.toString()).getId(), uri);
    }

    public static RepoPreferences fromId(Context c, long repoId, DataRepository repo) {
        return new RepoPreferences(c, repoId, Uri.parse(repo.getRepo(repoId).getUrl()));
    }

    public RepoPreferences(Context c, long rid, Uri uri) {
        repoId = rid;
        context = c;
        repoUri = uri;
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

    public Uri getRepoUri() { return repoUri; }

    public long getRepoId() {
        return repoId;
    }

    public Context getContext() {
        return context;
    }
}

package com.orgzly.android.ui.fragments;

import android.os.Bundle;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import com.orgzly.R;

public class AutoSyncSettingsFragment extends PreferenceFragment {
    private static final String TAG = AutoSyncSettingsFragment.class.getName();
    public static final String FRAGMENT_TAG = AutoSyncSettingsFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_auto_sync);
    }

    public static AutoSyncSettingsFragment getInstance() {
        return new AutoSyncSettingsFragment();
    }

}

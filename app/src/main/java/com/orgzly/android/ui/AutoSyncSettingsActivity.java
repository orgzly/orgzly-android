package com.orgzly.android.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.support.v4.app.Fragment;

import com.orgzly.R;
import com.orgzly.android.ui.fragments.AutoSyncSettingsFragment;
import com.orgzly.android.ui.util.ActivityUtils;


public class AutoSyncSettingsActivity extends CommonActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.pref_title_preferencescreen_auto_sync);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            Fragment fragment = AutoSyncSettingsFragment.getInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment, AutoSyncSettingsFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ActivityUtils.setColorsForFragment(this, AutoSyncSettingsFragment.FRAGMENT_TAG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ActivityUtils.closeSoftKeyboard(this);
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

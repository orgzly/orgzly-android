package com.orgzly.android.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import com.github.machinarius.preferencefragment.PreferenceFragment;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Shelf;
import com.orgzly.android.Notifications;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.prefs.ListPreferenceWithValueAsSummary;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.ui.FragmentListener;
import com.orgzly.android.ui.NoteStateSpinner;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Displays settings.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = SettingsFragment.class.getName();

    public static final String FRAGMENT_TAG = SettingsFragment.class.getName();

    private static final @StringRes int[] REQUIRE_ACTIVITY_RESTART = new int [] {
            R.string.pref_key_font_size,
            R.string.pref_key_color_scheme,
            R.string.pref_key_layout_direction
    };

    private Preference mReposPreference;
    private SettingsFragmentListener mListener;
    public static SettingsFragment getInstance() {
        return new SettingsFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        mReposPreference = findPreference(getString(R.string.pref_key_repos));

        findPreference(getString(R.string.pref_key_clear_database))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (mListener != null) {
                            mListener.onDatabaseClearRequest();
                        }
                        return true;
                    }
                });
        findPreference(getString(R.string.pref_key_reload_getting_started))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (mListener != null) {
                            mListener.onGettingStartedNotebookReloadRequest();
                        }
                        return true;
                    }
                });

        setupVersionPreference();

        /* Receive onCreateOptionsMenu() call, to remove search menu item. */
        setHasOptionsMenu(true);

        setDefaultStateForNewNote();

        /* Disable preference for changing the layout, if not on API version that supports that. */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Preference layoutDirectionPref = findPreference(getString(R.string.pref_key_layout_direction));
            layoutDirectionPref.setEnabled(false);
        }

        /* Update preferences which depend on multiple others. */
        updateRemindersPreferences();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        /*
         * Set fragment's background.
         */
        if (view != null) {
            int[] textSizeAttr = new int[] { R.attr.item_book_card_bg_color};
            TypedArray typedArray = view.getContext().obtainStyledAttributes(textSizeAttr);
            int color = typedArray.getColor(0, -1);
            typedArray.recycle();

            if (color != -1) {
                view.setBackgroundColor(color);
            }
        }

        /*
         * Add some padding to list.
         */
        if (view != null) {
            ListView list = (ListView) view.findViewById(android.R.id.list);
            if (list != null) {
                int h = (int) getResources().getDimension(R.dimen.fragment_horizontal_padding);
                int v = (int) getResources().getDimension(R.dimen.fragment_vertical_padding);
                list.setPadding(h, v, h, v);
            }
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mListener = (SettingsFragmentListener) getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateUserReposPreferenceSummary();

        /* Start to listen for any preference changes. */
        android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);


        announceChangesToActivity();
    }

    private void announceChangesToActivity() {
        if (mListener != null) {
            mListener.announceChanges(
                    SettingsFragment.FRAGMENT_TAG,
                    getString(R.string.fragment_settings_title),
                    null,
                    0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        /* Stop listening for preference changed. */
        android.preference.PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    private void setupVersionPreference() {
        Preference versionPreference = findPreference(getString(R.string.pref_key_version));

        /* Set summary to the current version string, appending suffix for the flavor. */
        versionPreference.setSummary(BuildConfig.VERSION_NAME + BuildConfig.VERSION_NAME_SUFFIX);

        /* Display changelog dialog when version is clicked. */
        versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mListener != null) {
                    mListener.onWhatsNewDisplayRequest();
                }

                return true;
            }
        });

    }

    /**
     * Updates preference's summary with a list of configured user repos.
     */
    public void updateUserReposPreferenceSummary() {
        Map<String, Repo> repos = ReposClient.getAll(getActivity().getApplicationContext());

        if (repos.isEmpty()) {
            mReposPreference.setSummary(R.string.no_user_repos_configured_pref_summary);

        } else {
            List<String> list = new ArrayList<>();
            for (Repo repo : repos.values()) {
                list.add(repo.toString());
            }
            Collections.sort(list);
            mReposPreference.setSummary(TextUtils.join("\n", list));
        }
    }

    /**
     * Callback for options menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /*
         * Clear the menu. We want new SettingsActivity anyway,
         * to remove as much code from MainActivity as possible.
         */
        menu.clear();
    }

    /**
     * Called when a shared preference is modified in any way.
     * Used to update AppPreferences' static values and do any required post-settings-change work.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sharedPreferences, key);

        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        /* State keywords. */
        if (getString(R.string.pref_key_states).equals(key)) {
            AppPreferences.updateStaticKeywords(getContext());

            /* Re-parse notes. */
            ActivityUtils.closeSoftKeyboard(activity);

            if (mListener != null) {
                mListener.onStateKeywordsPreferenceChanged();
            }

            setDefaultStateForNewNote();
        }

        /* Recreate activity if preference change requires it. */
        for (int res: REQUIRE_ACTIVITY_RESTART) {
            if (key.equals(getString(res))) {
                activity.recreate();
                break;

            }
        }

        /* Cancel or create an new-note ongoing notification. */
        if (getString(R.string.pref_key_new_note_notification).equals(key)) {
            if (AppPreferences.newNoteNotification(getContext())) {
                Notifications.createNewNoteNotification(getContext());
            } else {
                Notifications.cancelNewNoteNotification(getContext());
            }
        }

        /* Update default priority when minimum priority changes. */
        if (getString(R.string.pref_key_min_priority).equals(key)) {

            String defPri = AppPreferences.defaultPriority(getContext());
            String minPri = sharedPreferences.getString(key, null);

            // Default priority is lower then minimum
            if (defPri.compareToIgnoreCase(minPri) > 0) { // minPri -> defPri
                /* Must use preference directly to update the view too. */
                ListPreferenceWithValueAsSummary pref =
                        (ListPreferenceWithValueAsSummary)
                                findPreference(getString(R.string.pref_key_default_priority));
                pref.setValue(minPri);
            }
        }

        /* Update minimum priority when default priority changes. */
        if (getString(R.string.pref_key_default_priority).equals(key)) {
            String minPri = AppPreferences.minPriority(getContext());
            String defPri = sharedPreferences.getString(key, null);

            // Default priority is lower then minimum
            if (minPri.compareToIgnoreCase(defPri) < 0) { // minPri -> defPri
                /* Must use preference directly to update the view too. */
                ListPreferenceWithValueAsSummary pref =
                        (ListPreferenceWithValueAsSummary)
                                findPreference(getString(R.string.pref_key_min_priority));
                pref.setValue(defPri);
            }
        }

        if (getString(R.string.pref_key_use_reminders_for_scheduled_times).equals(key)) {
            /* Reset last run time if reminders are being enabled or disabled. */
            AppPreferences.reminderServiceLastRun(getContext(), 0L);
        }

        if (getString(R.string.pref_key_use_reminders_for_scheduled_times).equals(key)) {
            updateRemindersPreferences();
        }


        /* Always notify if settings are modified.
         * Changing states or priorities can affect the displayed data for example.
         */
        Shelf.notifyDataChanged(getContext());
    }

    private void updateRemindersPreferences() {
        boolean remindersEnabled = ((CheckBoxPreference) findPreference(getString(R.string.pref_key_use_reminders_for_scheduled_times))).isChecked();

        findPreference(getString(R.string.pref_key_reminders_sound)).setEnabled(remindersEnabled);
        findPreference(getString(R.string.pref_key_reminders_vibrate)).setEnabled(remindersEnabled);
    }

    /**
     * Update list of possible states that can be used as default for a new note.
     */
    private void setDefaultStateForNewNote() {
        /* NOTE followed by to-do keywords */
        LinkedHashSet<CharSequence> entries = new LinkedHashSet<>();
        entries.add(NoteStateSpinner.NO_STATE_KEYWORD);
        entries.addAll(AppPreferences.todoKeywordsSet(getContext()));
        CharSequence[] entriesArray = entries.toArray(new CharSequence[entries.size()]);

        /* Set possible values. */
        ListPreferenceWithValueAsSummary pref = (ListPreferenceWithValueAsSummary) findPreference(getString(R.string.pref_key_new_note_state));
        pref.setEntries(entriesArray);
        pref.setEntryValues(entriesArray);

        /* Set current value. */
        String value = AppPreferences.newNoteState(getContext());
        if (entries.contains(value)) {
            pref.setValue(value);
        } else {
            pref.setValue(NoteStateSpinner.NO_STATE_KEYWORD);
        }
    }

    public interface SettingsFragmentListener extends FragmentListener {
        void onStateKeywordsPreferenceChanged();

        void onDatabaseClearRequest();

        void onGettingStartedNotebookReloadRequest();

        void onWhatsNewDisplayRequest();
    }
}
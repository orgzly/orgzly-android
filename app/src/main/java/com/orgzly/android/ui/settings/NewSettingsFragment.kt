package com.orgzly.android.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceScreen
import android.support.annotation.StringRes
import android.text.TextUtils
import android.view.*
import android.widget.ListView
import com.github.machinarius.preferencefragment.PreferenceFragment
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.Notifications
import com.orgzly.android.Shelf
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.prefs.ListPreferenceWithValueAsSummary
import com.orgzly.android.provider.clients.ReposClient
import com.orgzly.android.ui.NoteStateSpinner
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import java.util.*

/**
 * Displays settings.
 */
class NewSettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mReposPreference: Preference? = null
    private var mListener: NewSettingsFragmentListener? = null

    fun isForResource(resource: String?): Boolean {
        val thisResource = arguments.getString(ARG_RESOURCE)
        return if (resource == null) thisResource == null else resource == thisResource
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource()

        /* Receive onCreateOptionsMenu() call, to remove search menu item. */
        setHasOptionsMenu(true)
    }

    private fun addPreferencesFromResource() {
        val resourceName = arguments?.getString(ARG_RESOURCE)

        when (resourceName) {
            null -> { // Main screen
                addPreferencesFromResource(R.xml.preferences)
                setupMainPageSettings()
            }
            "preferences_org_mode_tags_indent" -> addPreferencesFromResource(R.xml.preferences_org_mode_tags_indent)
            "preferences_auto_sync" -> addPreferencesFromResource(R.xml.preferences_auto_sync)
        }
    }

    private fun setupMainPageSettings() {
        mReposPreference = findPreference(getString(R.string.pref_key_repos))

        findPreference(getString(R.string.pref_key_clear_database))
                .setOnPreferenceClickListener { preference ->
                    if (mListener != null) {
                        mListener!!.onDatabaseClearRequest()
                    }
                    true
                }
        findPreference(getString(R.string.pref_key_reload_getting_started))
                .setOnPreferenceClickListener { preference ->
                    if (mListener != null) {
                        mListener!!.onGettingStartedNotebookReloadRequest()
                    }
                    true
                }

        setupVersionPreference()

        setDefaultStateForNewNote()

        /* Disable preference for changing the layout, if not on API version that supports that. */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val layoutDirectionPref = findPreference(getString(R.string.pref_key_layout_direction))
            layoutDirectionPref.isEnabled = false
        }

        /* Update preferences which depend on multiple others. */
        updateOtherPreferencesForReminders()
    }

    private fun setupVersionPreference() {
        val versionPreference = findPreference(getString(R.string.pref_key_version))

        /* Set summary to the current version string, appending suffix for the flavor. */
        versionPreference.summary = BuildConfig.VERSION_NAME + BuildConfig.VERSION_NAME_SUFFIX

        /* Display changelog dialog when version is clicked. */
        versionPreference.setOnPreferenceClickListener { preference ->
            if (mListener != null) {
                mListener!!.onWhatsNewDisplayRequest()
            }

            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = super.onCreateView(inflater, container, savedInstanceState)

        /*
         * Set fragment's background.
         */
        if (view != null) {
            val textSizeAttr = intArrayOf(R.attr.item_book_card_bg_color)
            val typedArray = view.context.obtainStyledAttributes(textSizeAttr)
            val color = typedArray.getColor(0, -1)
            typedArray.recycle()

            if (color != -1) {
                view.setBackgroundColor(color)
            }
        }

        /*
         * Add some padding to list.
         */
        if (view != null) {
            val list = view.findViewById(android.R.id.list) as ListView
            if (list != null) {
                val h = resources.getDimension(R.dimen.fragment_horizontal_padding).toInt()
                val v = resources.getDimension(R.dimen.fragment_vertical_padding).toInt()
                list.setPadding(h, v, h, v)
            }
        }

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        mListener = activity as NewSettingsFragmentListener
    }

    override fun onResume() {
        super.onResume()

        // Needs to be done in background.
        // updateUserReposPreferenceSummary();

        /* Start to listen for any preference changes. */
        android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
                .registerOnSharedPreferenceChangeListener(this)
    }


    override fun onPause() {
        super.onPause()

        /* Stop listening for preference changed. */
        android.preference.PreferenceManager.getDefaultSharedPreferences(activity)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDetach() {
        super.onDetach()

        mListener = null
    }

    /**
     * Updates preference's summary with a list of configured user repos.
     */
    fun updateUserReposPreferenceSummary() {
        val repos = ReposClient.getAll(activity.applicationContext)

        if (repos.isEmpty()) {
            mReposPreference!!.setSummary(R.string.no_user_repos_configured_pref_summary)

        } else {
            val list = ArrayList<String>()
            for (repo in repos.values) {
                list.add(repo.toString())
            }
            Collections.sort(list)
            mReposPreference!!.summary = TextUtils.join("\n", list)
        }
    }

    /**
     * Callback for options menu.
     */
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        /*
         * Clear the menu. We want new SettingsActivity anyway,
         * to remove as much code from MainActivity as possible.
         */
        menu!!.clear()
    }

    /**
     * Called when a shared preference is modified in any way.
     * Used to update AppPreferences' static values and do any required post-settings-change work.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sharedPreferences, key)

        val activity = activity

        /* No activity or not the main settings page. */
        if (activity == null || arguments.get(ARG_RESOURCE) != null) {
            return
        }

        /* State keywords. */
        if (getString(R.string.pref_key_states) == key) {
            AppPreferences.updateStaticKeywords(context)

            /* Re-parse notes. */
            ActivityUtils.closeSoftKeyboard(activity)

            if (mListener != null) {
                mListener!!.onStateKeywordsPreferenceChanged()
            }

            setDefaultStateForNewNote()
        }

        /* Recreate activity if preference change requires it. */
        for (res in REQUIRE_ACTIVITY_RESTART) {
            if (key == getString(res)) {
                activity.recreate()
                break

            }
        }

        /* Cancel or create an new-note ongoing notification. */
        if (getString(R.string.pref_key_new_note_notification) == key) {
            if (AppPreferences.newNoteNotification(context)) {
                Notifications.createNewNoteNotification(context)
            } else {
                Notifications.cancelNewNoteNotification(context)
            }
        }

        /* Update default priority when minimum priority changes. */
        if (getString(R.string.pref_key_min_priority) == key) {

            val defPri = AppPreferences.defaultPriority(context)
            val minPri = sharedPreferences.getString(key, null)

            // Default priority is lower then minimum
            if (defPri.compareTo(minPri!!, ignoreCase = true) > 0) { // minPri -> defPri
                /* Must use preference directly to update the view too. */
                val pref = findPreference(getString(R.string.pref_key_default_priority)) as ListPreferenceWithValueAsSummary
                pref.value = minPri
            }
        }

        /* Update minimum priority when default priority changes. */
        if (getString(R.string.pref_key_default_priority) == key) {
            val minPri = AppPreferences.minPriority(context)
            val defPri = sharedPreferences.getString(key, null)

            // Default priority is lower then minimum
            if (minPri.compareTo(defPri!!, ignoreCase = true) < 0) { // minPri -> defPri
                /* Must use preference directly to update the view too. */
                val pref = findPreference(getString(R.string.pref_key_min_priority)) as ListPreferenceWithValueAsSummary
                pref.value = defPri
            }
        }

        /* Reminders for scheduled notes. Reset last run time. */
        if (getString(R.string.pref_key_use_reminders_for_scheduled_times) == key) {
            AppPreferences.reminderLastRunForScheduled(context, 0L)
        }

        /* Reminders for deadlines. Reset last run time. */
        if (getString(R.string.pref_key_use_reminders_for_deadline_times) == key) {
            AppPreferences.reminderLastRunForDeadline(context, 0L)
        }

        updateOtherPreferencesForReminders()

        /* Always notify about possibly changed data, if settings are modified.
         *
         * For example:
         * - Changing states or priorities can affect the displayed data
         * - Enabling or disabling reminders need to trigger service notification
         */
        Shelf.notifyDataChanged(context)
    }

    private fun updateOtherPreferencesForReminders() {
        val remindersEnabled = (findPreference(getString(R.string.pref_key_use_reminders_for_scheduled_times)) as CheckBoxPreference).isChecked || (findPreference(getString(R.string.pref_key_use_reminders_for_deadline_times)) as CheckBoxPreference).isChecked

        findPreference(getString(R.string.pref_key_reminders_sound)).isEnabled = remindersEnabled
        findPreference(getString(R.string.pref_key_reminders_vibrate)).isEnabled = remindersEnabled
        findPreference(getString(R.string.pref_key_snooze_time)).isEnabled = remindersEnabled
        findPreference(getString(R.string.pref_key_snooze_type)).isEnabled = remindersEnabled
    }

    /**
     * Update list of possible states that can be used as default for a new note.
     */
    private fun setDefaultStateForNewNote() {
        /* NOTE followed by to-do keywords */
        val entries = LinkedHashSet<CharSequence>()
        entries.add(NoteStateSpinner.NO_STATE_KEYWORD)
        entries.addAll(AppPreferences.todoKeywordsSet(context))
        val entriesArray = entries.toTypedArray()

        /* Set possible values. */
        val pref = findPreference(getString(R.string.pref_key_new_note_state)) as ListPreferenceWithValueAsSummary
        pref.entries = entriesArray
        pref.entryValues = entriesArray

        /* Set current value. */
        val value = AppPreferences.newNoteState(context)
        if (entries.contains(value)) {
            pref.value = value
        } else {
            pref.value = NoteStateSpinner.NO_STATE_KEYWORD
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference): Boolean {
        super.onPreferenceTreeClick(preferenceScreen, preference)
        if (preference is PreferenceScreen) {
            mListener!!.onPreferenceScreen(preference.getKey())
            return true
        }
        return false
    }

    interface NewSettingsFragmentListener {
        fun onStateKeywordsPreferenceChanged()
        fun onDatabaseClearRequest()
        fun onGettingStartedNotebookReloadRequest()
        fun onWhatsNewDisplayRequest()
        fun onPreferenceScreen(resource: String)
    }

    companion object {
        val TAG = NewSettingsFragment::class.java.name

        val FRAGMENT_TAG = NewSettingsFragment::class.java.name

        private val ARG_RESOURCE = "resource"

        @StringRes private val REQUIRE_ACTIVITY_RESTART = intArrayOf(R.string.pref_key_font_size, R.string.pref_key_color_scheme, R.string.pref_key_layout_direction)

        fun getInstance(res: String? = null): NewSettingsFragment {
            val fragment = NewSettingsFragment()

            val args = Bundle()
            args.putString(ARG_RESOURCE, res)
            fragment.arguments = args

            return fragment
        }
    }
}

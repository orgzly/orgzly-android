package com.orgzly.android.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import android.preference.TwoStatePreference
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.machinarius.preferencefragment.PreferenceFragment
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.Notifications
import com.orgzly.android.Shelf
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.prefs.ListPreferenceWithValueAsSummary
import com.orgzly.android.ui.NoteStates
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import com.orgzly.android.widgets.ListWidgetProvider
import java.util.*

/**
 * Displays settings.
 */
class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var mListener: SettingsFragmentListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource()

        setupPreferences()
    }

    private fun addPreferencesFromResource() {
        val resourceName = arguments?.getString(ARG_RESOURCE)

        when (resourceName) {
            null -> addPreferencesFromResource(R.xml.prefs) // Headings

            in PREFS_RESOURCES -> addPreferencesFromResource(PREFS_RESOURCES.getValue(resourceName))
        }
    }

    private fun setupPreferences() {
        /* Make icons lighter. */
//        preferenceScreen?.let {
//            for (i in 0 until it.preferenceCount) {
//                it.getPreference(i)?.icon?.alpha = 140
//            }
//        }

        findPreference(getString(R.string.pref_key_clear_database))?.let {
            it.setOnPreferenceClickListener { _ ->
                mListener?.onDatabaseClearRequest()
                true
            }
        }

        findPreference(getString(R.string.pref_key_reload_getting_started))?.let {
            it.setOnPreferenceClickListener { _ ->
                mListener?.onGettingStartedNotebookReloadRequest()
                true
            }
        }

        setupVersionPreference()

        setDefaultStateForNewNote()

        /* Disable changing the language if it's not supported in this version. */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val pref = findPreference(getString(R.string.pref_key_ignore_system_locale))
            if (pref != null) {
                preferenceScreen.removePreference(pref)
            }
        }

        /* Update preferences which depend on multiple others. */
        updateRemindersScreen()
    }

    private fun setupVersionPreference() {
        findPreference(getString(R.string.pref_key_version))?.let { pref ->
            /* Set summary to the current version string, appending suffix for the flavor. */
            pref.summary = BuildConfig.VERSION_NAME + BuildConfig.VERSION_NAME_SUFFIX

            /* Display changelog dialog when version is clicked. */
            pref.setOnPreferenceClickListener { _ ->
                mListener?.onWhatsNewDisplayRequest()
                true
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        /* Remove dividers. */
//        view?.findViewById(android.R.id.list)?.let {
//            (it as? ListView)?.divider = null
//        }

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        mListener = activity as SettingsFragmentListener
    }

    override fun onResume() {
        super.onResume()

        mListener?.onTitleChange(preferenceScreen?.title)

        /* Start to listen for any preference changes. */
        PreferenceManager.getDefaultSharedPreferences(activity)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        /* Stop listening for preference changed. */
        PreferenceManager.getDefaultSharedPreferences(activity)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDetach() {
        super.onDetach()

        mListener = null
    }

    /**
     * Called when a shared preference is modified in any way.
     * Used to update AppPreferences' static values and do any required post-settings-change work.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sharedPreferences, key)

        val activity = activity ?: return

        /* State keywords. */
        if (getString(R.string.pref_key_states) == key) {
            AppPreferences.updateStaticKeywords(context)

            /* Re-parse notes. */
            ActivityUtils.closeSoftKeyboard(activity)
            mListener?.onNotesUpdateRequest(AppIntent.ACTION_REPARSE_NOTES)

            setDefaultStateForNewNote()
        }

        if (getString(R.string.pref_key_is_created_at_added) == key || getString(R.string.pref_key_created_at_property) == key) {
            if (AppPreferences.createdAt(context)) {
                mListener?.onNotesUpdateRequest(AppIntent.ACTION_SYNC_CREATED_AT_WITH_PROPERTY)
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
            findPreference(getString(R.string.pref_key_default_priority))?.let {
                val pref = it as ListPreferenceWithValueAsSummary

                val defPri = AppPreferences.defaultPriority(context)
                val minPri = sharedPreferences.getString(key, null)

                // Default priority is lower then minimum
                if (defPri.compareTo(minPri, ignoreCase = true) > 0) { // minPri -> defPri
                    /* Must use preference directly to update the view too. */
                    pref.value = minPri
                }
            }
        }

        /* Update minimum priority when default priority changes. */
        if (getString(R.string.pref_key_default_priority) == key) {
            findPreference(getString(R.string.pref_key_min_priority))?.let {
                val pref = it as ListPreferenceWithValueAsSummary

                val minPri = AppPreferences.minPriority(context)
                val defPri = sharedPreferences.getString(key, null)

                // Default priority is lower then minimum
                if (minPri.compareTo(defPri, ignoreCase = true) < 0) { // minPri -> defPri
                    /* Must use preference directly to update the view too. */
                    pref.value = defPri
                }
            }
        }

        /* Update widget for changed style. */
        when (key) {
            getString(R.string.pref_key_widget_color_scheme),
            getString(R.string.pref_key_widget_font_size),
            getString(R.string.pref_key_widget_opacity),
            getString(R.string.pref_key_widget_update_frequency) -> {
                val intent = Intent(context, ListWidgetProvider::class.java)
                intent.action = AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET
                context?.sendBroadcast(intent)
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

        updateRemindersScreen()

        /* Always notify about possibly changed data, if settings are modified.
         *
         * For example:
         * - Changing states or priorities can affect the displayed data
         * - Enabling or disabling reminders need to trigger service notification
         */
        Shelf.notifyDataChanged(context)
    }

    private fun updateRemindersScreen() {
        val scheduled = findPreference(getString(R.string.pref_key_use_reminders_for_scheduled_times))
        val deadline = findPreference(getString(R.string.pref_key_use_reminders_for_deadline_times))

        if (scheduled != null && deadline != null) {
            val remindersEnabled = (scheduled as TwoStatePreference).isChecked
                                   || (deadline as TwoStatePreference).isChecked

            /* These do not exist on Oreo and later */
            findPreference(getString(R.string.pref_key_reminders_sound))?.isEnabled = remindersEnabled
            findPreference(getString(R.string.pref_key_reminders_led))?.isEnabled = remindersEnabled
            findPreference(getString(R.string.pref_key_reminders_vibrate))?.isEnabled = remindersEnabled

            /* This does not exist before Oreo. */
            findPreference(getString(R.string.pref_key_reminders_channel_notification_settings))?.isEnabled = remindersEnabled

            findPreference(getString(R.string.pref_key_snooze_time)).isEnabled = remindersEnabled
            findPreference(getString(R.string.pref_key_snooze_type)).isEnabled = remindersEnabled
        }
    }


    /**
     * Update list of possible states that can be used as default for a new note.
     */
    private fun setDefaultStateForNewNote() {
        findPreference(getString(R.string.pref_key_new_note_state))?.let {
            val pref = it as ListPreferenceWithValueAsSummary

            /* NOTE followed by to-do keywords */
            val entries = LinkedHashSet<CharSequence>()
            entries.add(NoteStates.NO_STATE_KEYWORD)
            entries.addAll(AppPreferences.todoKeywordsSet(context))
            val entriesArray = entries.toTypedArray()

            /* Set possible values. */
            pref.entries = entriesArray
            pref.entryValues = entriesArray

            /* Set current value. */
            val value = AppPreferences.newNoteState(context)
            if (entries.contains(value)) {
                pref.value = value
            } else {
                pref.value = NoteStates.NO_STATE_KEYWORD
            }
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
        if (preference != null && preference is PreferenceScreen) {
            preference.key?.let { key ->
                if (key in PREFS_RESOURCES) {
                    mListener?.onPreferenceScreen(key)
                    return true
                }
            }
        }

        return false
    }

    interface SettingsFragmentListener {
        fun onNotesUpdateRequest(action: String)
        fun onDatabaseClearRequest()
        fun onGettingStartedNotebookReloadRequest()
        fun onWhatsNewDisplayRequest()
        fun onPreferenceScreen(resource: String)
        fun onTitleChange(title: CharSequence?)
    }

    companion object {
        private val TAG: String = SettingsFragment::class.java.name

        val FRAGMENT_TAG: String = SettingsFragment::class.java.name

        private val ARG_RESOURCE = "resource"

        /* Using headers file & fragments didn't work well - transitions were
         * not smooth, previous fragment would be briefly displayed.
         */
        @JvmStatic
        val PREFS_RESOURCES: HashMap<String, Int> = hashMapOf(
                "prefs_screen_look_and_feel" to R.xml.prefs_screen_look_and_feel,
                "prefs_screen_notebooks" to R.xml.prefs_screen_notebooks,
                "prefs_screen_notifications" to R.xml.prefs_screen_notifications,
                "prefs_screen_reminders" to R.xml.prefs_screen_reminders,
                "prefs_screen_sync" to R.xml.prefs_screen_sync,
                "prefs_screen_auto_sync" to R.xml.prefs_screen_auto_sync, // Sub-screen
                "prefs_screen_org_file_format" to R.xml.prefs_screen_org_file_format, // Sub-screen
                "prefs_screen_org_mode_tags_indent" to R.xml.prefs_screen_org_mode_tags_indent, // Sub-screen
                "prefs_screen_widget" to R.xml.prefs_screen_widget, // Sub-screen
                "prefs_screen_app" to R.xml.prefs_screen_app
        )

        fun getInstance(res: String? = null): SettingsFragment {
            val fragment = SettingsFragment()

            val args = Bundle()
            args.putString(ARG_RESOURCE, res)
            fragment.arguments = args

            return fragment
        }
    }
}

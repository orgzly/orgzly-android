package com.orgzly.android.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.*
import com.orgzly.android.reminders.ReminderService
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.NoteStates
import com.orgzly.android.ui.notifications.Notifications
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.usecase.NoteReparseStateAndTitles
import com.orgzly.android.usecase.NoteSyncCreatedAtTimeWithProperty
import com.orgzly.android.usecase.UseCase
import com.orgzly.android.util.AppPermissions
import com.orgzly.android.util.LogUtils
import com.orgzly.android.widgets.ListWidgetProvider
import java.util.*

/**
 * Displays settings.
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var listener: Listener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
                listener?.onDatabaseClearRequest()
                true
            }
        }

        findPreference(getString(R.string.pref_key_reload_getting_started))?.let {
            it.setOnPreferenceClickListener { _ ->
                listener?.onGettingStartedNotebookReloadRequest()
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
                listener?.onWhatsNewDisplayRequest()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = activity as Listener
    }

    /*
     * Display custom preference's dialog.
     */
    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, preference)

        when (preference) {
            is StatesPreference ->
                displayCustomPreferenceDialogFragment(
                        StatesPreferenceFragment.getInstance(preference),
                        StatesPreferenceFragment.FRAGMENT_TAG)

            is IntegerPreference ->
                displayCustomPreferenceDialogFragment(
                        IntegerPreferenceFragment.getInstance(preference),
                        IntegerPreferenceFragment.FRAGMENT_TAG)

            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun displayCustomPreferenceDialogFragment(
            fragment: PreferenceDialogFragmentCompat,
            tag: String
    ) {
        fragmentManager?.let {
            fragment.setTargetFragment(this, 0)
            fragment.show(it, tag)
        }
    }

    override fun onResume() {
        super.onResume()

        listener?.onTitleChange(preferenceScreen?.title)

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

        listener = null
    }

    /**
     * Called when a shared preference is modified in any way.
     * Used to update AppPreferences' static values and do any required post-settings-change work.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sharedPreferences, key)

        val activity = activity as? CommonActivity ?: return

        when (key) {
            // State keywords
            getString(R.string.pref_key_states) -> {
                AppPreferences.updateStaticKeywords(context)

                /* Re-parse notes. */
                ActivityUtils.closeSoftKeyboard(activity)
                listener?.onNotesUpdateRequest(NoteReparseStateAndTitles())

                setDefaultStateForNewNote()
            }

            // Created-at property
            getString(R.string.pref_key_is_created_at_added),
            getString(R.string.pref_key_created_at_property) -> {
                if (AppPreferences.createdAt(context)) {
                    listener?.onNotesUpdateRequest(NoteSyncCreatedAtTimeWithProperty())
                }
            }

            // Cancel or create an new-note ongoing notification
            getString(R.string.pref_key_new_note_notification) -> {
                if (AppPreferences.newNoteNotification(context)) {
                    Notifications.createNewNoteNotification(context)
                } else {
                    Notifications.cancelNewNoteNotification(context)
                }
            }

            // Update default priority when minimum priority changes
            getString(R.string.pref_key_min_priority) -> {
                findPreference(getString(R.string.pref_key_default_priority))?.let {
                    val pref = it as ListPreferenceWithValueAsSummary

                    val defPri = AppPreferences.defaultPriority(context)
                    val minPri = sharedPreferences.getString(key, null)

                    // Default priority is lower then minimum
                    if (minPri != null && defPri.compareTo(minPri, ignoreCase = true) > 0) {
                        // minPri -> defPri
                        // Must use preference directly to update the view too
                        pref.value = minPri
                    }
                }
            }

            // Update minimum priority when default priority changes
            getString(R.string.pref_key_default_priority) -> {
                findPreference(getString(R.string.pref_key_min_priority))?.let {
                    val pref = it as ListPreferenceWithValueAsSummary

                    val minPri = AppPreferences.minPriority(context)
                    val defPri = sharedPreferences.getString(key, null)

                    // Default priority is lower then minimum
                    if (defPri != null && minPri.compareTo(defPri, ignoreCase = true) < 0) {
                        // minPri -> defPri
                        // Must use preference directly to update the view too
                        pref.value = defPri
                    }
                }
            }

            // Update widget for changed style
            getString(R.string.pref_key_widget_color_scheme),
            getString(R.string.pref_key_widget_font_size),
            getString(R.string.pref_key_widget_opacity),
            getString(R.string.pref_key_widget_update_frequency) -> {
                val intent = Intent(context, ListWidgetProvider::class.java)
                intent.action = AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET
                context?.sendBroadcast(intent)
            }

            // Reminders for scheduled notes - reset last run time
            getString(R.string.pref_key_use_reminders_for_scheduled_times) ->
                AppPreferences.reminderLastRunForScheduled(context, 0L)

            // Reminders for deadlines - reset last run time
            getString(R.string.pref_key_use_reminders_for_deadline_times) ->
                AppPreferences.reminderLastRunForDeadline(context, 0L)

            // Reminders for events - reset last run time
            getString(R.string.pref_key_use_reminders_for_event_times) ->
                AppPreferences.reminderLastRunForEvents(context, 0L)

            // Display images inline enabled - request permission
            getString(R.string.pref_key_images_enabled) -> {
                if (AppPreferences.imagesEnabled(context)) {
                    Handler().post {
                        AppPermissions.isGrantedOrRequest(
                                activity, AppPermissions.Usage.EXTERNAL_FILES_ACCESS)
                    }
                }
            }
        }

        updateRemindersScreen()

        /* Always notify about possibly changed data, if settings are modified.
         *
         * For example:
         * - Changing states or priorities can affect the displayed data
         * - Enabling or disabling reminders needs to trigger reminder service notification
         */
        ReminderService.notifyDataChanged(context)
        ListWidgetProvider.notifyDataChanged(context)
    }

    private fun updateRemindersScreen() {
        val scheduled = findPreference(getString(R.string.pref_key_use_reminders_for_scheduled_times))
        val deadline = findPreference(getString(R.string.pref_key_use_reminders_for_deadline_times))
        val event = findPreference(getString(R.string.pref_key_use_reminders_for_event_times))

        if (scheduled != null && deadline != null && event != null) {
            val remindersEnabled =
                    (scheduled as TwoStatePreference).isChecked
                            || (deadline as TwoStatePreference).isChecked
                            || (event as TwoStatePreference).isChecked

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

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference != null && preference is PreferenceScreen) {
            preference.key?.let { key ->
                if (key in PREFS_RESOURCES) {
                    listener?.onPreferenceScreen(key)
                    return true
                }
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    interface Listener {
        fun onNotesUpdateRequest(action: UseCase)
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
        @JvmField
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

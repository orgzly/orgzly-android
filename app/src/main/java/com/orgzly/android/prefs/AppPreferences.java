package com.orgzly.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.orgzly.R;
import com.orgzly.org.OrgStatesWorkflow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.preference.PreferenceManager.setDefaultValues;


/**
 * Shared preferences utility class.
 */
public class AppPreferences {
    /* Static members for quick access. */
    private static Set<String> todoKeywords;
    private static Set<String> doneKeywords;

    /* Shared Preferences for states. */
    private static SharedPreferences getStateSharedPreferences(Context context) {
        return context.getSharedPreferences("state", Context.MODE_PRIVATE);
    }

    public static boolean isDoneKeyword(Context context, String state) {
        return state != null && AppPreferences.doneKeywordsSet(context).contains(state);
    }

    public static AppPreferencesValues getAllValues(Context context) {
        AppPreferencesValues values = new AppPreferencesValues();

        values.defaultPrefsValues = getDefaultSharedPreferences(context).getAll();
        values.statePrefsValues = getStateSharedPreferences(context).getAll();

        return values;
    }

    public static void setAllFromValues(Context context, AppPreferencesValues values) {
        AppPreferences.clearAllSharedPreferences(context);

        setPrefsFromValues(getDefaultSharedPreferences(context), values.defaultPrefsValues);
        setPrefsFromValues(getStateSharedPreferences(context), values.statePrefsValues);
    }

    @SuppressWarnings("unchecked")
    private static void setPrefsFromValues(SharedPreferences prefs, Map<String, ?> values) {
        SharedPreferences.Editor edit = prefs.edit();

        for (String key: values.keySet()) {
            Object value = values.get(key);

            if (value instanceof Boolean) {
                edit.putBoolean(key, (boolean) value);

            } else if (value instanceof Float) {
                edit.putFloat(key, (float) value);

            } else if (value instanceof Integer) {
                edit.putInt(key, (int) value);

            } else if (value instanceof Long) {
                edit.putLong(key, (long) value);

            } else if (value instanceof String) {
                edit.putString(key, (String) value);

            } else if (value instanceof Set) {
                edit.putStringSet(key, (Set) value);
            }
        }

        edit.apply();
    }

    /**
     * Clears all preferences and sets them to default values.
     */
    public static void setToDefaults(Context context) {
        clearAllSharedPreferences(context);

        setDefaultValues(context, R.xml.preferences, true);
    }

    private static void clearAllSharedPreferences(Context context) {
        /* Clear default preferences. */
        getDefaultSharedPreferences(context).edit().clear().apply();

        /* Clear state preferences. */
        getStateSharedPreferences(context).edit().clear().apply();
    }

    public static boolean refreshOnSharedPreferenceChanged(Context context, String key) {
        boolean isReparsingNotesRequired = false;

        if (context.getString(R.string.pref_key_states).equals(key)) {
            updateStaticKeywords(context);
            isReparsingNotesRequired = true;
        }

        return isReparsingNotesRequired;
    }

    /*
     * User preferences.
     * Default values are taken from string resources (also used by preferences.xml)
     */

    public static boolean isNotesContentDisplayedInList(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_notes_content_displayed_in_list),
                context.getResources().getBoolean(R.bool.pref_default_is_notes_content_displayed_in_list));
    }

    public static boolean isNotesContentDisplayedInSearch(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_notes_content_displayed_in_search),
                context.getResources().getBoolean(R.bool.pref_default_is_notes_content_displayed_in_search));
    }

    public static boolean isNotesContentFoldable(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_notes_content_foldable),
                context.getResources().getBoolean(R.bool.pref_default_is_notes_content_foldable));
    }

    public static String prefaceDisplay(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_preface_in_book),
                context.getResources().getString(R.string.pref_default_value_preface_in_book));
    }

    public static void prefaceDisplay(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_preface_in_book);
        getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    public static boolean isFontMonospaced(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_font_monospaced),
                context.getResources().getBoolean(R.bool.pref_default_is_font_monospaced));
    }

    public static String notebooksSortOrder(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_notebooks_sort_order),
                context.getResources().getString(R.string.pref_default_notebooks_sort_order));
    }

    public static String notesListDensity(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_notes_list_density),
                context.getResources().getString(R.string.pref_default_notes_list_density));
    }

    public static boolean displayPlanning(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_display_planning),
                context.getResources().getBoolean(R.bool.pref_default_display_planning));
    }

    public static boolean createdAt(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_created_at_added),
                context.getResources().getBoolean(R.bool.pref_default_value_is_created_at_added));
    }

    public static String createdAtProperty(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_created_at_property),
                context.getResources().getString(R.string.pref_default_created_at_property));
    }

    public static String shareNotebook(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_share_notebook),
                context.getResources().getString(R.string.pref_default_share_notebook));
    }

    public static boolean newNoteNotification(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_new_note_notification),
                context.getResources().getBoolean(R.bool.pref_default_value_new_note_notification));
    }

    public static boolean remindersForScheduledTimes(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_use_reminders_for_scheduled_times),
                context.getResources().getBoolean(R.bool.pref_default_value_use_reminders_for_scheduled_times));
    }

    public static boolean remindersSound(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_reminders_sound),
                context.getResources().getBoolean(R.bool.pref_default_value_reminders_sound));
    }

    public static boolean remindersVibrate(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_reminders_vibrate),
                context.getResources().getBoolean(R.bool.pref_default_value_reminders_vibrate));
    }

    public static boolean showSyncNotifications(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_show_sync_notifications),
                context.getResources().getBoolean(R.bool.pref_default_value_show_sync_notifications));
    }

    public static String colorScheme(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_color_scheme),
                context.getResources().getString(R.string.pref_default_value_color_scheme));
    }

    public static String layoutDirection(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_layout_direction),
                context.getResources().getString(R.string.pref_default_value_layout_direction));
    }

    public static String bookNameInSearchResults(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_book_name_in_search),
                context.getResources().getString(R.string.pref_default_value_book_name_in_search));
    }

    public static boolean inheritedTagsInSearchResults(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_display_inherited_tags_in_search_results),
                context.getResources().getBoolean(R.bool.pref_default_display_inherited_tags_in_search_results));
    }

    public static void inheritedTagsInSearchResults(Context context, boolean value) {
        String key = context.getResources().getString(R.string.pref_key_display_inherited_tags_in_search_results);
        getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static String fontSize(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_font_size),
                context.getResources().getString(R.string.pref_default_value_font_size));
    }

    public static Set<String> displayedBookDetails(Context context) {
        return getDefaultSharedPreferences(context).getStringSet(
                context.getResources().getString(R.string.pref_key_displayed_book_details),
                new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.displayed_book_details_default))));
    }

    public static void displayedBookDetails(Context context, List<String> value) {
        String key = context.getResources().getString(R.string.pref_key_displayed_book_details);
        getDefaultSharedPreferences(context).edit().putStringSet(key, new HashSet<>(value)).apply();
    }

    public static String separateNotesWithNewLine(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_separate_notes_with_new_line),
                context.getResources().getString(R.string.pref_default_separate_notes_with_new_line));
    }

    public static boolean separateHeaderAndContentWithNewLine(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_separate_header_and_content_with_new_line),
                context.getResources().getBoolean(R.bool.pref_default_separate_header_and_content_with_new_line));
    }

    /*
     * Click action.
     */

    public static boolean isReverseNoteClickAction(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_reverse_click_action),
                context.getResources().getBoolean(R.bool.pref_default_is_reverse_click_action));
    }

    public static void isReverseNoteClickAction(Context context, boolean value) {
        String key = context.getResources().getString(R.string.pref_key_is_reverse_click_action);
        getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    /*
     * Schedule new note.
     */

    public static boolean isNewNoteScheduled(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_is_new_note_scheduled),
                context.getResources().getBoolean(R.bool.pref_default_value_is_new_note_scheduled));
    }

    /*
     * Set to-do for new note.
     */

    public static String newNoteState(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_new_note_state),
                context.getResources().getString(R.string.pref_default_value_new_note_state));
    }

    public static void newNoteState(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_new_note_state);
        getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    /*
     * State keywords
     */

    public static String states(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_states),
                context.getResources().getString(R.string.pref_default_states));
    }

    public static void states(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_states);
        getDefaultSharedPreferences(context).edit().putString(key, value).apply();
        updateStaticKeywords(context);
    }

    /*
     * TO-DO keywords
     */

    public static Set<String> todoKeywordsSet(Context context) {
        synchronized (AppPreferences.class) {
            if (todoKeywords == null) {
                updateStaticKeywords(context);
            }

            return todoKeywords;
        }
    }

    public static void updateStaticKeywords(Context context) {
        synchronized (AppPreferences.class) {
            todoKeywords = new LinkedHashSet<>();
            doneKeywords = new LinkedHashSet<>();

            for (OrgStatesWorkflow workflow: new StateWorkflows(states(context))) {
                todoKeywords.addAll(workflow.getTodoKeywords());
                doneKeywords.addAll(workflow.getDoneKeywords());
            }
        }
    }

    /*
     * DONE keywords
     */

    public static Set<String> doneKeywordsSet(Context context) {
        synchronized (AppPreferences.class) {
            if (doneKeywords == null) {
                updateStaticKeywords(context);
            }
            return doneKeywords;
        }
    }

    /*
     * Lowest priority.
     */

    public static String minPriority(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_min_priority),
                context.getResources().getString(R.string.pref_default_min_priority));
    }

    public static void minPriority(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_min_priority);
        getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    /*
     * Default priority.
     */

    public static String defaultPriority(Context context) {
        return getDefaultSharedPreferences(context).getString(
                context.getResources().getString(R.string.pref_key_default_priority),
                context.getResources().getString(R.string.pref_default_default_priority));
    }

    public static void defaultPriority(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_default_priority);
        getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    /*
     * State flags and values.
     * They have no default values, they are not set by user.
     */

    /*
     * Dropbox token.
     */

    public static String dropboxToken(Context context) {
        String key = context.getResources().getString(R.string.pref_key_dropbox_token);
        return getStateSharedPreferences(context).getString(key, null);
    }

    public static void dropboxToken(Context context, String value) {
        String key = context.getResources().getString(R.string.pref_key_dropbox_token);
        getStateSharedPreferences(context).edit().putString(key, value).apply();
    }

    /*
     * Last used version.
     */

    public static int lastUsedVersionCode(Context context) {
        String key = context.getResources().getString(R.string.pref_key_last_used_version_code);
        return getStateSharedPreferences(context).getInt(key, 0);
    }

    public static void lastUsedVersionCode(Context context, int value) {
        String key = context.getResources().getString(R.string.pref_key_last_used_version_code);
        getStateSharedPreferences(context).edit().putInt(key, value).apply();
    }

    /*
     * Getting started notebook loaded.
     */

    public static boolean isGettingStartedNotebookLoaded(Context context) {
        String key = context.getResources().getString(R.string.pref_key_is_getting_started_notebook_loaded);
        return getStateSharedPreferences(context).getBoolean(key, false);
    }

    public static void isGettingStartedNotebookLoaded(Context context, boolean value) {
        String key = context.getResources().getString(R.string.pref_key_is_getting_started_notebook_loaded);
        getStateSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    /*
     * Last sync.
     */

    public static void lastSuccessfulSyncTime(Context context, Long value) {
        String key = context.getResources().getString(R.string.pref_key_last_successful_sync_time);
        getStateSharedPreferences(context).edit().putLong(key, value).apply();
    }

    public static long lastSuccessfulSyncTime(Context context) {
        String key = context.getResources().getString(R.string.pref_key_last_successful_sync_time);
        return getStateSharedPreferences(context).getLong(key, 0L);
    }

    /*
     * ReminderService
     */

    public static void reminderServiceJobId(Context context, int value) {
        String key = context.getResources().getString(R.string.pref_key_reminder_service_job_id);
        getStateSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static void reminderServiceLastRun(Context context, long value) {
        String key = context.getResources().getString(R.string.pref_key_reminder_service_last_run);
        getStateSharedPreferences(context).edit().putLong(key, value).apply();
    }

    public static long reminderServiceLastRun(Context context) {
        String key = context.getResources().getString(R.string.pref_key_reminder_service_last_run);
        return getStateSharedPreferences(context).getLong(key, 0L);
    }

    public static boolean syncAfterNewNoteCreated(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(
                context.getResources().getString(R.string.pref_key_sync_after),
                context.getResources().getBoolean(R.bool.pref_default_value_sync_after));
    }
}

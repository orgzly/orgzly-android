package com.orgzly.android.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.entity.SavedSearch;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.android.ui.share.ShareActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.ui.util.SystemServices;
import com.orgzly.android.usecase.NoteUpdateStateToggle;
import com.orgzly.android.usecase.UseCaseRunner;
import com.orgzly.android.util.LogUtils;

import java.util.Calendar;
import java.util.Collections;

import javax.inject.Inject;

/**
 * The AppWidgetProvider for the list widget
 */
public class ListWidgetProvider extends AppWidgetProvider {
    private static final String TAG = ListWidgetProvider.class.getName();

    private static final String PREFERENCES_ID = "list-widget";

    public static final int OPEN_CLICK_TYPE = 1;
    public static final int DONE_CLICK_TYPE = 2;

    @Inject
    DataRepository dataRepository;

    public static void notifyDataSetChanged(Context context) {
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_UPDATE_LIST_WIDGET);
        context.sendBroadcast(intent);
    }

    public static void update(Context context) {
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidgetLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        App.EXECUTORS.diskIO().execute(() -> {
            SavedSearch savedSearch = getSavedSearch(context, appWidgetId);

            App.EXECUTORS.mainThread().execute(() -> {

                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.list_widget);

                WidgetStyle.updateWidget(remoteViews, context);

                Intent serviceIntent = new Intent(context, ListWidgetService.class);
                serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                serviceIntent.putExtra(AppIntent.EXTRA_QUERY_STRING, savedSearch.getQuery());
                serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

                // Tell ListView where to get the data from
                remoteViews.setRemoteAdapter(R.id.list_widget_list_view, serviceIntent);

                remoteViews.setEmptyView(R.id.list_widget_list_view, R.id.list_widget_empty_view);

                // Rows - open note
                final Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
                onClickIntent.setAction(AppIntent.ACTION_CLICK_LIST_WIDGET);
                onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
                final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        onClickIntent,
                        ActivityUtils.mutable(PendingIntent.FLAG_UPDATE_CURRENT));

                remoteViews.setPendingIntentTemplate(R.id.list_widget_list_view, onClickPendingIntent);

                // Plus icon - new note
                remoteViews.setOnClickPendingIntent(
                        R.id.list_widget_header_add,
                        ShareActivity.createNewNotePendingIntent(context, "widget-" + appWidgetId, savedSearch));

                // Logo - open query
                Intent openIntent = Intent.makeRestartActivityTask(new ComponentName(context, MainActivity.class));
                openIntent.putExtra(AppIntent.EXTRA_QUERY_STRING, savedSearch.getQuery());
                openIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(
                        R.id.list_widget_header_logo,
                        PendingIntent.getActivity(
                                context,
                                0,
                                openIntent,
                                ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT)));

                Intent selectionIntent = new Intent(context, ListWidgetSelectionActivity.class);
                selectionIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                selectionIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(
                        R.id.list_widget_header_bar,
                        PendingIntent.getActivity(context,
                                0,
                                selectionIntent,
                                ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT)));

                remoteViews.setTextViewText(
                        R.id.list_widget_header_selection,
                        savedSearch.getName());

                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            });
        });
    }

    private void updateAppWidgetLayouts(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        }

        scheduleUpdate(context);
    }

    private void updateListContents(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_widget_list_view);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        clearUpdate(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).edit();
        for (int id: appWidgetIds) {
            editor.remove(getFilterPreferenceKey(id));
        }
        editor.apply();
    }

    private void scheduleUpdate(Context context) {
        /*
         schedule updates via AlarmManager, because we don't want to wake the device on every update
         see https://developer.android.com/guide/topics/appwidgets/index.html#MetaData
         */
        AlarmManager alarmManager = SystemServices.getAlarmManager(context);

        PendingIntent intent = getAlarmIntent(context);

        alarmManager.cancel(intent);

        int intervalMin = AppPreferences.widgetUpdateFrequency(context);
        long intervalMillis = intervalMin * 60 * 1000;

        long now = System.currentTimeMillis();
        Calendar triggerAt = Calendar.getInstance();
        triggerAt.setTimeInMillis(now);
        triggerAt.set(Calendar.MILLISECOND, 1);
        triggerAt.set(Calendar.SECOND, 0);
        triggerAt.set(Calendar.MINUTE, 0);
        do {
            triggerAt.add(Calendar.MINUTE, intervalMin);
        } while (triggerAt.getTimeInMillis() < now);

        alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                triggerAt.getTimeInMillis(),
                intervalMillis,
                intent);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, triggerAt.getTimeInMillis(), intervalMillis);
    }

    private void clearUpdate(Context context) {
        SystemServices.getAlarmManager(context).cancel(getAlarmIntent(context));
    }

    private PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_UPDATE_LIST_WIDGET);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void setSelectionFromIntent(Context context, Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        long savedSearchId = intent.getLongExtra(AppIntent.EXTRA_SAVED_SEARCH_ID, 0);

        setFilter(context, appWidgetId, savedSearchId);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_widget_list_view);
    }

    private SavedSearch getSavedSearch(Context context, int appWidgetId) {
        long filterId = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE)
                .getLong(getFilterPreferenceKey(appWidgetId), -1);

        SavedSearch savedSearch = null;
        if (filterId != -1) {
            savedSearch = dataRepository.getSavedSearch(filterId);
        }

        if (savedSearch == null) {
            savedSearch = new SavedSearch(0, context.getString(R.string.list_widget_select_search), "", 0);
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedSearch, appWidgetId, filterId);

        return savedSearch;
    }

    private void setFilter(Context context, int appWidgetId, long id) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).edit();
        editor.putLong(getFilterPreferenceKey(appWidgetId), id);
        editor.apply();
    }

    private String getFilterPreferenceKey(int appWidgetId) {
        return "widget-filter-" + appWidgetId;
    }

    private void setNoteDone(Intent intent) {
        final long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0L);

        App.EXECUTORS.diskIO().execute(() ->
                UseCaseRunner.run(new NoteUpdateStateToggle(Collections.singleton(noteId))));
    }

    private void openNote(Context context, Intent intent) {
        long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0L);
        long bookId = intent.getLongExtra(AppIntent.EXTRA_BOOK_ID, 0L);

        PendingIntent pi = ActivityUtils.mainActivityPendingIntent(context, bookId, noteId);
        try {
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Error opening note: " + e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        App.appComponent.inject(this);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (AppIntent.ACTION_UPDATE_LIST_WIDGET.equals(intent.getAction())) {
            updateListContents(context);

        } else if (AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET.equals(intent.getAction())) {
            updateAppWidgetLayouts(context);

        } else if (AppIntent.ACTION_SET_LIST_WIDGET_SELECTION.equals(intent.getAction())) {
            setSelectionFromIntent(context, intent);

        } else if (AppIntent.ACTION_CLICK_LIST_WIDGET.equals(intent.getAction())) {
            switch (intent.getIntExtra(AppIntent.EXTRA_CLICK_TYPE, -1)) {
                case OPEN_CLICK_TYPE:
                    openNote(context, intent);
                    break;
                case DONE_CLICK_TYPE:
                    setNoteDone(intent);
                    break;
            }

        } else {
            super.onReceive(context, intent);
        }
    }
}

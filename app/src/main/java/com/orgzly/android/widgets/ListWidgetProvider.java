package com.orgzly.android.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Filter;
import com.orgzly.android.Shelf;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;

import java.util.Calendar;

/**
 * The AppWidgetProvider for the list widget
 */
public class ListWidgetProvider extends AppWidgetProvider {

    private static final String TAG = ListWidgetProvider.class.getName();
    private static final String PREFERENCES_ID = "list-widget";
    public static final String EXTRA_CLICK_TYPE = "click_type";
    public static final int OPEN_CLICK_TYPE = 1;
    public static final int DONE_CLICK_TYPE = 2;
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_FILTER_ID = "filter_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "onUpdate");

        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private static void updateAppWidgetLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Filter filter = getFilter(context, appWidgetId);

        Intent serviceIntent = new Intent(context, ListWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.putExtra(ListWidgetService.EXTRA_QUERY_STRING, filter.getQuery());
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.list_widget);
        remoteViews.setRemoteAdapter(R.id.list_widget_list_view, serviceIntent);

        remoteViews.setEmptyView(R.id.list_widget_list_view, R.id.list_widget_empty_view);
        if (filter.getQuery() == null) {
            remoteViews.setTextViewText(R.id.list_widget_empty_view, context.getString(R.string.select_a_filter_long));
        } else {
            remoteViews.setTextViewText(R.id.list_widget_empty_view, context.getString(R.string.no_notes_found_after_search));
        }

        /* Set the PendingIntent template for the clicks on the rows */
        final Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
        onClickIntent.setAction(AppIntent.ACTION_LIST_WIDGET_CLICK);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.list_widget_list_view, onClickPendingIntent);

        remoteViews.setOnClickPendingIntent(R.id.list_widget_header_add, ShareActivity.createNewNoteIntent(context));

        Intent filterSelectIntent = new Intent(context, FilterSelectDialogActivity.class);
        filterSelectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        filterSelectIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.list_widget_header_filter, PendingIntent.getActivity(context, 0, filterSelectIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        remoteViews.setTextViewText(
                R.id.list_widget_header_filter,
                filter.getName());

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static void updateAppWidgetLayouts(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "updateAppWidgetLayouts");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        }
    }

    private static void updateListContents(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "updateListContents");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_widget_list_view);
    }

    @Override
    public void onEnabled(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppIntent.ACTION_LIST_WIDGET_UPDATE);
        filter.addAction(AppIntent.ACTION_LIST_WIDGET_UPDATE_LAYOUT);

        LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);

        scheduleUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        clearUpdate(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).edit();
            editor.remove(getFilterPreferenceKey(id));
            editor.apply();
        }
    }

    private static void scheduleUpdate(Context context) {
        /*
         schedule updates via AlarmManager, because we don't want to wake the device on every update
         see https://developer.android.com/guide/topics/appwidgets/index.html#MetaData
         */
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent intent = getAlarmIntent(context);
        alarmManager.cancel(intent);

        /* repeat after every full hour because results of search can change on new day
            because of timezones repeat every hour instead of every day */
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);
        alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HOUR, intent);
    }

    private static void clearUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(getAlarmIntent(context));
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(AppIntent.ACTION_LIST_WIDGET_UPDATE);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void setFilterFromIntent(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        long filterId = intent.getLongExtra(EXTRA_FILTER_ID, 0);

        setFilter(context, appWidgetId, filterId);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_widget_list_view);
    }

    private static Filter getFilter(Context context, int appWidgetId) {
        long filterId = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).getLong(getFilterPreferenceKey(appWidgetId), -1);
        Filter filter = null;
        if (filterId != -1) {
            filter = FiltersClient.get(context, filterId);
        }

        if (filter == null) {
            filter = new Filter(context.getString(R.string.select_a_filter), null);
        }
        return filter;
    }

    private static void setFilter(Context context, int appWidgetId, long id) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).edit();
        editor.putLong(getFilterPreferenceKey(appWidgetId), id);
        editor.apply();
    }

    private static String getFilterPreferenceKey(int appWidgetId) {
        return "widget-filter-" + appWidgetId;
    }

    private void setNoteDone(Context context, Intent intent) {
        final Shelf shelf = new Shelf(context);

        final long noteId = intent.getLongExtra(ListWidgetProvider.EXTRA_NOTE_ID, 0L);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                shelf.setStateToDone(noteId);
                return null;
            }
        }.execute();
    }

    private void openNote(Context context, Intent intent) {
        long noteId = intent.getLongExtra(ListWidgetProvider.EXTRA_NOTE_ID, 0L);
        long bookId = intent.getLongExtra(ListWidgetProvider.EXTRA_BOOK_ID, 0L);

        try {
            ActivityUtils.mainActivityPendingIntent(context, bookId, noteId).send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Error opening note: " + e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (AppIntent.ACTION_LIST_WIDGET_UPDATE.equals(intent.getAction())) {
            updateListContents(context);

        } else if (AppIntent.ACTION_LIST_WIDGET_UPDATE_LAYOUT.equals(intent.getAction())) {
            updateAppWidgetLayouts(context);

        } else if (AppIntent.ACTION_LIST_WIDGET_SET_FILTER.equals(intent.getAction())) {
            setFilterFromIntent(context, intent);

        } else if (AppIntent.ACTION_LIST_WIDGET_CLICK.equals(intent.getAction())) {
            switch (intent.getIntExtra(EXTRA_CLICK_TYPE, -1)) {
                case OPEN_CLICK_TYPE:
                    openNote(context, intent);
                    break;
                case DONE_CLICK_TYPE:
                    setNoteDone(context, intent);
                    break;
            }

        } else {
            super.onReceive(context, intent);
        }
    }
}

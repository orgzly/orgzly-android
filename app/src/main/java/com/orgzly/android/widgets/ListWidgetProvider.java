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
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Filter;
import com.orgzly.android.Shelf;
import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.util.LogUtils;

/**
 * The AppWidgetProvider for the list widget
 */
public class ListWidgetProvider extends AppWidgetProvider {

    private static final String TAG = ListWidgetProvider.class.getName();
    private static final String ACTION_UPDATE = "com.orgzly.action.ACTION_UPDATE_LIST_WIDGET";
    private static final String ACTION_CLICK = "com.orgzly.action.ACTION_CLICK_LIST_WIDGET";
    private static final String PREFERENCES_ID = "com.orgzly.action.ACTION_UPDATE_LIST_WIDGET";
    public static final String ACTION_SET_FILTER = "com.orgzly.action.ACTION_SET_FILTER_LIST_WIDGET";
    public static final String EXTRA_CLICK_TYPE = "click_type";
    public static final int OPEN_CLICK_TYPE = 1;
    public static final int DONE_CLICK_TYPE = 2;
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_FILTER_ID = "filter_id";
    /* Setting this to a lower value doesn't have an effect */
    private static final int UPDATE_INTERVAL_MILLIS = 60 * 1000;

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

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_list);
        remoteViews.setRemoteAdapter(R.id.widget_list_list_view, serviceIntent);

        remoteViews.setEmptyView(R.id.widget_list_list_view, R.id.widget_list_empty_view);

        /* Set the PendingIntent template for the clicks on the rows */
        final Intent onClickIntent = new Intent(context, ListWidgetProvider.class);
        onClickIntent.setAction(ListWidgetProvider.ACTION_CLICK);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.widget_list_list_view, onClickPendingIntent);

        remoteViews.setOnClickPendingIntent(R.id.widget_list_header_add, ShareActivity.createNewNoteIntent(context));

        Intent filterSelectIntent = new Intent(context, FilterSelectDialogActivity.class);
        filterSelectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        filterSelectIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_list_header, PendingIntent.getActivity(context, 0, filterSelectIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        remoteViews.setTextViewText(
                R.id.widget_list_header_filter,
                filter.getName());

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public static void updateAppWidgetLayouts(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "updateAppWidgetLayouts");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateListContents(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "updateListContents");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), ListWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_list_view);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        clearUpdate(context);
    }

    private static void scheduleUpdate(Context context) {
        /*
         schedule updates via AlarmManager, because we don't want to wake the device on every update
         see https://developer.android.com/guide/topics/appwidgets/index.html#MetaData
         */
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent intent = getAlarmIntent(context);
        alarmManager.cancel(intent);
        alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), UPDATE_INTERVAL_MILLIS, intent);
    }

    private static void clearUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(getAlarmIntent(context));
    }

    public static void scheduleUpdateIfNeeded(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, ListWidgetProvider.class));
        if (ids.length > 0) {
            scheduleUpdate(context);
        }
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, ListWidgetProvider.class);
        intent.setAction(ACTION_UPDATE);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void setFilterFromIntent(Context context, Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        long filterId = intent.getLongExtra(EXTRA_FILTER_ID, 0);

        setFilter(context, appWidgetId, filterId);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        updateAppWidgetLayout(context, appWidgetManager, appWidgetId);
    }

    private static Filter getFilter(Context context, int appWidgetId) {
        long filterId = context.getSharedPreferences(PREFERENCES_ID, Context.MODE_PRIVATE).getLong("Filter" + appWidgetId, -1);
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
        editor.putLong("Filter" + appWidgetId, id);
        editor.apply();
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

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.putExtra(MainActivity.EXTRA_NOTE_ID, noteId);
        launchIntent.putExtra(MainActivity.EXTRA_BOOK_ID, bookId);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.setData(Uri.parse(launchIntent.toUri(Intent.URI_INTENT_SCHEME)));

        try {
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT).send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Error opening note: " + e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_UPDATE.equals(intent.getAction())) {
            updateListContents(context);
        } else if (ACTION_SET_FILTER.equals(intent.getAction())) {
            setFilterFromIntent(context, intent);
        } else if (ACTION_CLICK.equals(intent.getAction())) {
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

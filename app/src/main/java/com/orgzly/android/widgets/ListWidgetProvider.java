package com.orgzly.android.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;

/**
 * The AppWidgetProvider for the list widget
 */
public class ListWidgetProvider extends AppWidgetProvider {

    private static final String TAG = ListWidgetProvider.class.getName();
    private static final String ACTION_UPDATE = "com.orgzly.action.ACTION_UPDATE_LIST_WIDGET";
    private static final int UPDATE_INTERVAL_MILLIS = 1000; // TOOD value

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent serviceIntent = new Intent(context, ListWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.list_widget);
            remoteViews.setRemoteAdapter(R.id.list_widget_list_view, serviceIntent);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateList(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "updateList");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(),getClass().getName());
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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_UPDATE.equals(intent.getAction())) {
            updateList(context);
        } else {
            super.onReceive(context, intent);
        }
    }
}

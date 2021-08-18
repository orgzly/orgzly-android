package com.orgzly.android.external.actionhandlers

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.orgzly.android.AppIntent
import com.orgzly.android.external.types.Response
import com.orgzly.android.widgets.ListWidgetProvider

class ManageWidgets : ExternalAccessActionHandler() {
    override val actions = listOf(
        action(::getWidgets, "GET_WIDGETS"),
        action(::setWidget, "SET_WIDGET")
    )

    private fun getWidgets(context: Context): Response {
        val widgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context.packageName, ListWidgetProvider::class.java.name)
        val widgetData = widgetManager.getAppWidgetIds(componentName)
                .map { it to ListWidgetProvider.getSavedSearch(context, it, dataRepository) }
                .toMap()
        return Response(true, widgetData)
    }

    private fun setWidget(intent: Intent, context: Context): Response {
        val widgetId = intent.getIntExtra("WIDGET_ID", -1)
        if (widgetId < 0) return Response(false, "invalid widget ID")
        val savedSearch = intent.getSavedSearch()
                ?: return Response(false, "invalid saved search ID")

        context.sendBroadcast(Intent(context, ListWidgetProvider::class.java).apply {
            action = AppIntent.ACTION_SET_LIST_WIDGET_SELECTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppIntent.EXTRA_SAVED_SEARCH_ID, savedSearch.id)
        })

        return Response()
    }
}

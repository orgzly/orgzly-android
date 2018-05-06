package com.orgzly.android.widgets

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.TitleGenerator


object WidgetStyle {
    fun updateWidget(remoteViews: RemoteViews, context: Context) {
        remoteViews.setInt(
                R.id.list_widget_header_container,
                "setBackgroundColor",
                WidgetStyle.headerBackground(context))

        remoteViews.setTextColor(
                R.id.list_widget_header_filter,
                WidgetStyle.headerTextColor(context))

        remoteViews.setTextColor(
                R.id.list_widget_empty_view,
                WidgetStyle.primaryTextColor(context))

        remoteViews.setInt(
                R.id.list_widget_list_container,
                "setBackgroundColor",
                WidgetStyle.listBackgroundColor(context))
    }

    fun updateDivider(remoteViews: RemoteViews, context: Context) {
        remoteViews.setTextColor(
                R.id.widget_list_item_divider_value,
                WidgetStyle.primaryTextColor(context))
    }

    fun updateNote(remoteViews: RemoteViews, context: Context) {
        remoteViews.setTextColor(
                R.id.item_list_widget_title,
                WidgetStyle.primaryTextColor(context))

        remoteViews.setImageViewResource(
                R.id.item_list_widget_scheduled_icon,
                WidgetStyle.scheduledIcon(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_scheduled_text,
                WidgetStyle.secondaryTextColor(context))

        remoteViews.setImageViewResource(
                R.id.item_list_widget_deadline_icon,
                WidgetStyle.deadlineIcon(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_deadline_text,
                WidgetStyle.secondaryTextColor(context))

        remoteViews.setImageViewResource(
                R.id.item_list_widget_closed_icon,
                WidgetStyle.closedIcon(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_closed_text,
                WidgetStyle.secondaryTextColor(context))

        remoteViews.setImageViewResource(
                R.id.item_list_widget_done,
                WidgetStyle.doneIcon(context))
    }

    fun getTitleAttributes(context: Context): TitleGenerator.TitleAttributes {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark" ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_dark_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_dark_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_dark_state_unknown_color),
                        context.resources.getDimension(R.dimen.widget_post_title_text_size).toInt(),
                        ContextCompat.getColor(context, R.color.widget_dark_post_title_color))

            "black" ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_black_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_black_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_black_state_unknown_color),
                        context.resources.getDimension(R.dimen.widget_post_title_text_size).toInt(),
                        ContextCompat.getColor(context, R.color.widget_black_post_title_color))

            else ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_light_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_light_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_light_state_unknown_color),
                        context.resources.getDimension(R.dimen.widget_post_title_text_size).toInt(),
                        ContextCompat.getColor(context, R.color.widget_light_post_title_color))
        }
    }

    @ColorInt
    private fun primaryTextColor(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark" -> ContextCompat.getColor(context, R.color.widget_dark_title_color)
            "black" -> ContextCompat.getColor(context, R.color.widget_black_title_color)
            else -> ContextCompat.getColor(context, R.color.widget_light_title_color)
        }
    }

    @ColorInt
    private fun secondaryTextColor(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark" -> ContextCompat.getColor(context, R.color.widget_dark_post_title_color)
            "black" -> ContextCompat.getColor(context, R.color.widget_black_post_title_color)
            else -> ContextCompat.getColor(context, R.color.widget_light_post_title_color)
        }
    }

    @ColorInt
    fun headerBackground(context: Context): Int {
        val color = when (AppPreferences.widgetColorScheme(context)) {
            "dark" -> ContextCompat.getColor(context, R.color.widget_dark_header_bg_color)
            "black" -> ContextCompat.getColor(context, R.color.widget_black_header_bg_color)
            else -> ContextCompat.getColor(context, R.color.widget_light_header_bg_color)
        }

        return withOpacity(context, color)
    }

    @ColorInt
    private fun headerTextColor(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark" -> ContextCompat.getColor(context, R.color.widget_dark_header_text_color)
            "black" -> ContextCompat.getColor(context, R.color.widget_black_header_text_color)
            else -> ContextCompat.getColor(context, R.color.widget_light_header_text_color)
        }
    }

    @ColorInt
    private fun listBackgroundColor(context: Context): Int {
        val color = when (AppPreferences.widgetColorScheme(context)) {
            "dark" -> ContextCompat.getColor(context, R.color.widget_dark_list_bg_color)
            "black" -> ContextCompat.getColor(context, R.color.widget_black_list_bg_color)
            else -> ContextCompat.getColor(context, R.color.widget_light_list_bg_color)
        }

        return withOpacity(context, color)
    }

    @DrawableRes
    private fun scheduledIcon(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> R.drawable.ic_today_white_18dp
            else -> R.drawable.ic_today_black_18dp
        }
    }

    @DrawableRes
    private fun deadlineIcon(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> R.drawable.ic_alarm_white_18dp
            else -> R.drawable.ic_alarm_black_18dp
        }
    }

    @DrawableRes
    private fun closedIcon(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> R.drawable.ic_done_white_18dp
            else -> R.drawable.ic_done_black_18dp
        }
    }

    @DrawableRes
    private fun doneIcon(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> R.drawable.ic_done_white_24dp
            else -> R.drawable.ic_done_black_24dp
        }
    }

    @ColorInt
    private fun withOpacity(context: Context, @ColorInt color: Int): Int {
        val opacity = AppPreferences.widgetOpacity(context) / 100f
        return ((opacity * 0xFF).toInt() shl 24) or (color and 0x00ffffff)
    }
}

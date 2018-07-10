package com.orgzly.android.widgets

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.Window
import android.widget.RemoteViews
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.TitleGenerator


object WidgetStyle {
    @JvmStatic
    fun updateActivity(activity: AppCompatActivity) {
        when (AppPreferences.widgetColorScheme(activity)) {
            "dark", "black" -> activity.setTheme(R.style.Theme_AppCompat_Dialog_Alert)
            else -> activity.setTheme(R.style.Theme_AppCompat_Light_Dialog_Alert)
        }

        activity.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    @JvmStatic
    fun updateWidget(remoteViews: RemoteViews, context: Context) {
        remoteViews.setInt(
                R.id.list_widget_header_container,
                "setBackgroundColor",
                WidgetStyle.headerBackground(context))

        remoteViews.setTextViewTextSize(
                R.id.list_widget_header_filter,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.headerTextSize(context))

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

    @JvmStatic
    fun updateDivider(remoteViews: RemoteViews, context: Context) {
        remoteViews.setTextViewTextSize(
                R.id.widget_list_item_divider_value,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.titleTextSize(context))

        remoteViews.setTextColor(
                R.id.widget_list_item_divider_value,
                WidgetStyle.primaryTextColor(context))
    }

    @JvmStatic
    fun updateNote(remoteViews: RemoteViews, context: Context) {
        /* Title */

        remoteViews.setTextViewTextSize(
                R.id.item_list_widget_title,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.titleTextSize(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_title,
                WidgetStyle.primaryTextColor(context))

        /* Book name */

        remoteViews.setImageViewResource(
                R.id.item_list_widget_book_icon,
                WidgetStyle.bookIcon(context))

        remoteViews.setTextViewTextSize(
                R.id.item_list_widget_book_text,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.postTitleTextSize(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_book_text,
                WidgetStyle.secondaryTextColor(context))

        /* Scheduled time */

        remoteViews.setImageViewResource(
                R.id.item_list_widget_scheduled_icon,
                WidgetStyle.scheduledIcon(context))

        remoteViews.setTextViewTextSize(
                R.id.item_list_widget_scheduled_text,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.postTitleTextSize(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_scheduled_text,
                WidgetStyle.secondaryTextColor(context))

        /* Deadline time */

        remoteViews.setImageViewResource(
                R.id.item_list_widget_deadline_icon,
                WidgetStyle.deadlineIcon(context))

        remoteViews.setTextViewTextSize(
                R.id.item_list_widget_deadline_text,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.postTitleTextSize(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_deadline_text,
                WidgetStyle.secondaryTextColor(context))

        /* Closed time */

        remoteViews.setImageViewResource(
                R.id.item_list_widget_closed_icon,
                WidgetStyle.closedIcon(context))

        remoteViews.setTextViewTextSize(
                R.id.item_list_widget_closed_text,
                TypedValue.COMPLEX_UNIT_PX,
                WidgetStyle.postTitleTextSize(context))

        remoteViews.setTextColor(
                R.id.item_list_widget_closed_text,
                WidgetStyle.secondaryTextColor(context))

        /* Done icon */

        remoteViews.setImageViewResource(
                R.id.item_list_widget_done,
                WidgetStyle.doneIcon(context))
    }

    @JvmStatic
    fun getTitleAttributes(context: Context): TitleGenerator.TitleAttributes {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark" ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_dark_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_dark_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_dark_state_unknown_color),
                        postTitleTextSize(context).toInt(),
                        ContextCompat.getColor(context, R.color.widget_dark_post_title_color))

            "black" ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_black_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_black_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_black_state_unknown_color),
                        postTitleTextSize(context).toInt(),
                        ContextCompat.getColor(context, R.color.widget_black_post_title_color))

            else ->
                TitleGenerator.TitleAttributes(
                        ContextCompat.getColor(context, R.color.widget_light_state_todo_color),
                        ContextCompat.getColor(context, R.color.widget_light_state_done_color),
                        ContextCompat.getColor(context, R.color.widget_light_state_unknown_color),
                        postTitleTextSize(context).toInt(),
                        ContextCompat.getColor(context, R.color.widget_light_post_title_color))
        }
    }

    private fun titleTextSize(context: Context): Float {
        return when (AppPreferences.widgetFontSize(context)) {
            16 -> context.resources.getDimension(R.dimen.widget_title_text_size_16)
            else -> context.resources.getDimension(R.dimen.widget_title_text_size_14)
        }
    }

    private fun postTitleTextSize(context: Context): Float {
        return when (AppPreferences.widgetFontSize(context)) {
            16 -> context.resources.getDimensionPixelOffset(R.dimen.widget_post_title_text_size_16).toFloat()
            else -> context.resources.getDimensionPixelOffset(R.dimen.widget_post_title_text_size_14).toFloat()
        }
    }

    private fun headerTextSize(context: Context): Float {
        return when (AppPreferences.widgetFontSize(context)) {
            16 -> context.resources.getDimension(R.dimen.widget_header_text_size_16)
            else -> context.resources.getDimension(R.dimen.widget_header_text_size_14)
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
    private fun bookIcon(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> R.drawable.ic_folder_open_white_18dp
            else -> R.drawable.ic_folder_open_black_18dp
        }
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

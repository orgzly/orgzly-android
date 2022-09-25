package com.orgzly.android.widgets

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Window
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.TitleGenerator
import com.orgzly.android.util.LogUtils

/*
 * Known issue:
 * Widget needs to be recreated when scheme is switched back to dynamic color on API < 31.
 */
object WidgetStyle {
    @JvmStatic
    fun updateActivity(activity: AppCompatActivity) {
        when (AppPreferences.widgetColorScheme(activity)) {
            "light" -> activity.setTheme(R.style.Theme_Material3_Light_Dialog_Alert)
            "dark", "black" -> activity.setTheme(R.style.Theme_Material3_Dark_Dialog_Alert)
            else -> activity.setTheme(R.style.Theme_Material3_DayNight_Dialog_Alert)
        }

        activity.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    // Color or attribute id
    private fun getInt(scheme: String, id: Int): Int {
        WidgetColors.colors[scheme]?.get(id).let { value ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$scheme/$id: $value")
            return value ?: throw Exception("Not defined: $scheme/$id")
        }
    }

    @ColorInt
    private fun getColor(context: Context, scheme: String, name: String): Int {
        val s = WidgetColors.dataDependentColors[scheme]?.get(name).let { value ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$scheme/$name: $value")
            value ?: throw Exception("Not defined: $scheme/$name")
        }

        return ContextCompat.getColor(context, s)
    }

    private fun setColorFilter(
        context: Context,
        colorScheme: String,
        remoteViews: RemoteViews,
        @IdRes id: Int) {

        setInt("setColorFilter", context, colorScheme, remoteViews, id)
    }

    private fun setInt(
        methodName: String,
        context: Context,
        colorScheme: String,
        remoteViews: RemoteViews,
        @IdRes id: Int,
        withOpacity: Boolean = false) {

        if (colorScheme == "dynamic") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                remoteViews.setColorAttr(id, methodName, getInt(colorScheme, id))
            } // else: Widget needs to be recreated

        } else {
            var color = ContextCompat.getColor(context, getInt(colorScheme, id))
            if (withOpacity) {
                color = withOpacity(context, color)
            }
            remoteViews.setInt(id, methodName, color)
        }
    }

    private fun setTextColor(
        context: Context,
        colorScheme: String,
        remoteViews: RemoteViews,
        @IdRes id: Int) {

        if (colorScheme == "dynamic") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                remoteViews.setColorAttr(id, "setTextColor", getInt(colorScheme, id))
            } // else: Widget needs to be recreated

        } else {
            val color = ContextCompat.getColor(context, getInt(colorScheme, id))
            remoteViews.setTextColor(id, color)
        }
    }

    private fun setBackgroundColor(
        context: Context,
        colorScheme: String,
        remoteViews: RemoteViews,
        @IdRes id: Int) {

        setInt("setBackgroundColor", context, colorScheme, remoteViews, id, true)
    }

    @JvmStatic
    fun updateWidget(remoteViews: RemoteViews, context: Context) {
        val scheme = AppPreferences.widgetColorScheme(context)

        setBackgroundColor(context, scheme, remoteViews, R.id.list_widget_header_container)
        setBackgroundColor(context, scheme, remoteViews, R.id.list_widget_list_container)
        setColorFilter(context, scheme, remoteViews, R.id.list_widget_header_logo)
        setTextColor(context, scheme, remoteViews, R.id.list_widget_header_selection)
        setColorFilter(context, scheme, remoteViews, R.id.list_widget_header_selection_arrow)
        setColorFilter(context, scheme, remoteViews, R.id.list_widget_header_add)
        setTextColor(context, scheme, remoteViews, R.id.list_widget_empty_view)

        remoteViews.setTextViewTextSize(
            R.id.list_widget_header_selection,
            TypedValue.COMPLEX_UNIT_PX,
            headerTextSize(context))
    }

    @JvmStatic
    fun updateDivider(remoteViews: RemoteViews, context: Context) {
        val scheme = AppPreferences.widgetColorScheme(context)

        setTextColor(context, scheme, remoteViews, R.id.widget_list_item_divider_value)

        remoteViews.setTextViewTextSize(
            R.id.widget_list_item_divider_value,
            TypedValue.COMPLEX_UNIT_PX,
            titleTextSize(context))
    }

    @JvmStatic
    fun updateNote(remoteViews: RemoteViews, context: Context) {
        val colorScheme = AppPreferences.widgetColorScheme(context)

        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_title)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_book_icon)
        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_book_text)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_scheduled_icon)
        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_scheduled_text)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_deadline_icon)
        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_deadline_text)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_event_icon)
        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_event_text)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_closed_icon)
        setTextColor(context, colorScheme, remoteViews, R.id.item_list_widget_closed_text)
        setColorFilter(context, colorScheme, remoteViews, R.id.item_list_widget_done)

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_title,
            TypedValue.COMPLEX_UNIT_PX,
            titleTextSize(context))

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_book_text,
            TypedValue.COMPLEX_UNIT_PX,
            postTitleTextSize(context))

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_scheduled_text,
            TypedValue.COMPLEX_UNIT_PX,
            postTitleTextSize(context))

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_deadline_text,
            TypedValue.COMPLEX_UNIT_PX,
            postTitleTextSize(context))

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_event_text,
            TypedValue.COMPLEX_UNIT_PX,
            postTitleTextSize(context))

        remoteViews.setTextViewTextSize(
            R.id.item_list_widget_closed_text,
            TypedValue.COMPLEX_UNIT_PX,
            postTitleTextSize(context))

        remoteViews.setInt(
            R.id.item_list_widget_done, "setAlpha", doneIconAlpha(context))
    }

    @JvmStatic
    fun getTitleAttributes(context: Context): TitleGenerator.TitleAttributes {
        val scheme = AppPreferences.widgetColorScheme(context)
        val dayNight = context.getString(R.string.day_night)

        val stateColorsKey = if (scheme == "dynamic") {
            "dynamic-$dayNight"
        } else {
            scheme
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$scheme/$dayNight: $stateColorsKey")

        return TitleGenerator.TitleAttributes(
            getColor(context, stateColorsKey, "todo"),
            getColor(context, stateColorsKey, "done"),
            postTitleTextSize(context).toInt(),
            getColor(context, stateColorsKey, "post title"))
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

    private fun doneIconAlpha(context: Context): Int {
        return when (AppPreferences.widgetColorScheme(context)) {
            "dark", "black" -> 0xB3 // 70%
            else -> 0x8C // 55%
        }
    }

    @ColorInt
    private fun withOpacity(context: Context, @ColorInt color: Int): Int {
        val opacity = AppPreferences.widgetOpacity(context) / 100f
        return ((opacity * 0xFF).toInt() shl 24) or (color and 0x00ffffff)
    }

    private val TAG = WidgetStyle::class.java.name
}

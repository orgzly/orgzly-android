package com.orgzly.android.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.TimeType
import com.orgzly.android.ui.notes.query.agenda.AgendaItem
import com.orgzly.android.ui.notes.query.agenda.AgendaItems
import com.orgzly.android.ui.util.TitleGenerator
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.org.datetime.OrgRange
import org.joda.time.DateTime
import javax.inject.Inject

class ListWidgetService : RemoteViewsService() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onCreate() {
        App.appComponent.inject(this)

        super.onCreate()
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val queryString = intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING).orEmpty()

        return ListWidgetViewsFactory(applicationContext, queryString)
    }

    private sealed class WidgetEntry(open val id: Long) {
        data class Overdue(override val id: Long) : WidgetEntry(id)

        data class Day(override val id: Long, val day: DateTime) : WidgetEntry(id)

        data class Note(
                override val id: Long,
                val noteView: NoteView,
                val agendaTimeType: TimeType? = null
        ) : WidgetEntry(id)
    }

    inner class ListWidgetViewsFactory(
            val context: Context,
            private val queryString: String
    ) : RemoteViewsFactory {

        private val query: Query by lazy {
            val parser = InternalQueryParser()
            parser.parse(queryString)
        }

        private val userTimeFormatter by lazy {
            UserTimeFormatter(context)
        }

        private var dataList: List<WidgetEntry> = emptyList()

        override fun onCreate() {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return dataList[position].id
        }

        override fun onDataSetChanged() {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

            val notes = dataRepository.selectNotesFromQuery(query)

            if (query.isAgenda()) {
                val idMap = mutableMapOf<Long, Long>()
                val hideEmptyDaysInAgenda = AppPreferences.hideEmptyDaysInAgenda(context)
                val agendaItems = AgendaItems(hideEmptyDaysInAgenda).getList(notes, query, idMap)

                dataList = agendaItems.map {
                    when (it) {
                        is AgendaItem.Overdue -> WidgetEntry.Overdue(it.id)
                        is AgendaItem.Day -> WidgetEntry.Day(it.id, it.day)
                        is AgendaItem.Note -> WidgetEntry.Note(it.id, it.note, it.timeType)
                    }
                }

            } else {
                dataList = notes.map {
                    WidgetEntry.Note(it.note.id, it)
                }
            }
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getViewAt(position: Int): RemoteViews? {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position)

            if (position >= dataList.size) {
                Log.e(TAG, "List too small (${dataList.size}) for requested position $position")
                return null
            }

            return when (val entry = dataList[position]) {
                is WidgetEntry.Overdue ->
                    RemoteViews(context.packageName, R.layout.item_list_widget_divider).apply {
                        setupRemoteViews(this)
                        WidgetStyle.updateDivider(this, context)
                    }

                is WidgetEntry.Day ->
                    RemoteViews(context.packageName, R.layout.item_list_widget_divider).apply {
                        setupRemoteViews(this, entry)
                        WidgetStyle.updateDivider(this, context)
                    }

                is WidgetEntry.Note ->
                    RemoteViews(context.packageName, R.layout.item_list_widget).apply {
                        setupRemoteViews(this, entry)
                        WidgetStyle.updateNote(this, context)
                    }
            }
        }

        override fun getCount(): Int {
            return dataList.size
        }

        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun onDestroy() {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        }

        private fun setupRemoteViews(views: RemoteViews) {
            views.setTextViewText(
                    R.id.widget_list_item_divider_value,
                    context.getString(R.string.overdue))
        }

        private fun setupRemoteViews(views: RemoteViews, entry: WidgetEntry.Day) {
            views.setTextViewText(
                    R.id.widget_list_item_divider_value,
                    userTimeFormatter.formatDate(entry.day))
        }

        private fun setupRemoteViews(row: RemoteViews, entry: WidgetEntry.Note) {
            val noteView = entry.noteView

            val displayPlanningTimes = AppPreferences.displayPlanning(context)
            val displayBookName = AppPreferences.widgetDisplayBookName(context)
            val doneStates = AppPreferences.doneKeywordsSet(context)

            // Title (colors depend on current theme)
            val titleGenerator = TitleGenerator(context, false, WidgetStyle.getTitleAttributes(context))
            row.setTextViewText(R.id.item_list_widget_title, titleGenerator.generateTitle(noteView))

            // Notebook name
            if (displayBookName) {
                row.setTextViewText(R.id.item_list_widget_book_text, noteView.bookName)
                row.setViewVisibility(R.id.item_list_widget_book, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_book, View.GONE)
            }

            // Closed time
            if (displayPlanningTimes && noteView.closedRangeString != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(noteView.closedRangeString))
                row.setTextViewText(R.id.item_list_widget_closed_text, time)
                row.setViewVisibility(R.id.item_list_widget_closed, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_closed, View.GONE)
            }

            var scheduled = noteView.scheduledRangeString
            var deadline = noteView.deadlineRangeString
            var event = noteView.eventString

            // In Agenda only display time responsible for item's presence
            when (entry.agendaTimeType) {
                TimeType.SCHEDULED -> {
                    deadline = null
                    event = null
                }
                TimeType.DEADLINE -> {
                    scheduled = null
                    event = null
                }
                TimeType.EVENT -> {
                    scheduled = null
                    deadline = null
                }
                else -> {
                }
            }

            // Scheduled time
            if (displayPlanningTimes && scheduled != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(scheduled))
                row.setTextViewText(R.id.item_list_widget_scheduled_text, time)
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.GONE)
            }

            // Deadline time
            if (displayPlanningTimes && deadline != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(deadline))
                row.setTextViewText(R.id.item_list_widget_deadline_text, time)
                row.setViewVisibility(R.id.item_list_widget_deadline, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_deadline, View.GONE)
            }

            // Event time
            if (displayPlanningTimes && event != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(event))
                row.setTextViewText(R.id.item_list_widget_event_text, time)
                row.setViewVisibility(R.id.item_list_widget_event, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_event, View.GONE)
            }


            // Check mark
            if (!AppPreferences.widgetDisplayCheckmarks(context) || doneStates.contains(noteView.note.state)) {
                row.setViewVisibility(R.id.item_list_widget_done, View.GONE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_done, View.VISIBLE)
            }

            // Intent for opening note
            val openIntent = Intent()
            openIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.OPEN_CLICK_TYPE)
            openIntent.putExtra(AppIntent.EXTRA_NOTE_ID, noteView.note.id)
            openIntent.putExtra(AppIntent.EXTRA_BOOK_ID, noteView.note.position.bookId)
            row.setOnClickFillInIntent(R.id.item_list_widget_layout, openIntent)

            // Intent for marking note done
            val doneIntent = Intent()
            doneIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.DONE_CLICK_TYPE)
            doneIntent.putExtra(AppIntent.EXTRA_NOTE_ID, noteView.note.id)
            row.setOnClickFillInIntent(R.id.item_list_widget_done, doneIntent)
        }
    }

    companion object {
        private val TAG = ListWidgetService::class.java.name
    }
}

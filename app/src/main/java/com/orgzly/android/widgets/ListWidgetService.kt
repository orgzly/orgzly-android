package com.orgzly.android.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.query.Query
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.notes.query.agenda.AgendaItem
import com.orgzly.android.ui.notes.query.agenda.AgendaItems
import com.orgzly.android.ui.util.TitleGenerator
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.UserTimeFormatter
import com.orgzly.org.datetime.OrgRange
import dagger.android.AndroidInjection
import org.joda.time.DateTime
import javax.inject.Inject

class ListWidgetService : RemoteViewsService() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        AndroidInjection.inject(this)

        return ListWidgetViewsFactory(
                applicationContext,
                intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING))
    }

    private sealed class WidgetEntry(open val id: Long) {
        data class WidgetNoteEntry(override val id: Long, val noteId: Long) : WidgetEntry(id)

        data class WidgetDividerEntry(override val id: Long, val day: DateTime) : WidgetEntry(id)
    }

    inner class ListWidgetViewsFactory(val context: Context, val queryString: String) :
            RemoteViewsService.RemoteViewsFactory {

        private val query: Query by lazy {
            val parser = InternalQueryParser()
            parser.parse(queryString)
        }

        private val userTimeFormatter by lazy {
            UserTimeFormatter(context)
        }

        private lateinit var titleGenerator: TitleGenerator

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

            // Refresh title generator (for changed settings)
            val attrs = WidgetStyle.getTitleAttributes(context)
            titleGenerator = TitleGenerator(context, false, attrs)

            val notes = dataRepository.selectNotesFromQuery(query)

            if (query.isAgenda()) {
                val idMap = mutableMapOf<Long, Long>()
                val agendaItems = AgendaItems.getList(notes, query, idMap)

                dataList = agendaItems.map {
                    when (it) {
                        is AgendaItem.Note -> WidgetEntry.WidgetNoteEntry(it.id, it.note.note.id)
                        is AgendaItem.Divider -> WidgetEntry.WidgetDividerEntry(it.id, it.day)
                    }
                }

            } else {
                dataList = notes.map {
                    WidgetEntry.WidgetNoteEntry(it.note.id, it.note.id)
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

            val entry = dataList[position]

            return when (entry) {
                is WidgetEntry.WidgetDividerEntry ->
                    RemoteViews(context.packageName, R.layout.item_list_widget_divider).apply {
                        setupRemoteViews(this, entry)
                        WidgetStyle.updateDivider(this, context)
                    }

                is WidgetEntry.WidgetNoteEntry ->
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

        private fun setupRemoteViews(views: RemoteViews, entry: WidgetEntry.WidgetDividerEntry) {
            views.setTextViewText(
                    R.id.widget_list_item_divider_value,
                    userTimeFormatter.formatDate(entry.day))
        }

        private fun setupRemoteViews(row: RemoteViews, entry: WidgetEntry.WidgetNoteEntry) {
            val noteView = dataRepository.getNoteView(entry.noteId) ?: return

            val displayPlanningTimes = AppPreferences.displayPlanning(context)
            val displayBookName = AppPreferences.widgetDisplayBookName(context)
            val todoStates = AppPreferences.todoKeywordsSet(context)

            // Title
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

            // Deadline time
            if (displayPlanningTimes && noteView.deadlineRangeString != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(noteView.deadlineRangeString))
                row.setTextViewText(R.id.item_list_widget_deadline_text, time)
                row.setViewVisibility(R.id.item_list_widget_deadline, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_deadline, View.GONE)
            }

            // Scheduled time
            if (displayPlanningTimes && noteView.scheduledRangeString != null) {
                val time = userTimeFormatter.formatAll(OrgRange.parse(noteView.scheduledRangeString))
                row.setTextViewText(R.id.item_list_widget_scheduled_text, time)
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.GONE)
            }

            // Check mark
            if (todoStates.contains(noteView.note.state)) {
                row.setViewVisibility(R.id.item_list_widget_done, View.VISIBLE)
            } else {
                row.setViewVisibility(R.id.item_list_widget_done, View.GONE)
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
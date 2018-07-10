package com.orgzly.android.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Note;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.AgendaCursor;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.views.DbNoteViewColumns;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryParser;
import com.orgzly.android.query.user.InternalQueryParser;
import com.orgzly.android.ui.util.TitleGenerator;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.OrgHead;

import java.util.Map;

public class ListWidgetService extends RemoteViewsService {
    private static final String TAG = ListWidgetService.class.getName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        return new ListWidgetViewsFactory(getApplicationContext(), intent.getStringExtra(AppIntent.EXTRA_QUERY_STRING));
    }

    private class ListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {
        private boolean isPartitioned;
        private Cursor cursor;
        private Map<Long, AgendaCursor.NoteForDay> originalNoteIDs;

        private Context context;
        private String queryString;
        private TitleGenerator titleGenerator;
        private UserTimeFormatter userTimeFormatter;

        public ListWidgetViewsFactory(Context context, String queryString) {
            this.context = context;

            // Query string which doesn't match anything
            this.queryString = queryString != null ? queryString : ".b.a b.a";

            this.userTimeFormatter = new UserTimeFormatter(context);
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

            TitleGenerator.TitleAttributes attrs = WidgetStyle.getTitleAttributes(context);
            this.titleGenerator = new TitleGenerator(context, false, attrs);

            if (cursor != null) {
                cursor.close();
            }

            // Parse query
            QueryParser queryParser = new InternalQueryParser();
            Query query = queryParser.parse(queryString);

            isPartitioned = query.getOptions().getAgendaDays() > 0;

            // from http://stackoverflow.com/a/20645908
            final long token = Binder.clearCallingIdentity();
            try {
                if (isPartitioned) {
                    Cursor cursor = NotesClient.getCursorForQuery(context, queryString);

                    AgendaCursor.AgendaMergedCursor agendaCursor =
                            AgendaCursor.create(context, cursor, queryString);

                    this.cursor = agendaCursor.getCursor();
                    originalNoteIDs = agendaCursor.getOriginalNoteIDs();

                } else {
                    cursor = NotesClient.getCursorForQuery(context, queryString);
                }

            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void onDestroy() {
            if (cursor != null) {
                cursor.close();
            }
        }

        @Override
        public int getCount() {
            if (cursor != null) {
                return cursor.getCount();
            }
            return 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position);

            RemoteViews row = null;

            if (cursor.moveToPosition(position)) {
                String packageName = context.getPackageName();

                if (isPartitioned && AgendaCursor.isDivider(cursor)) {
                    row = new RemoteViews(packageName, R.layout.item_list_widget_divider);
                    setupDividerRow(row, cursor);
                    WidgetStyle.updateDivider(row, context);

                } else {
                    row = new RemoteViews(packageName, R.layout.item_list_widget);
                    setupNoteRow(row, cursor);
                    WidgetStyle.updateNote(row, context);
                }
            }

            return row;
        }

        private void setupDividerRow(RemoteViews row, Cursor cursor) {
            String value = AgendaCursor.getDividerDate(cursor);

            row.setTextViewText(R.id.widget_list_item_divider_value, value);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, row, cursor, value);
        }


        private void setupNoteRow(RemoteViews row, Cursor cursor) {
            Note note = NotesClient.fromCursor(cursor, true);

            if (isPartitioned) {
                Long originalId = originalNoteIDs.get(note.getId()).getNoteId();
                note.setId(originalId);
            }

            OrgHead head = note.getHead();
            row.setTextViewText(R.id.item_list_widget_title, titleGenerator.generateTitle(note, head));

            /* Notebook name. */
            if (AppPreferences.widgetDisplayBookName(context)) {
                String bookName = cursor.getString(cursor.getColumnIndex(DbNoteViewColumns.BOOK_NAME));
                row.setTextViewText(R.id.item_list_widget_book_text, bookName);
                row.setViewVisibility(R.id.item_list_widget_book, View.VISIBLE);
            } else {
                row.setViewVisibility(R.id.item_list_widget_book, View.GONE);
            }

            /* Closed time. */
            if (head.hasClosed() && AppPreferences.displayPlanning(context)) {
                row.setTextViewText(R.id.item_list_widget_closed_text, userTimeFormatter.formatAll(head.getClosed()));
                row.setViewVisibility(R.id.item_list_widget_closed, View.VISIBLE);
            } else {
                row.setViewVisibility(R.id.item_list_widget_closed, View.GONE);
            }

            /* Deadline time. */
            if (head.hasDeadline() && AppPreferences.displayPlanning(context)) {
                row.setTextViewText(R.id.item_list_widget_deadline_text, userTimeFormatter.formatAll(head.getDeadline()));
                row.setViewVisibility(R.id.item_list_widget_deadline, View.VISIBLE);
            } else {
                row.setViewVisibility(R.id.item_list_widget_deadline, View.GONE);
            }

            /* Scheduled time. */
            if (head.hasScheduled() && AppPreferences.displayPlanning(context)) {
                row.setTextViewText(R.id.item_list_widget_scheduled_text, userTimeFormatter.formatAll(head.getScheduled()));
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.VISIBLE);
            } else {
                row.setViewVisibility(R.id.item_list_widget_scheduled, View.GONE);
            }

            if (AppPreferences.todoKeywordsSet(context).contains(head.getState())) {
                row.setViewVisibility(R.id.item_list_widget_done, View.VISIBLE);
            } else {
                row.setViewVisibility(R.id.item_list_widget_done, View.GONE);
            }

            final Intent openIntent = new Intent();
            openIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.OPEN_CLICK_TYPE);
            openIntent.putExtra(AppIntent.EXTRA_NOTE_ID, note.getId());
            openIntent.putExtra(AppIntent.EXTRA_BOOK_ID, note.getPosition().getBookId());
            row.setOnClickFillInIntent(R.id.item_list_widget_layout, openIntent);

            final Intent doneIntent = new Intent();
            doneIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.DONE_CLICK_TYPE);
            doneIntent.putExtra(AppIntent.EXTRA_NOTE_ID, note.getId());
            row.setOnClickFillInIntent(R.id.item_list_widget_done, doneIntent);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, row, cursor);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return isPartitioned ? 2 : 1;
        }

        @Override
        public long getItemId(int position) {
            long id;

            if (cursor.moveToPosition(position)) {
                id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

            } else {
                id = -position;
            }

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, id);

            return id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }


}
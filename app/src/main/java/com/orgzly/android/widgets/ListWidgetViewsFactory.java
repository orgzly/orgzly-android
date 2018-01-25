package com.orgzly.android.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.provider.BaseColumns;
import android.support.v4.util.LongSparseArray;
import android.util.TypedValue;
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
import com.orgzly.android.query.Query;
import com.orgzly.android.query.QueryParser;
import com.orgzly.android.query.user.InternalQueryParser;
import com.orgzly.android.ui.util.TitleGenerator;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.OrgHead;

public class ListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = ListWidgetViewsFactory.class.getName();

    private boolean isPartitioned;
    private Cursor mCursor;
    private LongSparseArray<Long> originalNoteIDs;

    private Context mContext;
    private String queryString;
    private TitleGenerator titleGenerator;
    private UserTimeFormatter userTimeFormatter;

    public ListWidgetViewsFactory(Context mContext, String queryString) {
        this.mContext = mContext;
        // this should be a query string, which doesn't match anything
        this.queryString = queryString != null ? queryString : ".b.a b.a";

        this.userTimeFormatter = new UserTimeFormatter(mContext);
        this.titleGenerator = new TitleGenerator(mContext, false, new TitleGenerator.TitleAttributes(
                Color.rgb(0xdc, 0,0),
                Color.rgb(0, 0x80,0),
                Color.rgb(0, 0,0xff),
                // see http://stackoverflow.com/a/8296048/7757713
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, Resources.getSystem().getDisplayMetrics()),
                Color.rgb(0x69, 0x69,0x69)
        ));
    }

    @Override
    public void onCreate() { }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        // Parse query
        QueryParser queryParser = new InternalQueryParser();
        Query query = queryParser.parse(queryString);

        isPartitioned = query.getOptions().getAgendaDays() > 0;

        // from http://stackoverflow.com/a/20645908
        final long token = Binder.clearCallingIdentity();
        try {
            if (isPartitioned) {
                Cursor cursor = NotesClient.getCursorForQuery(mContext, queryString);

                AgendaCursor.AgendaMergedCursor agendaCursor =
                        AgendaCursor.INSTANCE.create(mContext, cursor, queryString);

                mCursor = agendaCursor.getCursor();
                originalNoteIDs = agendaCursor.getOriginalNoteIDs();

            } else {
                mCursor = NotesClient.getCursorForQuery(mContext, queryString);
            }

        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = null;

        if (mCursor.moveToPosition(position)) {
            String packageName = mContext.getPackageName();

            if (isPartitioned && AgendaCursor.INSTANCE.isDivider(mCursor)) {
                row = new RemoteViews(packageName, R.layout.item_list_widget_divider);
                setupDividerRow(row, mCursor);

            } else {
                row = new RemoteViews(packageName, R.layout.item_list_widget);
                setupNoteRow(row, mCursor);
            }
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, position, row.getLayoutId());

        return row;
    }

    private void setupDividerRow(RemoteViews row, Cursor cursor) {
        String value = AgendaCursor.INSTANCE.getDividerDate(cursor);

        row.setTextViewText(R.id.widget_list_item_divider_value, value);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, row, cursor, value);
    }

    private void setupNoteRow(RemoteViews row, Cursor cursor) {
        Note note = NotesClient.fromCursor(cursor);

        if (isPartitioned) {
            Long originalId = originalNoteIDs.get(note.getId());
            note.setId(originalId);
        }

        OrgHead head = note.getHead();
        row.setTextViewText(R.id.item_list_widget_title, titleGenerator.generateTitle(note, head));

        /* Closed time. */
        if (head.hasClosed() && AppPreferences.displayPlanning(mContext)) {
            row.setTextViewText(R.id.item_list_widget_closed_text, userTimeFormatter.formatAll(head.getClosed()));
            row.setViewVisibility(R.id.item_list_widget_closed, View.VISIBLE);
        } else {
            row.setViewVisibility(R.id.item_list_widget_closed, View.GONE);
        }

        /* Deadline time. */
        if (head.hasDeadline() && AppPreferences.displayPlanning(mContext)) {
            row.setTextViewText(R.id.item_list_widget_deadline_text, userTimeFormatter.formatAll(head.getDeadline()));
            row.setViewVisibility(R.id.item_list_widget_deadline, View.VISIBLE);
        } else {
            row.setViewVisibility(R.id.item_list_widget_deadline, View.GONE);
        }

        /* Scheduled time. */
        if (head.hasScheduled() && AppPreferences.displayPlanning(mContext)) {
            row.setTextViewText(R.id.item_list_widget_scheduled_text, userTimeFormatter.formatAll(head.getScheduled()));
            row.setViewVisibility(R.id.item_list_widget_scheduled, View.VISIBLE);
        } else {
            row.setViewVisibility(R.id.item_list_widget_scheduled, View.GONE);
        }

        if (AppPreferences.todoKeywordsSet(mContext).contains(head.getState())) {
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

        if (mCursor.moveToPosition(position)) {
            id = mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));

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

package com.orgzly.android.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Note;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.ui.util.TitleGenerator;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.OrgHead;

public class ListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ListWidgetViewsFactory.class.getName();

    private Cursor mCursor;
    private Context mContext;
    private String query;
    private TitleGenerator titleGenerator;
    private UserTimeFormatter userTimeFormatter;

    public ListWidgetViewsFactory(Context mContext, String queryString) {
        this.mContext = mContext;
        // this should be a query string, which doesn't match anything
        this.query = queryString != null ? queryString : ".b.a b.a";

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

        // from http://stackoverflow.com/a/20645908
        final long token = Binder.clearCallingIdentity();
        try {
            mCursor = NotesClient.getCursorForQuery(mContext, query);
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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "getViewAt", position);

        Note note = null;
        if (mCursor.moveToPosition(position)) {
            note = NotesClient.fromCursor(mCursor);
        }

        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.item_list_widget);

        if (note != null) {
            setContent(row, note);

            final Intent openIntent = new Intent();
            openIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.OPEN_CLICK_TYPE);
            openIntent.putExtra(AppIntent.EXTRA_NOTE_ID, note.getId());
            openIntent.putExtra(AppIntent.EXTRA_BOOK_ID, note.getPosition().getBookId());
            row.setOnClickFillInIntent(R.id.item_list_widget_layout, openIntent);

            final Intent doneIntent = new Intent();
            doneIntent.putExtra(AppIntent.EXTRA_CLICK_TYPE, ListWidgetProvider.DONE_CLICK_TYPE);
            doneIntent.putExtra(AppIntent.EXTRA_NOTE_ID, note.getId());
            row.setOnClickFillInIntent(R.id.item_list_widget_done, doneIntent);
        }

        return row;
    }

    private void setContent(RemoteViews row, Note note) {
        /* see also HeadsListViewAdapter.bindView */
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
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "getItemId", position);

        if (mCursor.moveToPosition(position)) {
            return NotesClient.idFromCursor(mCursor);
        }
        return -position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

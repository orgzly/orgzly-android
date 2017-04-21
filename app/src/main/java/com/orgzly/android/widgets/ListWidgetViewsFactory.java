package com.orgzly.android.widgets;

import android.content.Context;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.orgzly.R;
import com.orgzly.android.Note;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.provider.clients.NotesClient;

public class ListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor;
    private Context mContext;

    public ListWidgetViewsFactory(Context mContext) {
        this.mContext = mContext;
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
            mCursor = NotesClient.getCursorForQuery(mContext, new SearchQuery("i.todo"));
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
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        String title = "not found!";
        if (mCursor.moveToPosition(position)) {
            Note note = NotesClient.fromCursor(mCursor);
            title = note.getHead().getTitle();
        }

        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.list_widget_row);
        row.setTextViewText(android.R.id.text1, "Note: " + title); // TODO

        return row;
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
        // TODO is this correct?
        return position;
    }

    @Override
    public boolean hasStableIds() {
        // TODO is this correct?
        return true;
    }
}

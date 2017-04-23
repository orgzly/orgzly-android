package com.orgzly.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Binder;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.orgzly.R;
import com.orgzly.android.Note;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.util.NoteContentParser;
import com.orgzly.org.OrgHead;

public class ListWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final static String TITLE_SEPARATOR = "  ";

    private final TypedArrayAttributeSpans attributes;
    private Cursor mCursor;
    private Context mContext;
    private SearchQuery query;

    private class TypedArrayAttributeSpans {
        ForegroundColorSpan colorTodo;
        ForegroundColorSpan colorDone;
        ForegroundColorSpan colorUnknown;
        AbsoluteSizeSpan postTitleTextSize;
        ForegroundColorSpan postTitleTextColor;

        public TypedArrayAttributeSpans() {
            TypedArray typedArray = mContext.obtainStyledAttributes(new int[] {
                    R.attr.item_head_state_todo_color,
                    R.attr.item_head_state_done_color,
                    R.attr.item_head_state_unknown_color,
                    R.attr.item_head_post_title_text_size,
                    R.attr.item_head_post_title_text_color
            });

            colorTodo = new ForegroundColorSpan(typedArray.getColor(0, 0));
            colorDone = new ForegroundColorSpan(typedArray.getColor(1, 0));
            colorUnknown = new ForegroundColorSpan(typedArray.getColor(2, 0));

            postTitleTextSize = new AbsoluteSizeSpan(typedArray.getDimensionPixelSize(3, 0));
            postTitleTextColor = new ForegroundColorSpan(typedArray.getColor(4, 0));

            typedArray.recycle();
        }
    }

    public ListWidgetViewsFactory(Context mContext, String queryString) {
        this.mContext = mContext;
        this.query = new SearchQuery(queryString != null ? queryString : "");

        this.attributes = new TypedArrayAttributeSpans();
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
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        CharSequence title = "not found!";
        if (mCursor.moveToPosition(position)) {
            Note note = NotesClient.fromCursor(mCursor);
            title = generateTitle(note);
        }


        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.item_list_widget);
        row.setTextViewText(R.id.item_list_widget_title, title); // TODO

        return row;
    }

    private CharSequence generateTitle(Note note) {
        // TODO
        final OrgHead head = note.getHead();

        SpannableStringBuilder builder = new SpannableStringBuilder();

        /* State. */
        if (head.getState() != null) {
            builder.append(generateState(head));
        }

        /* Bold everything up until now. */
        if (builder.length() > 0) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        /* Space before title, unless there's nothing added. */
        if (builder.length() > 0) {
            builder.append(TITLE_SEPARATOR);
        }

        /* Title. */
        builder.append(NoteContentParser.fromOrg(head.getTitle()));

        return builder;
    }

    private CharSequence generateState(OrgHead head) {
        SpannableString str = new SpannableString(head.getState());

        ForegroundColorSpan color;

        if (AppPreferences.todoKeywordsSet(mContext).contains(head.getState())) {
            color = attributes.colorTodo;
        } else if (AppPreferences.doneKeywordsSet(mContext).contains(head.getState())) {
            color = attributes.colorDone;
        } else {
            color = attributes.colorUnknown;
        }

        str.setSpan(new ForegroundColorSpan(Color.rgb(200, 0, 0)), 0, str.length(), 0);

        return str;
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

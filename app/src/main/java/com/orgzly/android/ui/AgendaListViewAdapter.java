package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by pxsalehi on 11.04.17.
 */

public class AgendaListViewAdapter extends HeadsListViewAdapter {
    // assuming query always uses days
    private SearchQuery query;
    private Date start, end, cur;
    private static final int TYPE_COUNT = 2;
    public static final int TYPE_ITEM = 0;
    public static final int TYPE_SEPARATOR = 1;
    private Set<Integer> separators = new HashSet<>();
    private LayoutInflater inflater;

    public AgendaListViewAdapter(Context context, Selection selection,
                                 GesturedListViewItemMenus toolbars, boolean inBook,
                                 SearchQuery query) {
        super(context, selection, toolbars, inBook);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.query = query;
        start = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(start);
        c.add(Calendar.DAY_OF_YEAR, query.getScheduled().getValue());
        end = c.getTime();
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            return super.getView(position, convertView, parent);
        // make sure @convertView is a separator if cursor is a separator
        // otherwise create a new view
        boolean isSeperator = (boolean) convertView.getTag(R.id.IS_AGENDA_ITEM_SEPARATOR);
        int id = getCursor().getInt(getCursor().getColumnIndex(BaseColumns._ID));
        if (separators.contains(id) != isSeperator) {
            // do not use @convertView
            convertView = null;
        }
        System.out.println("Position is: " + position);
        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        if (separators.contains(id)) {
            TextView textView = new TextView(context);
            textView.setId(id);
            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
            textView.setGravity(Gravity.CENTER);
            textView.setTag(R.id.IS_AGENDA_ITEM_SEPARATOR, Boolean.TRUE);
            return textView;
        } else {
            View v = super.newView(context, cursor, parent);
            v.setTag(R.id.IS_AGENDA_ITEM_SEPARATOR, Boolean.FALSE);
            return v;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        if (separators.contains(id)) {
            TextView textView = (TextView) view;
            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
            textView.setGravity(Gravity.CENTER);
        } else {
            super.bindView(view, context, cursor);
        }
    }

    @Override
    public int getItemViewType(int id) {
        return separators.contains(id) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    public void addSeparatorItem(int id) {
        separators.add(id);
    }
}

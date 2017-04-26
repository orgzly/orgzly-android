package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
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
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (separators.contains(cursor.getPosition())) {
//            View view = inflater.inflate(R.layout.agenda_day_head, parent);
//            TextView textView = (TextView) view.findViewById(R.id.agenda_day_head);
            TextView textView = new TextView(context);

            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
            return textView;
        } else
            return super.newView(context, cursor, parent);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (separators.contains(cursor.getPosition())) {
            TextView textView = (TextView) view;
            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
        } else
            super.bindView(view, context, cursor);
    }

    @Override
    public int getItemViewType(int position) {
        return separators.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    public void addSeparatorItem(int position) {
        separators.add(position);
    }
}

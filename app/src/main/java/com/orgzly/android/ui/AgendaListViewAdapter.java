package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pxsalehi on 11.04.17.
 */

public class AgendaListViewAdapter extends HeadsListViewAdapter {
    private static final int TYPE_COUNT = 2;
    public static final int TYPE_ITEM = 0;
    public static final int TYPE_SEPARATOR = 1;
    private Set<Integer> separatorIDs = new HashSet<>();

    public AgendaListViewAdapter(Context context, Selection selection,
                                 GesturedListViewItemMenus toolbars, boolean inBook) {
        super(context, selection, toolbars, inBook);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            return super.getView(position, convertView, parent);
        // make sure @convertView is a separator if cursor is a separator
        // otherwise create a new view
        boolean isSeparator = (boolean) convertView.getTag(R.id.AGENDA_ITEM_SEPARATOR_TAG);
        Cursor cursor = (Cursor) getItem(position);
        int id = cursor.getInt(getCursor().getColumnIndex(BaseColumns._ID));
        if (separatorIDs.contains(id) != isSeparator) {
            // do not use @convertView
            return super.getView(position, null, parent);
        }
        System.out.println("Position is: " + position);

        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));

        if (separatorIDs.contains(id)) {
            TextView textView = createAgendaDateTextView(context);
            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
            textView.setTag(R.id.AGENDA_ITEM_SEPARATOR_TAG, Boolean.TRUE);
            return textView;
        } else {
            View v = super.newView(context, cursor, parent);
            v.setTag(R.id.AGENDA_ITEM_SEPARATOR_TAG, Boolean.FALSE);
            return v;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        boolean isSeparator = (boolean) view.getTag(R.id.AGENDA_ITEM_SEPARATOR_TAG);

        if (separatorIDs.contains(id) != isSeparator)
            throw new IllegalStateException("Cannot convert between agenda entry and header view types!");

        if (separatorIDs.contains(id) && isSeparator) {
            // reuse an agenda header
            TextView textView = (TextView) view;
            textView.setText(cursor.getString(cursor.getColumnIndex("day")));
        } else {
            super.bindView(view, context, cursor);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        return separatorIDs.contains(id) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    public void addSeparatorItem(int id) {
        separatorIDs.add(id);
    }

    private TextView createAgendaDateTextView(Context context) {
        View header = LayoutInflater.from(context).inflate(R.layout.agenda_day_head, null);
        return (TextView) header;
    }
}

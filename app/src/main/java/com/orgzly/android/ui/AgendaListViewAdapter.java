package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.ui.fragments.AgendaFragment;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;

/**
 * Created by pxsalehi on 11.04.17.
 */

public class AgendaListViewAdapter extends HeadsListViewAdapter {

    private static final int TYPE_COUNT = 2;
    public static final int TYPE_NOTE = 0;
    public static final int TYPE_SEPARATOR = 1;

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
        boolean isViewSeparator = (boolean) convertView.getTag(R.id.AGENDA_ITEM_SEPARATOR_TAG);
        Cursor cursor = (Cursor) getItem(position);
        boolean isCursorSeparator = getCursorType(cursor) == TYPE_SEPARATOR;
        if (isCursorSeparator != isViewSeparator) {
            // do not use @convertView
            return super.getView(position, null, parent);
        }
        System.out.println("Position is: " + position);

        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        int isSeparator = cursor.getInt(cursor.getColumnIndex(AgendaFragment.Columns.IS_SEPARATOR));

//        if (separatorIDs.contains(id)) {
        if (isSeparator == 1) {
            TextView textView = createAgendaDateTextView(context);
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaFragment.Columns.AGENDA_DAY)));
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
        boolean isCursorSeparator = getCursorType(cursor) == TYPE_SEPARATOR;
        boolean isViewSeparator = (boolean) view.getTag(R.id.AGENDA_ITEM_SEPARATOR_TAG);

        if (isCursorSeparator != isViewSeparator)
            throw new IllegalStateException("Cannot convert between agenda entry and header view types!");

        if (isCursorSeparator && isViewSeparator) {
            // reuse an agenda header
            TextView textView = (TextView) view;
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaFragment.Columns.AGENDA_DAY)));
        } else {
            super.bindView(view, context, cursor);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getCursorType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    private int getCursorType(Cursor cursor) {
        if (cursor.getInt(cursor.getColumnIndex(AgendaFragment.Columns.IS_SEPARATOR)) == 1)
            return TYPE_SEPARATOR;
        return TYPE_NOTE;
    }

    private TextView createAgendaDateTextView(Context context) {
        View header = LayoutInflater.from(context).inflate(R.layout.agenda_day_head, null);
        return (TextView) header;
    }
}

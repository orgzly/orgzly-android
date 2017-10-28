package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.ui.fragments.AgendaFragment;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;

public class AgendaListViewAdapter extends HeadsListViewAdapter {

    public static final int NOTE_TYPE = 0;
    public static final int SEPARATOR_TYPE = 1;

    private static final int TYPE_COUNT = 2;

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
        boolean isCursorSeparator = getCursorType(cursor) == SEPARATOR_TYPE;
        if (isCursorSeparator != isViewSeparator) {
            // do not use @convertView
            return super.getView(position, null, parent);
        }
        System.out.println("Position is: " + position);

        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int isSeparator = cursor.getInt(cursor.getColumnIndex(AgendaFragment.Columns.IS_SEPARATOR));

        if (isSeparator == 1) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_agenda_time, null);

            TextView textView = (TextView) view.findViewById(R.id.item_agenda_time_text);
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaFragment.Columns.AGENDA_DAY)));

            view.setTag(R.id.AGENDA_ITEM_SEPARATOR_TAG, Boolean.TRUE);
            return view;

        } else {
            View v = super.newView(context, cursor, parent);
            v.setTag(R.id.AGENDA_ITEM_SEPARATOR_TAG, Boolean.FALSE);
            return v;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean isCursorSeparator = getCursorType(cursor) == SEPARATOR_TYPE;
        boolean isViewSeparator = (boolean) view.getTag(R.id.AGENDA_ITEM_SEPARATOR_TAG);

        if (isCursorSeparator && isViewSeparator) {
            TextView textView = (TextView) view.findViewById(R.id.item_agenda_time_text);
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaFragment.Columns.AGENDA_DAY)));

            int[] margins = getMarginsForListDensity(context);
            view.setPadding(0, margins[1], 0, margins[1]);

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
            return SEPARATOR_TYPE;
        return NOTE_TYPE;
    }
}

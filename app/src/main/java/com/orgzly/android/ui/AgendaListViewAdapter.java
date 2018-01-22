package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orgzly.R;
import com.orgzly.android.provider.AgendaCursor;
import com.orgzly.android.ui.views.GesturedListViewItemMenus;

public class AgendaListViewAdapter extends HeadsListViewAdapter {
    private static final int VIEW_TYPE_COUNT = 2;

    public static final int NOTE_VIEW_TYPE = 0;
    public static final int DIVIDER_VIEW_TYPE = 1;

    public AgendaListViewAdapter(Context context, Selection selection,
                                 GesturedListViewItemMenus toolbars, boolean inBook) {
        super(context, selection, toolbars, inBook);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            return super.getView(position, null, parent);

        } else {
            boolean isViewDivider = (boolean) convertView.getTag(R.id.is_divider_view_tag);

            Cursor cursor = (Cursor) getItem(position);
            boolean isRowDivider = AgendaCursor.INSTANCE.isDivider(cursor);

            if (isRowDivider != isViewDivider) {
                // do not use @convertView
                return super.getView(position, null, parent);
            } else {
                return super.getView(position, convertView, parent);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (AgendaCursor.INSTANCE.isDivider(cursor)) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_agenda_divider, null);

            TextView textView = (TextView) view.findViewById(R.id.item_agenda_time_text);
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE)));

            view.setTag(R.id.is_divider_view_tag, Boolean.TRUE);
            return view;

        } else {
            View v = super.newView(context, cursor, parent);
            v.setTag(R.id.is_divider_view_tag, Boolean.FALSE);
            return v;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean isRowDivider = getCursorType(cursor) == DIVIDER_VIEW_TYPE;
        boolean isViewDivider = (boolean) view.getTag(R.id.is_divider_view_tag);

        if (isRowDivider && isViewDivider) {
            TextView textView = (TextView) view.findViewById(R.id.item_agenda_time_text);
            textView.setText(cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE)));

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
        return VIEW_TYPE_COUNT;
    }

    private int getCursorType(Cursor cursor) {
        if (AgendaCursor.INSTANCE.isDivider(cursor)) {
            return DIVIDER_VIEW_TYPE;
        } else {
            return NOTE_VIEW_TYPE;
        }
    }
}

package com.orgzly.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
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
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;

        if (AgendaCursor.isDivider(cursor)) {
            view = LayoutInflater.from(context).inflate(R.layout.item_agenda_divider, null);

            DividerHolder divider = (DividerHolder) view.getTag();
            if (divider == null) {
                divider = new DividerHolder(view);
                view.setTag(divider);
            }

            divider.text.setText(cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE)));

        } else {
            view = super.newView(context, cursor, parent);
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean isRowDivider = getCursorType(cursor) == DIVIDER_VIEW_TYPE;

        if (isRowDivider) {
            DividerHolder divider = (DividerHolder) view.getTag();
            String value = cursor.getString(cursor.getColumnIndex(AgendaCursor.Columns.DIVIDER_VALUE));
            divider.text.setText(value);

            int[] margins = getMarginsForListDensity(context);
            view.setPadding(view.getPaddingLeft(), margins[0], view.getPaddingRight(), margins[0]);

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
        if (AgendaCursor.isDivider(cursor)) {
            return DIVIDER_VIEW_TYPE;
        } else {
            return NOTE_VIEW_TYPE;
        }
    }

    private class DividerHolder {
        TextView text;

        DividerHolder(View view) {
            text = view.findViewById(R.id.item_agenda_time_text);
        }
    }
}

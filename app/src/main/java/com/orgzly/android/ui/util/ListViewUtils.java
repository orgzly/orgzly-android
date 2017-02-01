package com.orgzly.android.ui.util;

import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;

import java.util.TreeSet;

public class ListViewUtils {
    public static boolean isIdVisible(ListView listView, long id) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        for (int pos = firstListItemPosition; pos <= lastListItemPosition; pos++) {
            long posId = listView.getItemIdAtPosition(pos);

            if (posId == id) {
                return true;
            }
        }

        return false;
    }

    public static View getViewByPosition(ListView listView, int pos) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public static TreeSet<Long> getCheckedIds(ListView listView) {
        TreeSet<Long> ids = new TreeSet<>();

        SparseBooleanArray checked = listView.getCheckedItemPositions();

        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                long id = listView.getItemIdAtPosition(checked.keyAt(i));
                ids.add(id);
            }
        }

        return ids;
    }
}

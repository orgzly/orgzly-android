package com.orgzly.android.ui.util;

import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;

import java.util.TreeSet;

public class ListViewUtils {
    public static boolean isIdVisible(ListView listView, long id) {
        final int firstVisible = listView.getFirstVisiblePosition();
        final int lastVisible = listView.getLastVisiblePosition();

        for (int pos = firstVisible; pos <= lastVisible; pos++) {
            long posId = listView.getItemIdAtPosition(pos);

            if (posId == id) {
                return true;
            }
        }

        return false;
    }

    public static View getViewIfVisible(ListView listView, int pos) {
        return listView.getChildAt(pos - listView.getFirstVisiblePosition());
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

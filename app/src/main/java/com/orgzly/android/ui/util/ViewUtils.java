package com.orgzly.android.ui.util;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ViewUtils {
    /**
     * Returns all descendants of a specified view that are instances of specified class.
     * Starting view could be included.
     *
     * @param parent View to start the search from
     * @param klass Only objects of this type will be returned
     * @return
     */
    public static List<View> getAllChildren(View parent, Class klass) {
        List<View> result = new ArrayList<>();
        List<View> queued = new ArrayList<>();

        queued.add(parent);

        while (!queued.isEmpty()) {
            View child = queued.remove(0);

            if (klass.isInstance(child)) {
                result.add(child);
            }

            if (! (child instanceof ViewGroup)) {
                continue;
            }

            ViewGroup group = (ViewGroup) child;

            for (int i=0; i < group.getChildCount(); i++) {
                queued.add(group.getChildAt(i));
            }
        }

        return result;
    }

    public static String removeIndent(CharSequence text) {
        int indentCount = Integer.MAX_VALUE;
        String[] lines = text.toString().split("\n");
        for (String line: lines) {
            int indentCountCurrentLine = 0;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ') {
                    indentCountCurrentLine++;
                } else {
                    break;
                }
            }

            indentCount = Math.min(indentCount, indentCountCurrentLine);
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String line: lines) {
            stringBuilder.append(line.substring(indentCount));
        }

        return stringBuilder.toString();
    }
}

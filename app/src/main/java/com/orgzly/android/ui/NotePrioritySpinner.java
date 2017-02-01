package com.orgzly.android.ui;

import android.content.Context;
import android.widget.Spinner;

import com.orgzly.android.prefs.AppPreferences;

public class NotePrioritySpinner extends SpinnerValues {
    private static final String NO_VALUE_STRING = "—";

    public NotePrioritySpinner(Context context, Spinner spinner) {
        super(context, spinner);
    }

    @Override
    protected void initValues(Context context) {
        mValues.clear();

        String lastPriority = AppPreferences.minPriority(context);

        if (lastPriority == null || lastPriority.length() != 1) {
            throw new IllegalArgumentException("Last priority must be a character, not " + lastPriority);
        }

        /* Add no-priority string. */
        mValues.add(NO_VALUE_STRING);

        /* Add every priority starting from A. */
        for (char alphabet = 'A'; alphabet <= lastPriority.charAt(0); alphabet++) {
            mValues.add(String.valueOf(alphabet));
        }
    }

    /*
     * Update known values, in case they have been changed in Settings.
     */
    public void updatePossibleValues(Context context) {
        super.updatePossibleValues(context, NO_VALUE_STRING);
    }

    public String getCurrentValue() {
        return super.getCurrentValue(NO_VALUE_STRING);
    }

    @Override
    public void setCurrentValue(String val) {
        super.setCurrentValue(val != null ? val : NO_VALUE_STRING);
    }
}

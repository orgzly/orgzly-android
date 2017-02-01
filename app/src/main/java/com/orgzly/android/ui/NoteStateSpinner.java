package com.orgzly.android.ui;

import android.content.Context;
import android.widget.Spinner;

import com.orgzly.android.prefs.AppPreferences;

public class NoteStateSpinner extends SpinnerValues {
    public static final String NO_STATE_KEYWORD = "NOTE";

    public NoteStateSpinner(Context context, Spinner spinner) {
        super(context, spinner);
    }

    @Override
    protected void initValues(Context context) {
        mValues.clear();

        mValues.add(NO_STATE_KEYWORD);

        for (String state: AppPreferences.todoKeywordsSet(context)) {
            mValues.add(state);
        }

        for (String state: AppPreferences.doneKeywordsSet(context)) {
            mValues.add(state);
        }
    }

    /*
     * Update known values, in case they have been changed in Settings.
     */
    public void updatePossibleValues(Context context) {
        super.updatePossibleValues(context, NO_STATE_KEYWORD);
    }

    public String getCurrentValue() {
        return super.getCurrentValue(NO_STATE_KEYWORD);
    }

    @Override
    public void setCurrentValue(String val) {
        super.setCurrentValue(val != null ? val : NO_STATE_KEYWORD);
    }

    public static boolean isSet(String val) {
        return val != null && !val.equals(NO_STATE_KEYWORD);
    }
}

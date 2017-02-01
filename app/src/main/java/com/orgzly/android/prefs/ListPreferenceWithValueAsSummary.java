package com.orgzly.android.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * {@link ListPreference} with summary set to its current value.
 */
public class ListPreferenceWithValueAsSummary extends ListPreference {
    public ListPreferenceWithValueAsSummary(Context context) {
        super(context);
    }

    public ListPreferenceWithValueAsSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setValue(String text) {
        super.setValue(text);

        setSummary(getEntry());
    }
}

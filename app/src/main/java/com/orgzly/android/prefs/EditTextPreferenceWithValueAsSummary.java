package com.orgzly.android.prefs;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * {@link EditTextPreference} with summary set to its current value.
 */
public class EditTextPreferenceWithValueAsSummary extends EditTextPreference {
    public EditTextPreferenceWithValueAsSummary(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextPreferenceWithValueAsSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(String text) {
        super.setText(text);

        setSummary(text);
    }
}

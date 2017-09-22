package com.orgzly.android.prefs;

import android.content.Context;
import android.content.res.Resources;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import com.orgzly.R;

public class PositiveIntegerPreference extends EditTextPreference {
    public PositiveIntegerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PositiveIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        String value = getEditText().getText().toString();
        super.onDialogClosed(validateIntegerValue(value) != null);
    }


    @Override
    public void setText(String text) {
        text = validateIntegerValue(text);

        if (text != null) {
            super.setText(text);

            setSummary(text);
        }
    }

    private String validateIntegerValue(String s) {
        if (s != null && s.length() > 0) {
            try {
                int n = Integer.parseInt(s);
                if (n > 0) {
                    return String.valueOf(n);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

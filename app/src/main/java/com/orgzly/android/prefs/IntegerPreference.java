package com.orgzly.android.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import com.orgzly.R;

public class IntegerPreference extends EditTextPreference {
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;

    public IntegerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttrs(attrs);
    }

    public IntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs);
    }

    private void parseAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.IntegerRange);

            min = typedArray.getInt(R.styleable.IntegerRange_min, Integer.MIN_VALUE);
            max = typedArray.getInt(R.styleable.IntegerRange_max, Integer.MAX_VALUE);

            typedArray.recycle();
        }
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
                if (min <= n && n <= max) {
                    return String.valueOf(n);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

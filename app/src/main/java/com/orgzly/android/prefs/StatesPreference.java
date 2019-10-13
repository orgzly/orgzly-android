package com.orgzly.android.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.orgzly.R;

public class StatesPreference extends DialogPreference {
    public StatesPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    public StatesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    public StatesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    public StatesPreference(Context context) {
        super(context);
        setDialogLayoutResource(R.layout.pref_dialog_states);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        // AppPreferences.states(getContext(), (String) defaultValue);
    }

    @Override
    public CharSequence getSummary() {
        String s = AppPreferences.states(getContext()).trim();

        if ("|".equals(s)) {
            s = getContext().getString(R.string.no_states_defined);
        }

        return s;
    }
}

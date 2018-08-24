package com.orgzly.android.ui;

import android.content.Context;
import androidx.appcompat.widget.AppCompatSpinner;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.orgzly.R;

import java.util.ArrayList;
import java.util.List;

public abstract class SpinnerValues {
    protected Spinner mSpinner;

    protected List<String> mValues;

    /**
     * @param spinner {@link Spinner} {@code View} to use. If {@code null}, one will be created
     */
    public SpinnerValues(Context context, Spinner spinner) {
        /* Create Spinner View, if one is not provided. */
        if (spinner != null) {
            mSpinner = spinner;
        } else {
            mSpinner = new AppCompatSpinner(context);
        }

        mValues = new ArrayList<>();

        initValues(context);

        setAdapter();
    }

    abstract protected void initValues(Context context);

    protected void setAdapter() {
        if (mSpinner.getContext() == null) {
            throw new IllegalStateException("Spinner's Context is null");
        }

        /* Create adapter. */
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                mSpinner.getContext(),
                R.layout.spinner_item,
                mValues);

        adapter.setDropDownViewResource(R.layout.dropdown_item);

        /* Set adapter for Spinner. */
        mSpinner.setAdapter(adapter);
    }

    public Spinner getSpinner() {
        return mSpinner;
    }

    public List<String> getValues() {
        return mValues;
    }

    /**
     * @return current selected state or {@code null}
     */
    protected String getCurrentValue(String noValueString) {
        String spinnerVal = (String) getSpinner().getSelectedItem();

        if (spinnerVal == null || noValueString.equals(spinnerVal)) {
            return null;
        } else {
            return spinnerVal;
        }
    }

    protected void setCurrentValue(String val) {
        int pos = mValues.indexOf(val);

        /* If value is missing from current adapter, create a new adapter and update the Spinner. */
        if (pos == -1) {
            /* Re-initialize values. */
            initValues(mSpinner.getContext());

            /* Add missing value. */
            mValues.add(val);

            setAdapter();
        }

        /* Try again. */
        pos = mValues.indexOf(val);

        mSpinner.setSelection(pos);
    }

    /**
     * Update with known values, in case they have been changed in Settings.
     */
    public void updatePossibleValues(Context context, String noValueString) {
        /* Get the current value. */
        String currentValue = getCurrentValue(noValueString);

        initValues(context);

        setAdapter();

        /* Restore the current value. */
        setCurrentValue(currentValue);
    }
}

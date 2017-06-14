package com.orgzly.android.ui.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import com.orgzly.R;
import com.orgzly.org.datetime.OrgInterval;
import com.orgzly.org.datetime.OrgRepeater;


/**
 * A dialog that prompts the user for the repeater.
 */
public class RepeaterPickerDialog extends AlertDialog
        implements OnClickListener, OnTimeChangedListener {

    private static final String TYPE = "type";
    private static final String UNIT = "unit";
    private static final String VALUE = "value";

    private final NumberPicker mRepeaterType;
    private final NumberPicker mRepeaterValue;
    private final NumberPicker mRepeaterUnit;
    private final TextView mRepeaterDesc;

    private final OnRepeaterSetListener mRepeaterSetCallback;

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Done' button).
     */
    public interface OnRepeaterSetListener {

        /**
         * @param repeater
         */
        void onRepeaterSet(OrgRepeater repeater);
    }

    /**
     * @param context Parent.
     * @param callBack How parent is notified.

     */
    public RepeaterPickerDialog(Context context, OnRepeaterSetListener callBack, String repeaterStr) {
        this(context, 0, callBack, repeaterStr);
    }

    /**
     * @param context Parent.
     * @param theme the theme to apply to this dialog
     * @param callBack How parent is notified.
     */
    public RepeaterPickerDialog(Context context, int theme, OnRepeaterSetListener callBack, String repeaterStr) {
        super(context);

        mRepeaterSetCallback = callBack;

        final Context themeContext = getContext();
        final LayoutInflater inflater = LayoutInflater.from(themeContext);
        final View view = inflater.inflate(R.layout.dialog_repeater, null);
        setView(view);
        setButton(BUTTON_POSITIVE, themeContext.getString(R.string.ok), this);
        setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.cancel), this);


        String[] types = getContext().getResources().getStringArray(R.array.repeater_types);
        String[] units = getContext().getResources().getStringArray(R.array.time_units);

        mRepeaterType = (NumberPicker) view.findViewById(R.id.dialog_timestamp_repeater_type);
        mRepeaterType.setMinValue(0);
        mRepeaterType.setMaxValue(types.length - 1);
        mRepeaterType.setDisplayedValues(types);

        mRepeaterType.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateRepeaterDescription(newVal);
            }
        });

        mRepeaterValue = (NumberPicker) view.findViewById(R.id.dialog_timestamp_repeater_value);
        mRepeaterValue.setMinValue(1);
        mRepeaterValue.setMaxValue(100);
        mRepeaterValue.setWrapSelectorWheel(false);

        mRepeaterUnit = (NumberPicker) view.findViewById(R.id.dialog_timestamp_repeater_unit);
        mRepeaterUnit.setMinValue(0);
        mRepeaterUnit.setMaxValue(units.length - 1);
        mRepeaterUnit.setDisplayedValues(units);
        mRepeaterUnit.setWrapSelectorWheel(false);

        mRepeaterDesc = (TextView) view.findViewById(R.id.dialog_timestamp_repeater_description);

        setViewsFromString(repeaterStr);

        setTitle(R.string.repeat_dialog_title);
    }


    private void updateRepeaterDescription(int newVal) {
        mRepeaterDesc.setText(getContext().getResources().getStringArray(R.array.repeater_types_desc)[newVal]);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        /* do nothing */
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                if (mRepeaterSetCallback != null) {
                    OrgRepeater repeater = getRepeaterFromViews();

                    mRepeaterSetCallback.onRepeaterSet(repeater);
                }
                break;

            case BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

    private void setViewsFromString(String repeaterValue) {
        OrgRepeater repeater = OrgRepeater.parse(repeaterValue);

        mRepeaterType.setValue(repeater.getType().ordinal());

        /* Increase the maximum if needed. */
        if (mRepeaterValue.getMaxValue() < repeater.getValue()) {
            mRepeaterValue.setMaxValue(repeater.getValue());

            /* Has to be called after setting minimum and maximum values,
             * per http://stackoverflow.com/a/21065844
             */
            mRepeaterValue.setWrapSelectorWheel(false);
        }

        mRepeaterValue.setValue(repeater.getValue());

        mRepeaterUnit.setValue(repeater.getUnit().ordinal());

        updateRepeaterDescription(mRepeaterType.getValue());
    }

    private OrgRepeater getRepeaterFromViews() {
        OrgRepeater.Type type;
        int value;
        OrgInterval.Unit unit;

        switch (mRepeaterType.getValue()) {
            case 0:
                type = OrgRepeater.Type.CUMULATE;
                break;
            case 1:
                type = OrgRepeater.Type.CATCH_UP;
                break;
            case 2:
                type = OrgRepeater.Type.RESTART;
                break;
            default:
                throw new IllegalArgumentException("Unexpected spinner position for current repeater type: " + mRepeaterType.getValue());
        }

        value = mRepeaterValue.getValue();

        switch (mRepeaterUnit.getValue()) {
            case 0:
                unit = OrgInterval.Unit.HOUR;
                break;
            case 1:
                unit = OrgInterval.Unit.DAY;
                break;
            case 2:
                unit = OrgInterval.Unit.WEEK;
                break;
            case 3:
                unit = OrgInterval.Unit.MONTH;
                break;
            case 4:
                unit = OrgInterval.Unit.YEAR;
                break;
            default:
                throw new IllegalArgumentException("Unexpected spinner position for current repeater unit: " + mRepeaterType.getValue());
        }

        return new OrgRepeater(type, value, unit);
    }

    /**
     * Sets the current time.

     */
//    public void updateTime(int hourOfDay, int minuteOfHour) {
//        mTimePicker.setCurrentHour(hourOfDay);
//        mTimePicker.setCurrentMinute(minuteOfHour);
//    }

    @Override
    public Bundle onSaveInstanceState() {
        final Bundle state = super.onSaveInstanceState();

        state.putInt(TYPE, mRepeaterType.getValue());
        state.putInt(VALUE, mRepeaterValue.getValue());
        state.putInt(UNIT, mRepeaterUnit.getValue());

        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mRepeaterType.setValue(savedInstanceState.getInt(TYPE));
        mRepeaterValue.setValue(savedInstanceState.getInt(VALUE));
        mRepeaterUnit.setValue(savedInstanceState.getInt(UNIT));
    }
}

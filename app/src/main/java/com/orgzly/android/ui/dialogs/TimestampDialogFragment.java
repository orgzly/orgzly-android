package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDelay;
import com.orgzly.org.datetime.OrgRepeater;

import java.util.Calendar;
import java.util.Set;
import java.util.TreeSet;

public class TimestampDialogFragment extends DialogFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = TimestampDialogFragment.class.getName();

    public static final String FRAGMENT_TAG = TimestampDialogFragment.class.getName();

    private static final String ARG_DIALOG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_NOTE_IDS = "note_ids";
    private static final String ARG_TIME = "time";

    private static final String ARG_YEAR = "year";
    private static final String ARG_MONTH = "month";
    private static final String ARG_DAY = "day";

    private static final String ARG_USE_TIME = "use_time";
    private static final String ARG_HOUR = "hour";
    private static final String ARG_MINUTE = "minute";

    private static final String ARG_REPEATER = "repeater";
    private static final String ARG_USE_REPEAT = "use_repeat";

    private static final String ARG_DELAY = "delay";

    private static final String ARG_END_TIME_HOUR = "end_time_hour";
    private static final String ARG_END_TIME_MINUTE = "end_time_minute";

    private AlertDialog mDialog;

    /** Use by caller to know what's the timestamp for (scheduled, deadline, etc.). */
    private int mId;

    private TreeSet<Long> mNoteIds;

    private OnDateTimeSetListener mActivityListener;
    private Context mContext;
    private UserTimeFormatter mUserTimeFormatter;

    private Button mDatePicker;

    private Button mTimePicker;
    private CompoundButton mIsTimeUsed;

    private Button mRepeaterPicker;
    private CompoundButton mIsRepeaterUsed;

    private OrgDelay delay;

    private int mCurrentYear;
    private int mCurrentMonth;
    private int mCurrentDay;

    private int mCurrentHour;
    private int mCurrentMinute;

    private int mCurrentEndTimeHour;
    private int mCurrentEndTimeMinute;

    /* Keeping reference to avoid leak. */
    private AlertDialog pickerDialog;

    /**
     * @param id unique ID passed to every callback method. Useful for identifying dialog's invoker
     * @param time
     * @return
     */
    public static TimestampDialogFragment getInstance(int id, int title, long noteId, OrgDateTime time) {
        return TimestampDialogFragment.getInstance(id, title, MiscUtils.set(noteId), time);
    }

    public static TimestampDialogFragment getInstance(int id, int title, Set<Long> noteIds, OrgDateTime time) {
        TimestampDialogFragment fragment = new TimestampDialogFragment();

        /* Set arguments for fragment. */
        Bundle bundle = new Bundle();

        bundle.putInt(ARG_DIALOG_ID, id);
        bundle.putInt(ARG_TITLE, title);
        bundle.putLongArray(ARG_NOTE_IDS, toArray(noteIds));

        if (time != null) {
            bundle.putString(ARG_TIME, time.toString());
        }

        fragment.setArguments(bundle);

        return fragment;
    }

    private static long[] toArray(Set<Long> set) {
        int i = 0;
        long[] result = new long[set.size()];

        for (long e : set) {
            result[i++] = e;
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putInt(ARG_YEAR, mCurrentYear);
        state.putInt(ARG_MONTH, mCurrentMonth);
        state.putInt(ARG_DAY, mCurrentDay);

        state.putBoolean(ARG_USE_TIME, mIsTimeUsed.isChecked());
        state.putInt(ARG_HOUR, mCurrentHour);
        state.putInt(ARG_MINUTE, mCurrentMinute);

        state.putString(ARG_REPEATER, mRepeaterPicker.getText().toString());
        state.putBoolean(ARG_USE_REPEAT, mIsRepeaterUsed.isChecked());

        if (delay != null) {
            state.putString(ARG_DELAY, delay.toString());
        }

        state.putInt(ARG_END_TIME_HOUR, mCurrentEndTimeHour);
        state.putInt(ARG_END_TIME_MINUTE, mCurrentEndTimeMinute);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (pickerDialog != null) {
            pickerDialog.dismiss();
            pickerDialog = null;
        }
    }

    /**
     * Create a new instance of a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);


        mUserTimeFormatter = new UserTimeFormatter(getActivity());

        /* This makes sure that the fragment has implemented
         * the callback interface. If not, it throws an exception
         */
        if (!(getParentFragment() instanceof OnDateTimeSetListener)) {
            throw new IllegalStateException("Fragment " + getParentFragment() + " must implement " + OnDateTimeSetListener.class);
        }

        mActivityListener = (OnDateTimeSetListener) getParentFragment();


        mContext = getActivity();
        mId = getArguments().getInt(ARG_DIALOG_ID);

        mNoteIds = new TreeSet<>();
        for (long e : getArguments().getLongArray(ARG_NOTE_IDS)) {
            mNoteIds.add(e);
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_timestamp, null, false);


        mDatePicker = (Button) view.findViewById(R.id.dialog_timestamp_date_picker);

        mTimePicker = (Button) view.findViewById(R.id.dialog_timestamp_time_picker);
        mIsTimeUsed = (CompoundButton) view.findViewById(R.id.dialog_timestamp_time_check);

        mRepeaterPicker = (Button) view.findViewById(R.id.dialog_timestamp_repeater_picker);
        mIsRepeaterUsed = (CompoundButton) view.findViewById(R.id.dialog_timestamp_repeater_check);

        /* Set before toggle buttons are setup, as they trigger dialog title update .*/
        setValues(OrgDateTime.parseOrNull(getArguments().getString(ARG_TIME)));

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(getArguments().getInt(ARG_TITLE))
                .setView(view)
                .setPositiveButton(R.string.set, (dialog, which) -> {
                    if (mActivityListener != null) {
                        OrgDateTime time = getCurrentOrgTime();
                        mActivityListener.onDateTimeSet(mId, mNoteIds, time);
                    }
                })
                .setNeutralButton(R.string.clear, (dialog, which) -> {
                    if (mActivityListener != null) {
                        mActivityListener.onDateTimeCleared(mId, mNoteIds);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (mActivityListener != null) {
                        mActivityListener.onDateTimeAborted(mId, mNoteIds);
                    }
                })
                .create();

        mDatePicker.setOnClickListener(this);
        mTimePicker.setOnClickListener(this);
        mRepeaterPicker.setOnClickListener(this);

        /*
         * These callbacks are called not only on user press, but during initialization as well.
         * It's important that they are *after* dialog has been created.
         */
        mIsTimeUsed.setOnCheckedChangeListener(this);
        mIsRepeaterUsed.setOnCheckedChangeListener(this);

        view.findViewById(R.id.dialog_timestamp_today_shortcut).setOnClickListener(this);
        view.findViewById(R.id.dialog_timestamp_tomorrow_shortcut).setOnClickListener(this);
        view.findViewById(R.id.dialog_timestamp_next_week_shortcut).setOnClickListener(this);
        view.findViewById(R.id.dialog_timestamp_time_icon).setOnClickListener(this);
        view.findViewById(R.id.dialog_timestamp_repeater_icon).setOnClickListener(this);

        restoreState(savedInstanceState);

        setViewsFromCurrentValues();

        return mDialog;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentYear = savedInstanceState.getInt(ARG_YEAR);
            mCurrentMonth = savedInstanceState.getInt(ARG_MONTH);
            mCurrentDay = savedInstanceState.getInt(ARG_DAY);

            mIsTimeUsed.setChecked(savedInstanceState.getBoolean(ARG_USE_TIME));
            mCurrentHour = savedInstanceState.getInt(ARG_HOUR);
            mCurrentMinute = savedInstanceState.getInt(ARG_MINUTE);

            mRepeaterPicker.setText(savedInstanceState.getString(ARG_REPEATER));
            mIsRepeaterUsed.setChecked(savedInstanceState.getBoolean(ARG_USE_REPEAT));

            if (savedInstanceState.getString(ARG_DELAY) != null) {
                delay = OrgDelay.parse(savedInstanceState.getString(ARG_DELAY));
            }

            mCurrentEndTimeHour = savedInstanceState.getInt(ARG_END_TIME_HOUR);
            mCurrentEndTimeMinute = savedInstanceState.getInt(ARG_END_TIME_MINUTE);
        }
    }

    /**
     * Sets values from {@link OrgDateTime}.
     * Uses *now* if time is not provided.
     */
    private void setValues(OrgDateTime time) {
        Calendar cal;

        // TODO: Make it configurable
        Calendar defaultTime = Calendar.getInstance();
        defaultTime.add(Calendar.HOUR, 1);

        if (time != null) {
            cal = time.getCalendar();
        } else {
            cal = defaultTime;
        }

        mCurrentYear = cal.get(Calendar.YEAR);
        mCurrentMonth = cal.get(Calendar.MONTH);
        mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

        mIsTimeUsed.setChecked(time != null && time.hasTime());

        /* If there is no time part, set it to default's. */
        if (time != null && !time.hasTime()) {
            mCurrentHour = defaultTime.get(Calendar.HOUR_OF_DAY);
            mCurrentMinute = defaultTime.get(Calendar.MINUTE);
        } else {
            mCurrentHour = cal.get(Calendar.HOUR_OF_DAY);
            mCurrentMinute = cal.get(Calendar.MINUTE);
        }

        mCurrentEndTimeHour = cal.get(Calendar.HOUR_OF_DAY);
        mCurrentEndTimeMinute = cal.get(Calendar.MINUTE);

        if (time != null) {
            mIsRepeaterUsed.setChecked(time.hasRepeater());

            if (time.hasRepeater()) {
                mRepeaterPicker.setText(time.getRepeater().toString());
            }

            if (time.hasDelay()) {
                delay = time.getDelay();
            }
        }
    }

    @Override
    public void onClick(View v) {
        Calendar cal;

        switch (v.getId()) {
            case R.id.dialog_timestamp_date_picker:
                pickerDialog = new DatePickerDialog(mContext, (view, year, monthOfYear, dayOfMonth) -> {
                    mCurrentYear = year;
                    mCurrentMonth = monthOfYear;
                    mCurrentDay = dayOfMonth;

                    setViewsFromCurrentValues();
                }, mCurrentYear, mCurrentMonth, mCurrentDay);
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;

            case R.id.dialog_timestamp_time_picker:
            case R.id.dialog_timestamp_time_icon:
                pickerDialog = new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
                    mCurrentHour = hourOfDay;
                    mCurrentMinute = minute;

                    mIsTimeUsed.setChecked(true);

                    setViewsFromCurrentValues();
                }, mCurrentHour, mCurrentMinute, DateFormat.is24HourFormat(getContext()));
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;

            case R.id.dialog_timestamp_repeater_picker:
            case R.id.dialog_timestamp_repeater_icon:
                pickerDialog = new RepeaterPickerDialog(mContext, repeater -> {
                    mRepeaterPicker.setText(repeater.toString());
                    mIsRepeaterUsed.setChecked(true);

                    setViewsFromCurrentValues();
                }, mRepeaterPicker.getText().toString());
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;


            case R.id.dialog_timestamp_today_shortcut:
                cal = Calendar.getInstance();

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
                break;

            case R.id.dialog_timestamp_tomorrow_shortcut:
                cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
                break;

            case R.id.dialog_timestamp_next_week_shortcut:
                cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.add(Calendar.DATE, 7);

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.dialog_timestamp_time_check:
                setViewsFromCurrentValues();
                break;

            case R.id.dialog_timestamp_repeater_check:
                setViewsFromCurrentValues();
                break;
        }

    }

    private OrgDateTime getCurrentOrgTime() {
        OrgDateTime.Builder builder = new OrgDateTime.Builder()
                .setIsActive(true)

                .setYear(mCurrentYear)
                .setMonth(mCurrentMonth)

                .setHasTime(mIsTimeUsed.isChecked())
                .setDay(mCurrentDay)
                .setHour(mCurrentHour)
                .setMinute(mCurrentMinute);

        if (mIsRepeaterUsed.isChecked()) {
            OrgRepeater repeater = OrgRepeater.parse(mRepeaterPicker.getText().toString());
            builder.setHasRepeater(true);
            builder.setRepeater(repeater);
        }

        if (delay != null) {
            builder.setDelay(delay);
        }

        return builder.build();
    }

    private void setViewsFromCurrentValues() {
        /*
         * Check if dialog has been created.
         * Toggle buttons get fired on initialization, calling this method.
         * Should not happen before with the current ordering of setup methods, but just in case.
         */
        if (mDialog != null) {
            OrgDateTime time = getCurrentOrgTime();

            mDatePicker.setText(mUserTimeFormatter.formatDate(time));
            mTimePicker.setText(mUserTimeFormatter.formatTime(time));

            if (time.hasRepeater()) {
                mRepeaterPicker.setText(mUserTimeFormatter.formatRepeater(time));
            }

            // mEndTimePicker.setText(mOrgTimeFormatter.formatEndTime(time));

            mDialog.setTitle(mUserTimeFormatter.formatAll(time));
        }
    }

    /**
     * The callback used to indicate the user is done filling in the date.
     */
    public interface OnDateTimeSetListener {
        void onDateTimeSet(int id, TreeSet<Long> noteIds, OrgDateTime time);
        void onDateTimeCleared(int id, TreeSet<Long> noteIds);
        void onDateTimeAborted(int id, TreeSet<Long> noteIds);
    }
}
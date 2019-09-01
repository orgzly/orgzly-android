package com.orgzly.android.ui.dialogs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;

import androidx.fragment.app.DialogFragment;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.databinding.DialogTimestampBinding;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDelay;
import com.orgzly.org.datetime.OrgRepeater;

import java.util.Calendar;
import java.util.Collections;
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

    private DialogTimestampBinding binding;

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
        return TimestampDialogFragment.getInstance(id, title, Collections.singleton(noteId), time);
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

        state.putBoolean(ARG_USE_TIME, binding.timeUsedCheckbox.isChecked());
        state.putInt(ARG_HOUR, mCurrentHour);
        state.putInt(ARG_MINUTE, mCurrentMinute);

        state.putString(ARG_REPEATER, binding.repeaterPickerButton.getText().toString());
        state.putBoolean(ARG_USE_REPEAT, binding.repeaterUsedCheckbox.isChecked());

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

        binding = DialogTimestampBinding.inflate(LayoutInflater.from(getActivity()));

        /* Set before toggle buttons are setup, as they trigger dialog title update .*/
        setValues(OrgDateTime.parseOrNull(getArguments().getString(ARG_TIME)));

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(getArguments().getInt(ARG_TITLE))
                .setView(binding.getRoot())
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

        binding.datePickerButton.setOnClickListener(this);
        binding.timePickerButton.setOnClickListener(this);
        binding.repeaterPickerButton.setOnClickListener(this);

        /*
         * These callbacks are called not only on user press, but during initialization as well.
         * It's important that they are *after* dialog has been created.
         */
        binding.timeUsedCheckbox.setOnCheckedChangeListener(this);
        binding.repeaterUsedCheckbox.setOnCheckedChangeListener(this);

        binding.todayButton.setOnClickListener(this);
        binding.tomorrowButton.setOnClickListener(this);
        binding.nextWeekButton.setOnClickListener(this);
        binding.timeIcon.setOnClickListener(this);
        binding.repeaterIcon.setOnClickListener(this);

        restoreState(savedInstanceState);

        setViewsFromCurrentValues();

        return mDialog;
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentYear = savedInstanceState.getInt(ARG_YEAR);
            mCurrentMonth = savedInstanceState.getInt(ARG_MONTH);
            mCurrentDay = savedInstanceState.getInt(ARG_DAY);

            binding.timeUsedCheckbox.setChecked(savedInstanceState.getBoolean(ARG_USE_TIME));
            mCurrentHour = savedInstanceState.getInt(ARG_HOUR);
            mCurrentMinute = savedInstanceState.getInt(ARG_MINUTE);

            binding.repeaterPickerButton.setText(savedInstanceState.getString(ARG_REPEATER));
            binding.repeaterUsedCheckbox.setChecked(savedInstanceState.getBoolean(ARG_USE_REPEAT));

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

        binding.timeUsedCheckbox.setChecked(time != null && time.hasTime());

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
            binding.repeaterUsedCheckbox.setChecked(time.hasRepeater());

            if (time.hasRepeater()) {
                binding.repeaterPickerButton.setText(time.getRepeater().toString());
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
            case R.id.date_picker_button:
                pickerDialog = new DatePickerDialog(mContext, (view, year, monthOfYear, dayOfMonth) -> {
                    mCurrentYear = year;
                    mCurrentMonth = monthOfYear;
                    mCurrentDay = dayOfMonth;

                    setViewsFromCurrentValues();
                }, mCurrentYear, mCurrentMonth, mCurrentDay);
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;

            case R.id.time_picker_button:
            case R.id.time_icon:
                pickerDialog = new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
                    mCurrentHour = hourOfDay;
                    mCurrentMinute = minute;

                    binding.timeUsedCheckbox.setChecked(true);

                    setViewsFromCurrentValues();
                }, mCurrentHour, mCurrentMinute, DateFormat.is24HourFormat(getContext()));
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;

            case R.id.repeater_picker_button:
            case R.id.repeater_icon:
                pickerDialog = new RepeaterPickerDialog(mContext, repeater -> {
                    binding.repeaterPickerButton.setText(repeater.toString());
                    binding.repeaterUsedCheckbox.setChecked(true);

                    setViewsFromCurrentValues();
                }, binding.repeaterPickerButton.getText().toString());
                pickerDialog.setOnDismissListener(dialog -> pickerDialog = null);
                pickerDialog.show();
                break;


            case R.id.today_button:
                cal = Calendar.getInstance();

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
                break;

            case R.id.tomorrow_button:
                cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
                break;

            case R.id.next_week_button:
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
            case R.id.time_used_checkbox:
                setViewsFromCurrentValues();
                break;

            case R.id.repeater_used_checkbox:
                setViewsFromCurrentValues();
                break;
        }

    }

    private OrgDateTime getCurrentOrgTime() {
        OrgDateTime.Builder builder = new OrgDateTime.Builder()
                .setIsActive(true)

                .setYear(mCurrentYear)
                .setMonth(mCurrentMonth)

                .setHasTime(binding.timeUsedCheckbox.isChecked())
                .setDay(mCurrentDay)
                .setHour(mCurrentHour)
                .setMinute(mCurrentMinute);

        if (binding.repeaterUsedCheckbox.isChecked()) {
            OrgRepeater repeater = OrgRepeater.parse(binding.repeaterPickerButton.getText().toString());
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

            binding.datePickerButton.setText(mUserTimeFormatter.formatDate(time));
            binding.timePickerButton.setText(mUserTimeFormatter.formatTime(time));

            if (time.hasRepeater()) {
                binding.repeaterPickerButton.setText(mUserTimeFormatter.formatRepeater(time));
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
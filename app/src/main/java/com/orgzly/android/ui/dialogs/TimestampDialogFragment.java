package com.orgzly.android.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.UserTimeFormatter;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRepeater;

import java.util.Calendar;
import java.util.TreeSet;

public class TimestampDialogFragment extends DialogFragment {
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

    private static final String ARG_USE_REPEAT = "use_repeat";
    private static final String ARG_IS_ACTIVE = "is_active";

    private static final String ARG_USE_END_TIME = "use_end_time";
    private static final String ARG_END_TIME_HOUR = "end_time_hour";
    private static final String ARG_END_TIME_MINUTE = "end_time_minute";

    private AlertDialog mDialog;

    /** Use by caller to know what's the timestamp for (scheduled, deadline, etc.). */
    private int mId;

    private TreeSet<Long> mNoteIds;

    private OnDateTimeSetListener mActivityListener;
    private Context mContext;
    private UserTimeFormatter mUserTimeFormatter;


    /*
     * Buttons.
     */

    private CompoundButton mIsActive;

    private Button mDatePicker;

    private Button mTimePicker;
    private CompoundButton mIsTimeUsed;

    private Button mRepeaterPicker;
    private CompoundButton mIsRepeaterUsed;

    private Button mEndTimePicker;
    private CompoundButton mEndTime;

    /*
     *
     */

    private int mCurrentYear;
    private int mCurrentMonth;
    private int mCurrentDay;

    private int mCurrentHour;
    private int mCurrentMinute;
    //    private EditText mRepeatValue;
//    private Spinner mRepeatUnit;
    private int mCurrentEndTimeHour;
    private int mCurrentEndTimeMinute;


    /* Without these, if creating local variables, getting:
     *
     * 11-05 19:44:20.080 E/WindowManager( 4250): android.view.WindowLeaked:
     * Activity com.orgzly.android.ui.MainActivity has leaked window
     * com.android.internal.policy.impl.PhoneWindow$DecorView{2a02b17e V.E..... R.....I. 0,0-800,442}
     * that was originally added here
     */
    private DatePickerDialog mDatePickerDialog;
    private TimePickerDialog mTimePickerDialog;
    private RepeaterPickerDialog mRepeaterPickerDialog;
    private TimePickerDialog mEndTimePickerDialog;

    /**
     * @param id unique ID passed to every callback method. Useful for identifying dialog's invoker
     * @param time
     * @return
     */
    public static TimestampDialogFragment getInstance(int id, int title, long noteId, OrgDateTime time) {
        TreeSet<Long> noteIds = new TreeSet<>();
        noteIds.add(noteId);

        return TimestampDialogFragment.getInstance(id, title, noteIds, time);
    }

    public static TimestampDialogFragment getInstance(int id, int title, TreeSet<Long> noteIds, OrgDateTime time) {
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

    private static long[] toArray(TreeSet<Long> set) {
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

        state.putBoolean(ARG_USE_REPEAT, mIsRepeaterUsed.isChecked());
        state.putBoolean(ARG_IS_ACTIVE, mIsActive.isChecked());

        state.putBoolean(ARG_USE_END_TIME, mEndTime.isChecked());
        state.putInt(ARG_END_TIME_HOUR, mCurrentEndTimeHour);
        state.putInt(ARG_END_TIME_MINUTE, mCurrentEndTimeMinute);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, savedInstanceState);
        super.onCreate(savedInstanceState);
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
        if (!(getTargetFragment() instanceof OnDateTimeSetListener)) {
            throw new IllegalStateException("Fragment " + getTargetFragment() + " must implement " + OnDateTimeSetListener.class);
        }
        mActivityListener = (OnDateTimeSetListener) getTargetFragment();


        mContext = getActivity();
        mId = getArguments().getInt(ARG_DIALOG_ID);

        mNoteIds = new TreeSet<>();
        for (long e : getArguments().getLongArray(ARG_NOTE_IDS)) {
            mNoteIds.add(e);
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_timestamp, null, false);


        mIsActive = (CompoundButton) view.findViewById(R.id.dialog_timestamp_is_active);

        mDatePicker = (Button) view.findViewById(R.id.dialog_timestamp_date_picker);

        mTimePicker = (Button) view.findViewById(R.id.dialog_timestamp_time_picker);
        mIsTimeUsed = (CompoundButton) view.findViewById(R.id.dialog_timestamp_time);

        mRepeaterPicker = (Button) view.findViewById(R.id.dialog_timestamp_repeater);
        mIsRepeaterUsed = (CompoundButton) view.findViewById(R.id.dialog_timestamp_repeat);

        mEndTimePicker = (Button) view.findViewById(R.id.dialog_timestamp_end_time_picker);
        mEndTime = (CompoundButton) view.findViewById(R.id.dialog_timestamp_end_time);

        /*
         * Also make labels toggle the compound buttons.
         */

        view.findViewById(R.id.dialog_timestamp_is_active_label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsActive.toggle();
            }
        });

//        view.findViewById(R.id.dialog_timestamp_time_label).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mIsTimeUsed.toggle();
//            }
//        });
//
//        view.findViewById(R.id.dialog_timestamp_repeat_label).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mIsRepeaterUsed.toggle();
//            }
//        });
//
//        view.findViewById(R.id.dialog_timestamp_end_time_label).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mEndTime.toggle();
//            }
//        });


//        mRepeatValue = (EditText) view.findViewById(R.id.dialog_timestamp_repeat_value);
//        mRepeatUnit = (Spinner) view.findViewById(R.id.dialog_timestamp_repeat_unit);


        /* Set before toggle buttons are setup, as they trigger dialog title update .*/
        setValues(OrgDateTime.parseOrNull(getArguments().getString(ARG_TIME)));

//        mDialog = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.TimestampDialog))
        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(getArguments().getInt(ARG_TITLE))
                .setView(view)
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mActivityListener != null) {
                            OrgDateTime time = getCurrentOrgTime();
                            mActivityListener.onDateTimeSet(mId, mNoteIds, time);
                        }
                    }
                })
                .setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mActivityListener != null) {
                            mActivityListener.onDateTimeCleared(mId, mNoteIds);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mActivityListener != null) {
                            mActivityListener.onDateTimeAborted(mId, mNoteIds);
                        }
                    }
                })
                .create();

        setupDateShortcutsButtons(view);
        setupPickerButtons();
        setupToggleButtons();

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

            mIsRepeaterUsed.setChecked(savedInstanceState.getBoolean(ARG_USE_REPEAT));
            mIsActive.setChecked(savedInstanceState.getBoolean(ARG_IS_ACTIVE));

            mEndTime.setChecked(savedInstanceState.getBoolean(ARG_USE_END_TIME));
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

        Calendar nextMinute = Calendar.getInstance();
        nextMinute.add(Calendar.MINUTE, 1);

        if (time != null) {
            cal = time.getCalendar();
        } else {
            cal = nextMinute;
        }

        mIsActive.setChecked(time == null || time.isActive());

        mCurrentYear = cal.get(Calendar.YEAR);
        mCurrentMonth = cal.get(Calendar.MONTH);
        mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

        mIsTimeUsed.setChecked(time != null && time.hasTime());

        /* If date/time is set but there's no time part, use next minute for the time as default. */
        if (time != null && !time.hasTime()) {
            mCurrentHour = nextMinute.get(Calendar.HOUR_OF_DAY);
            mCurrentMinute = nextMinute.get(Calendar.MINUTE);
        } else {
            mCurrentHour = cal.get(Calendar.HOUR_OF_DAY);
            mCurrentMinute = cal.get(Calendar.MINUTE);
        }

        mEndTime.setChecked(false);
        mCurrentEndTimeHour = cal.get(Calendar.HOUR_OF_DAY);
        mCurrentEndTimeMinute = cal.get(Calendar.MINUTE);

        if (time != null) {
            mIsRepeaterUsed.setChecked(time.hasRepeater());

            if (time.hasRepeater()) {
                mRepeaterPicker.setText(time.getRepeater().toString());
            }
        }
    }

    private void setupDateShortcutsButtons(View view) {
        /* Button - today. */
        view.findViewById(R.id.dialog_timestamp_today_shortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
            }
        });

        /* Button - tomorrow. */
        view.findViewById(R.id.dialog_timestamp_tomorrow_shortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
            }
        });

        /* Button - next week. */
        view.findViewById(R.id.dialog_timestamp_next_week_shortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.add(Calendar.DATE, 7);

                mCurrentYear = cal.get(Calendar.YEAR);
                mCurrentMonth = cal.get(Calendar.MONTH);
                mCurrentDay = cal.get(Calendar.DAY_OF_MONTH);

                setViewsFromCurrentValues();
            }
        });
    }

    private void setupPickerButtons() {
        mDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDatePickerDialog = new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mCurrentYear = year;
                        mCurrentMonth = monthOfYear;
                        mCurrentDay = dayOfMonth;

                        setViewsFromCurrentValues();
                    }
                }, mCurrentYear, mCurrentMonth, mCurrentDay);

                mDatePickerDialog.show();
            }
        });

        mTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTimePickerDialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mCurrentHour = hourOfDay;
                        mCurrentMinute = minute;

                        mIsTimeUsed.setChecked(true);

                        setViewsFromCurrentValues();
                    }
                }, mCurrentHour, mCurrentMinute, true);

                mTimePickerDialog.show();
            }
        });

        mRepeaterPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRepeaterPickerDialog = new RepeaterPickerDialog(mContext, new RepeaterPickerDialog.OnRepeaterSetListener() {
                    @Override
                    public void onRepeaterSet(OrgRepeater repeater) {
                        mRepeaterPicker.setText(repeater.toString());
                        mIsRepeaterUsed.setChecked(true);

                        setViewsFromCurrentValues();

                    }
                }, mRepeaterPicker.getText().toString());

                mRepeaterPickerDialog.show();

            }
        });


        mEndTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEndTimePickerDialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mCurrentEndTimeHour = hourOfDay;
                        mCurrentEndTimeMinute = minute;

                        mEndTime.setChecked(true);

                        setViewsFromCurrentValues();
                    }
                }, mCurrentEndTimeHour, mCurrentEndTimeMinute, true);

                mEndTimePickerDialog.show();
            }
        });
    }

    /**
     * NOTE: These callbacks are called not only on user press, but during initialization as well.
     * Because of that, it's important that this method is called after dialog has been created.
     */
    private void setupToggleButtons() {
        mIsActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setViewsFromCurrentValues();
            }
        });

        mIsTimeUsed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setViewsFromCurrentValues();
            }
        });

        mEndTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setViewsFromCurrentValues();
            }
        });

        mIsRepeaterUsed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setViewsFromCurrentValues();
            }
        });
    }

    private OrgDateTime getCurrentOrgTime() {
        OrgDateTime.Builder builder = new OrgDateTime.Builder()
                .setIsActive(mIsActive.isChecked())

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

        return builder.build();
//
//
//        return OrgDateTime.getInstance(
//                mIsActive.isChecked(),
//                mIsTimeUsed.isChecked(),
//                mCurrentYear,
//                mCurrentMonth,
//                mCurrentDay,
//                mCurrentHour,
//                mCurrentMinute);
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, dialog);
        super.onDismiss(dialog);
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
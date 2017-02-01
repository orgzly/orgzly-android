package com.orgzly.android.util;

import android.content.Context;
import android.text.format.DateFormat;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

/**
 * Formats time to be displayed to user.
 */
public class OrgTimeUserFormatter {
    private Context mContext;
//    private StringBuilder mStrBuilder;
//    private Formatter mFormatter;

    private java.text.DateFormat mDateFormat;
    private java.text.DateFormat mTimeFormat;

    public OrgTimeUserFormatter(Context context) {
        mContext = context;
//        mStrBuilder = new StringBuilder(50);
//        mFormatter = new Formatter(mStrBuilder, Locale.getDefault());

        mDateFormat = DateFormat.getDateFormat(mContext);
        mTimeFormat = DateFormat.getTimeFormat(mContext);
    }

    public String format(OrgRange time) {
        /* Format without brackets for now. */
        if (time.getEndTime() != null) {
            return time.getStartTime().toStringWithoutBrackets() + " — " + time.getEndTime().toStringWithoutBrackets();
        } else {
            return time.getStartTime().toStringWithoutBrackets();
        }
    }

    public String format(OrgDateTime time) {
        return time.toStringWithoutBrackets();
    }

    public String formatDate(OrgDateTime time) {
        return mDateFormat.format(time.getCalendar().getTime());

//        long timestamp = time.getCalendar().getTimeInMillis();
//
//        int flags = DateUtils.FORMAT_SHOW_DATE |
//                DateUtils.FORMAT_ABBREV_MONTH |
//                DateUtils.FORMAT_SHOW_WEEKDAY |
//                DateUtils.FORMAT_ABBREV_WEEKDAY;
//
//        mStrBuilder.setLength(0);
//        return DateUtils.formatDateRange(mContext, mFormatter, timestamp, timestamp, flags).toString();
    }

    public String formatTime(OrgDateTime time) {
        return mTimeFormat.format(time.getCalendar().getTime());

//        long timestamp = time.getCalendar().getTimeInMillis();
//
//        int flags = DateUtils.FORMAT_SHOW_TIME;
//
//        mStrBuilder.setLength(0);
//        return DateUtils.formatDateRange(mContext, mFormatter, timestamp, timestamp, flags).toString();
    }

    public String formatRepeater(OrgDateTime time) {
        return time.getRepeater().toString();
    }
}

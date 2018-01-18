package com.orgzly.android.util;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import org.joda.time.DateTime;

import java.util.Formatter;
import java.util.Locale;

/**
 * Formats time to be displayed to user.
 */
public class UserTimeFormatter {
    private Context mContext;

    /* Reuse Formatter, for speed.
     */
    private StringBuilder formatterString;
    private Formatter formatter;


    public UserTimeFormatter(Context context) {
        mContext = context;
    }

    public CharSequence formatAll(OrgRange time) {
        SpannableStringBuilder s = new SpannableStringBuilder();

        s.append(formatAll(time.getStartTime()));

        if (time.getEndTime() != null) {
            s.append(" — ");
            s.append(formatAll(time.getEndTime()));
        }

        return s;
    }

    public String formatAll(OrgDateTime time) {
        StringBuilder s = new StringBuilder();

        s.append(formatDate(time));

        if (time.hasTime()) {
            s.append(" ");

            if (time.hasEndTime()) {
                s.append(formatTimeAndEndTime(time));
            } else {
                s.append(formatTime(time));
            }
        }

        if (time.hasRepeater()) {
            s.append(" ");
            s.append(formatRepeater(time));
        }

        if (time.hasDelay()) {
            s.append(" ");
            s.append(formatDelay(time));
        }

        return s.toString();
    }

    public String formatDate(DateTime datetime) {
        return formatDate(datetime.getMillis());
    }

    public String formatDate(OrgDateTime time) {
        return formatDate(time.getCalendar().getTimeInMillis());
    }

    private String formatDate(long timestamp) {
        int flags = DateUtils.FORMAT_SHOW_DATE |
                    DateUtils.FORMAT_ABBREV_MONTH |
                    DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_ABBREV_WEEKDAY;

        return format(timestamp, timestamp, flags);
    }

    public String formatTime(OrgDateTime time) {
        long timestamp = time.getCalendar().getTimeInMillis();

        int flags = DateUtils.FORMAT_SHOW_TIME;

        return format(timestamp, timestamp, flags);
    }

    public String formatTimeAndEndTime(OrgDateTime time) {
        long t1 = time.getCalendar().getTimeInMillis();
        long t2 = time.getEndCalendar().getTimeInMillis();

        int flags = DateUtils.FORMAT_SHOW_TIME;

        return format(t1, t2, flags);
    }

    public String formatRepeater(OrgDateTime time) {
        return time.getRepeater().toString();
    }

    public String formatDelay(OrgDateTime time) {
        return time.getDelay().toString();
    }

    /**
     * Reuse formatter, for speed. See comment in
     * {@link DateUtils#formatDateRange(Context, long, long, int)}.
     */
    private String format(long startMillis, long endMillis, int flags) {
        if (formatter == null) {
            formatterString = new StringBuilder(50);
            formatter = new Formatter(formatterString, Locale.getDefault());

        } else {
            formatterString.setLength(0);
        }

        return DateUtils.formatDateRange(mContext, formatter, startMillis, endMillis, flags).toString();
    }
}

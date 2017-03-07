package com.orgzly.android.util;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

/**
 * Formats time to be displayed to user.
 */
public class OrgTimeUserFormatter {
    private Context mContext;

    public OrgTimeUserFormatter(Context context) {
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
            s.append(formatTime(time));

            if (time.hasEndTime()) {
                s.append("–");
                s.append(formatEndTime(time));
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

    public String formatDate(OrgDateTime time) {
        long timestamp = time.getCalendar().getTimeInMillis();

        int flags = DateUtils.FORMAT_SHOW_DATE |
                    DateUtils.FORMAT_ABBREV_MONTH |
                    DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_ABBREV_WEEKDAY;

        return DateUtils.formatDateTime(mContext, timestamp, flags);
    }

    public String formatTime(OrgDateTime time) {
        long timestamp = time.getCalendar().getTimeInMillis();

        int flags = DateUtils.FORMAT_SHOW_TIME;

        return DateUtils.formatDateTime(mContext, timestamp, flags);
    }

    public String formatEndTime(OrgDateTime time) {
        long timestamp = time.getEndCalendar().getTimeInMillis();

        int flags = DateUtils.FORMAT_SHOW_TIME;

        return DateUtils.formatDateTime(mContext, timestamp, flags);
    }

    public String formatRepeater(OrgDateTime time) {
        return time.getRepeater().toString();
    }

    public String formatDelay(OrgDateTime time) {
        return time.getDelay().toString();
    }
}

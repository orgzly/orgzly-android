package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import com.orgzly.android.OrgzlyTest;

import java.util.Arrays;

public class OrgFormatterTest extends OrgzlyTest {

    protected class SpanItem {
        Object span;
        int start;
        int end;
        String url;
    }

    protected class ParseResult {
        final String outputString;
        final SpanItem[] foundSpans;

        public ParseResult(String str) {
            SpannableStringBuilder ssb = OrgFormatter.parse(str, context);

            outputString = ssb.toString();

            Object[] allSpans = ssb.getSpans(0, ssb.length(), Object.class);

            // LogUtils.d("WIP", "Found " + allSpans.length + " spans in: " + str + " parsed to: " + outputString);

            foundSpans = new SpanItem[allSpans.length];

            for (int i = 0; i < allSpans.length; i++) {
                Object span = allSpans[i];
                SpanItem spanItem = new SpanItem();

                spanItem.span = span;
                spanItem.start = ssb.getSpanStart(span);
                spanItem.end = ssb.getSpanEnd(span);

                if (span instanceof URLSpan) {
                    spanItem.url = ((URLSpan) span).getURL();
                }

                foundSpans[i] = spanItem;
            }

            // Sort spans in the order they appear
            Arrays.sort(foundSpans, (o1, o2) -> o1.start - o2.start);
        }
    }
}
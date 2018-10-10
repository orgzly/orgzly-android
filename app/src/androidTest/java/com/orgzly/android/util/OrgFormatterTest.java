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

    protected class OrgSpannable {
        final String string;
        final SpanItem[] spans;

        public OrgSpannable(String str) {
            SpannableStringBuilder ssb = OrgFormatter.parse(str, context);

            string = ssb.toString();


            Object[] allSpans = ssb.getSpans(0, ssb.length() - 1, Object.class);

            spans = new SpanItem[allSpans.length];

            for (int i = 0; i < allSpans.length; i++) {
                Object span = allSpans[i];
                SpanItem spanItem = new SpanItem();

                spanItem.span = span;
                spanItem.start = ssb.getSpanStart(span);
                spanItem.end = ssb.getSpanEnd(span);

                if (span instanceof URLSpan) {
                    spanItem.url = ((URLSpan) span).getURL();
                }

                spans[i] = spanItem;
            }

            // Sort spans in the order they appear
            Arrays.sort(spans, (o1, o2) -> o1.start - o2.start);
        }
    }
}
package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class OrgFormatterTest {
    @Test
    public void testLinksMultiLine() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com]]\n" +
                        "[[http://www.orgzly.com]]");

        assertEquals("http://www.orgzly.com\n" +
                "http://www.orgzly.com", spannable.string);

        assertEquals(2, spannable.spans.length);

        assertEquals(0, spannable.spans[0].start);
        assertEquals(21, spannable.spans[0].end);
        assertEquals("URLSpan", spannable.spans[0].className);
        assertEquals("http://www.orgzly.com", spannable.spans[0].url);

        assertEquals(22, spannable.spans[1].start);
        assertEquals(43, spannable.spans[1].end);
        assertEquals("URLSpan", spannable.spans[1].className);
        assertEquals("http://www.orgzly.com", spannable.spans[1].url);
    }

    @Test
    public void testLinksNamed() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com][Orgzly]]");

        assertEquals("Orgzly", spannable.string);

        assertEquals(1, spannable.spans.length);

        assertEquals(0, spannable.spans[0].start);
        assertEquals(6, spannable.spans[0].end);
        assertEquals("URLSpan", spannable.spans[0].className);
        assertEquals("http://www.orgzly.com", spannable.spans[0].url);
    }

    @Test
    public void testAllLinkTypes() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "AAA http://www.x.com BBB [[http://www.y.com]]CCC [[http://www.z.com][Z]]DDD");

        assertEquals("AAA http://www.x.com BBB http://www.y.comCCC ZDDD", spannable.string);

        assertEquals(3, spannable.spans.length);

        // Different values on different devices. Refactor OrgFormatter.
        for (int i: Arrays.asList(0, 2)) {
            assertThat(spannable.spans[i].start, anyOf(is(4), is(45)));
            assertThat(spannable.spans[i].end, anyOf(is(20), is(46)));
            assertThat(spannable.spans[i].className, is("URLSpan"));
            assertThat(spannable.spans[i].url, anyOf(is("http://www.x.com"), is("http://www.z.com")));
        }

        assertEquals(25, spannable.spans[1].start);
        assertEquals(41, spannable.spans[1].end);
        assertEquals("URLSpan", spannable.spans[1].className);
        assertEquals("http://www.y.com", spannable.spans[1].url);
    }

    @Test
    public void testMailto() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "AAA mailto:x@x.com BBB [[mailto:y@y.com]]CCC [[mailto:z@z.com][Z]]DDD");

        assertEquals("AAA mailto:x@x.com BBB mailto:y@y.comCCC ZDDD", spannable.string);

        assertEquals(3, spannable.spans.length);

        // Different values on different devices. Refactor OrgFormatter.
        for (int i: Arrays.asList(0, 2)) {
            assertThat(spannable.spans[i].start, anyOf(is(4), is(41)));
            assertThat(spannable.spans[i].end, anyOf(is(18), is(42)));
            assertThat(spannable.spans[i].className, is("URLSpan"));
            assertThat(spannable.spans[i].url, anyOf(is("mailto:x@x.com"), is("mailto:z@z.com")));
        }

        assertEquals(23, spannable.spans[1].start);
        assertEquals(37, spannable.spans[1].end);
        assertEquals("URLSpan", spannable.spans[1].className);
        assertEquals("mailto:y@y.com", spannable.spans[1].url);
    }

    private class OrgSpan {
        int start;
        int end;
        String className;
        String url;
    }

    private class OrgSpannable {
        String string;
        OrgSpan[] spans;

        public OrgSpannable(String str) {
            SpannableStringBuilder ssb = OrgFormatter.INSTANCE.parse(null, str);

            string = ssb.toString();

            Object[] allSpans = ssb.getSpans(0, ssb.length() - 1, Object.class);

            spans = new OrgSpan[allSpans.length];

            for (int i = 0; i < allSpans.length; i++) {
                spans[i] = new OrgSpan();

                spans[i].start = ssb.getSpanStart(allSpans[i]);
                spans[i].end = ssb.getSpanEnd(allSpans[i]);
                spans[i].className = allSpans[i].getClass().getSimpleName();

                if (allSpans[i] instanceof URLSpan) {
                    spans[i].url = ((URLSpan)allSpans[i]).getURL();
                }
            }
        }
    }
}
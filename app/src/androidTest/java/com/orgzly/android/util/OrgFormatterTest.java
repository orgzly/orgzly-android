package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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

        assertEquals(4, spannable.spans[2].start);
        assertEquals(20, spannable.spans[2].end);
        assertEquals("URLSpan", spannable.spans[2].className);
        assertEquals("http://www.x.com", spannable.spans[2].url);

        assertEquals(25, spannable.spans[1].start);
        assertEquals(41, spannable.spans[1].end);
        assertEquals("URLSpan", spannable.spans[1].className);
        assertEquals("http://www.y.com", spannable.spans[1].url);

        assertEquals(45, spannable.spans[0].start);
        assertEquals(46, spannable.spans[0].end);
        assertEquals("URLSpan", spannable.spans[0].className);
        assertEquals("http://www.z.com", spannable.spans[0].url);
    }

    @Test
    public void testMailto() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "AAA mailto:x@x.com BBB [[mailto:y@y.com]]CCC [[mailto:z@z.com][Z]]DDD");

        assertEquals("AAA mailto:x@x.com BBB mailto:y@y.comCCC ZDDD", spannable.string);

        assertEquals(3, spannable.spans.length);

        assertEquals(4, spannable.spans[2].start);
        assertEquals(18, spannable.spans[2].end);
        assertEquals("URLSpan", spannable.spans[2].className);
        assertEquals("mailto:x@x.com", spannable.spans[2].url);

        assertEquals(23, spannable.spans[1].start);
        assertEquals(37, spannable.spans[1].end);
        assertEquals("URLSpan", spannable.spans[1].className);
        assertEquals("mailto:y@y.com", spannable.spans[1].url);

        assertEquals(41, spannable.spans[0].start);
        assertEquals(42, spannable.spans[0].end);
        assertEquals("URLSpan", spannable.spans[0].className);
        assertEquals("mailto:z@z.com", spannable.spans[0].url);
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
            SpannableStringBuilder ssb = OrgFormatter.parse(str);

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
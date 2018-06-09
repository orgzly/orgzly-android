package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OrgFormatterTest extends OrgzlyTest {
    @Test
    public void testLinksMultiLine() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com]]\n" +
                "[[http://www.orgzly.com]]");

        assertThat(spannable.string, is("http://www.orgzly.com\nhttp://www.orgzly.com"));

        assertThat(spannable.spans.length, is(2));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(21));
        assertThat(spannable.spans[0].className, is("URLSpan"));
        assertThat(spannable.spans[0].url, is("http://www.orgzly.com"));

        assertThat(spannable.spans[1].start, is(22));
        assertThat(spannable.spans[1].end, is(43));
        assertThat(spannable.spans[1].className, is("URLSpan"));
        assertThat(spannable.spans[1].url, is("http://www.orgzly.com"));
    }

    @Test
    public void testLinksNamed() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com][Orgzly]]");

        assertThat(spannable.string, is("Orgzly"));

        assertThat(spannable.spans.length, is(1));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(6));
        assertThat(spannable.spans[0].className, is("URLSpan"));
        assertThat(spannable.spans[0].url, is("http://www.orgzly.com"));
    }

    @Test
    public void testAllLinkTypes() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "AAA http://www.x.com BBB [[http://www.y.com]]CCC [[http://www.z.com][Z]]DDD");

        assertThat(spannable.string, is("AAA http://www.x.com BBB http://www.y.comCCC ZDDD"));

        assertThat(spannable.spans.length, is(3));

        assertThat(spannable.spans[0].start, is(4));
        assertThat(spannable.spans[0].end, is(20));
        assertThat(spannable.spans[0].className, is("URLSpan"));
        assertThat(spannable.spans[0].url, is("http://www.x.com"));

        assertThat(spannable.spans[1].start, is(25));
        assertThat(spannable.spans[1].end, is(41));
        assertThat(spannable.spans[1].className, is("URLSpan"));
        assertThat(spannable.spans[1].url, is("http://www.y.com"));

        assertThat(spannable.spans[2].start, is(45));
        assertThat(spannable.spans[2].end, is(46));
        assertThat(spannable.spans[2].className, is("URLSpan"));
        assertThat(spannable.spans[2].url, is("http://www.z.com"));
    }

    @Test
    public void testMailto() throws Exception {
        OrgSpannable spannable = new OrgSpannable(
                "AAA mailto:x@x.com BBB [[mailto:y@y.com]]CCC [[mailto:z@z.com][Z]]DDD");

        assertThat(spannable.string, is("AAA mailto:x@x.com BBB mailto:y@y.comCCC ZDDD"));

        assertThat(spannable.spans.length, is(3));

        assertThat(spannable.spans[0].start, is(4));
        assertThat(spannable.spans[0].end, is(18));
        assertThat(spannable.spans[0].className, is("URLSpan"));
        assertThat(spannable.spans[0].url, is("mailto:x@x.com"));

        assertThat(spannable.spans[1].start, is(23));
        assertThat(spannable.spans[1].end, is(37));
        assertThat(spannable.spans[1].className, is("URLSpan"));
        assertThat(spannable.spans[1].url, is("mailto:y@y.com"));

        assertThat(spannable.spans[2].start, is(41));
        assertThat(spannable.spans[2].end, is(42));
        assertThat(spannable.spans[2].className, is("URLSpan"));
        assertThat(spannable.spans[2].url, is("mailto:z@z.com"));
    }

    @Test
    public void testTwoBoldNextToEachOtherWithMarks() {
        AppPreferences.styledTextWithMarks(context, true);

        OrgSpannable spannable = new OrgSpannable("*a* *b*");

        assertThat(spannable.string, is("*a* *b*"));

        assertThat(spannable.spans.length, is(2));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(3));

        assertThat(spannable.spans[1].start, is(4));
        assertThat(spannable.spans[1].end, is(7));
    }

    @Test
    public void testBoldItalic() {
        OrgSpannable spannable = new OrgSpannable("*_a_*");

        assertThat(spannable.string, is("a"));

        assertThat(spannable.spans.length, is(2));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(1));

        assertThat(spannable.spans[1].start, is(0));
        assertThat(spannable.spans[1].end, is(1));
    }

    @Test
    public void testBoldItalicWithMarks() {
        AppPreferences.styledTextWithMarks(context, true);

        OrgSpannable spannable = new OrgSpannable("*_a_*");

        assertThat(spannable.string, is("*_a_*"));

        assertThat(spannable.spans.length, is(2));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(5));

        assertThat(spannable.spans[1].start, is(0));
        assertThat(spannable.spans[1].end, is(5));
    }

    @Test
    public void testMarkupWithTrailingCharacters() {
        OrgSpannable spannable = new OrgSpannable("*a* b");
        assertThat(spannable.string, is("a b"));
    }

    @Test
    public void testIdLink() {
        OrgSpannable spannable = new OrgSpannable(
                "[[id:AA4E5D54-CB34-492E-967B-3657B26143E7][Vivamus at arcu velit]]\n" +
                "\n" +
                "Sed in fermentum diam\n" +
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit");

        assertThat(spannable.string, is(
                "Vivamus at arcu velit\n" +
                "\n" +
                "Sed in fermentum diam\n" +
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit"));

        assertThat(spannable.spans.length, is(1));
        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(21));
    }

    @Test
    public void testPlainLinkWithTrailingSlash() {
        OrgSpannable spannable = new OrgSpannable("http://orgzly.com/");
        assertThat(spannable.string, is("http://orgzly.com/"));
    }

    private class SpanItem {
        int start;
        int end;
        String className;
        String url;
    }

    private class OrgSpannable {
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

                spanItem.start = ssb.getSpanStart(span);
                spanItem.end = ssb.getSpanEnd(span);
                spanItem.className = span.getClass().getSimpleName();

                if (span instanceof URLSpan) {
                    spanItem.url = ((URLSpan)span).getURL();
                }

                spans[i] = spanItem;
            }

            // Sort spans in the order they appear
            Arrays.sort(spans, (o1, o2) -> o1.start - o2.start);
        }
    }
}
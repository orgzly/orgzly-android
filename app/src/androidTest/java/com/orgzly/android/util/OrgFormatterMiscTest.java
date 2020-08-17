package com.orgzly.android.util;

import android.text.style.URLSpan;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class OrgFormatterMiscTest extends OrgFormatterTest {
    @Test
    public void testLinksMultiLine() {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com]]\n" +
                "[[http://www.orgzly.com]]");

        assertThat(spannable.string, is("http://www.orgzly.com\nhttp://www.orgzly.com"));

        assertThat(spannable.spans.length, is(2));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(21));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[0].url, is("http://www.orgzly.com"));

        assertThat(spannable.spans[1].start, is(22));
        assertThat(spannable.spans[1].end, is(43));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[1].url, is("http://www.orgzly.com"));
    }

    @Test
    public void testLinksNamed() {
        OrgSpannable spannable = new OrgSpannable(
                "[[http://www.orgzly.com][Orgzly]]");

        assertThat(spannable.string, is("Orgzly"));

        assertThat(spannable.spans.length, is(1));

        assertThat(spannable.spans[0].start, is(0));
        assertThat(spannable.spans[0].end, is(6));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[0].url, is("http://www.orgzly.com"));
    }

    @Test
    public void testAllLinkTypes() {
        OrgSpannable spannable = new OrgSpannable(
                "AAA http://www.x.com BBB [[http://www.y.com]]CCC [[http://www.z.com][Z]]DDD");

        assertThat(spannable.string, is("AAA http://www.x.com BBB http://www.y.comCCC ZDDD"));

        assertThat(spannable.spans.length, is(3));

        assertThat(spannable.spans[0].start, is(4));
        assertThat(spannable.spans[0].end, is(20));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[0].url, is("http://www.x.com"));

        assertThat(spannable.spans[1].start, is(25));
        assertThat(spannable.spans[1].end, is(41));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[1].url, is("http://www.y.com"));

        assertThat(spannable.spans[2].start, is(45));
        assertThat(spannable.spans[2].end, is(46));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[2].url, is("http://www.z.com"));
    }

    @Test
    public void testMailto() {
        OrgSpannable spannable = new OrgSpannable(
                "AAA mailto:x@x.com BBB [[mailto:y@y.com]]CCC [[mailto:z@z.com][Z]]DDD");

        assertThat(spannable.string, is("AAA mailto:x@x.com BBB mailto:y@y.comCCC ZDDD"));

        assertThat(spannable.spans.length, is(3));

        assertThat(spannable.spans[0].start, is(4));
        assertThat(spannable.spans[0].end, is(18));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[0].url, is("mailto:x@x.com"));

        assertThat(spannable.spans[1].start, is(23));
        assertThat(spannable.spans[1].end, is(37));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[1].url, is("mailto:y@y.com"));

        assertThat(spannable.spans[2].start, is(41));
        assertThat(spannable.spans[2].end, is(42));
        assertThat(spannable.spans[0].span, instanceOf(URLSpan.class));
        assertThat(spannable.spans[2].url, is("mailto:z@z.com"));
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

    @Ignore
    @Test
    public void testPlainLinkPrefixed() {
        OrgSpannable spannable = new OrgSpannable("strhttp://orgzly.com");
        assertThat(spannable.string, is("strhttp://orgzly.com"));
        assertThat(spannable.spans.length, is(0));
    }
}
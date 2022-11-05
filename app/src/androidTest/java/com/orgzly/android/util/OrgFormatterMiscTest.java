package com.orgzly.android.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.orgzly.android.ui.views.style.UrlLinkSpan;

import org.junit.Test;

public class OrgFormatterMiscTest extends OrgFormatterTest {
    @Test
    public void testLinksMultiLine() {
        ParseResult spannable = new ParseResult(
                "[[https://www.orgzly.com]]\n" +
                "[[https://www.orgzly.com]]");

        assertThat(spannable.outputString, is("https://www.orgzly.com\nhttps://www.orgzly.com"));

        assertThat(spannable.foundSpans.length, is(2));

        assertThat(spannable.foundSpans[0].start, is(0));
        assertThat(spannable.foundSpans[0].end, is(22));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[0].url, is("https://www.orgzly.com"));

        assertThat(spannable.foundSpans[1].start, is(23));
        assertThat(spannable.foundSpans[1].end, is(45));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[1].url, is("https://www.orgzly.com"));
    }

    @Test
    public void testLinksNamed() {
        ParseResult spannable = new ParseResult(
                "[[https://www.orgzly.com][Orgzly]]");

        assertThat(spannable.outputString, is("Orgzly"));

        assertThat(spannable.foundSpans.length, is(1));

        assertThat(spannable.foundSpans[0].start, is(0));
        assertThat(spannable.foundSpans[0].end, is(6));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[0].url, is("https://www.orgzly.com"));
    }

    @Test
    public void testAllLinkTypes() {
        ParseResult spannable = new ParseResult(
                "AAA http://www.x.com BBB [[http://www.y.com]]CCC [[http://www.z.com][Z]]DDD");

        assertThat(spannable.outputString, is("AAA http://www.x.com BBB http://www.y.comCCC ZDDD"));

        assertThat(spannable.foundSpans.length, is(3));

        assertThat(spannable.foundSpans[0].start, is(4));
        assertThat(spannable.foundSpans[0].end, is(20));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[0].url, is("http://www.x.com"));

        assertThat(spannable.foundSpans[1].start, is(25));
        assertThat(spannable.foundSpans[1].end, is(41));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[1].url, is("http://www.y.com"));

        assertThat(spannable.foundSpans[2].start, is(45));
        assertThat(spannable.foundSpans[2].end, is(46));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[2].url, is("http://www.z.com"));
    }

    @Test
    public void testMailto() {
        ParseResult spannable = new ParseResult(
                "AAA mailto:x@x.com BBB [[mailto:y@y.com]]CCC [[mailto:z@z.com][Z]]DDD");

        assertThat(spannable.outputString, is("AAA mailto:x@x.com BBB mailto:y@y.comCCC ZDDD"));

        assertThat(spannable.foundSpans.length, is(3));

        assertThat(spannable.foundSpans[0].start, is(4));
        assertThat(spannable.foundSpans[0].end, is(18));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[0].url, is("mailto:x@x.com"));

        assertThat(spannable.foundSpans[1].start, is(23));
        assertThat(spannable.foundSpans[1].end, is(37));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[1].url, is("mailto:y@y.com"));

        assertThat(spannable.foundSpans[2].start, is(41));
        assertThat(spannable.foundSpans[2].end, is(42));
        assertThat(spannable.foundSpans[0].span, instanceOf(UrlLinkSpan.class));
        assertThat(spannable.foundSpans[2].url, is("mailto:z@z.com"));
    }

    @Test
    public void testPlainLinkWithTrailingSlash() {
        ParseResult spannable = new ParseResult("https://www.orgzly.com/");
        assertThat(spannable.outputString, is("https://www.orgzly.com/"));
    }

}
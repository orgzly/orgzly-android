package com.orgzly.android.util;

import com.orgzly.android.prefs.AppPreferences;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OrgFormatterStyleTextTest extends OrgFormatterTest {
    @Test
    public void testTwoBoldNextToEachOtherWithMarks() {
        AppPreferences.styledTextWithMarks(context, true);

        ParseResult spannable = new ParseResult("*a* *b*");

        assertThat(spannable.outputString, is("*a* *b*"));

        assertThat(spannable.foundSpans.length, is(2));

        assertThat(spannable.foundSpans[0].start, is(0));
        assertThat(spannable.foundSpans[0].end, is(3));

        assertThat(spannable.foundSpans[1].start, is(4));
        assertThat(spannable.foundSpans[1].end, is(7));
    }

    @Test
    public void testBoldItalic() {
        ParseResult spannable = new ParseResult("*_a_*");

        assertThat(spannable.outputString, is("a"));

        assertThat(spannable.foundSpans.length, is(2));

        assertThat(spannable.foundSpans[0].start, is(0));
        assertThat(spannable.foundSpans[0].end, is(1));

        assertThat(spannable.foundSpans[1].start, is(0));
        assertThat(spannable.foundSpans[1].end, is(1));
    }

    @Test
    public void testBoldItalicWithMarks() {
        AppPreferences.styledTextWithMarks(context, true);

        ParseResult spannable = new ParseResult("*_a_*");

        assertThat(spannable.outputString, is("*_a_*"));

        assertThat(spannable.foundSpans.length, is(2));

        assertThat(spannable.foundSpans[0].start, is(0));
        assertThat(spannable.foundSpans[0].end, is(5));

        assertThat(spannable.foundSpans[1].start, is(0));
        assertThat(spannable.foundSpans[1].end, is(5));
    }

    @Test
    public void testMarkupWithTrailingCharacters() {
        ParseResult spannable = new ParseResult("*a* b");
        assertThat(spannable.outputString, is("a b"));
    }
}
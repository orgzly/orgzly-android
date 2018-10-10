package com.orgzly.android.util;

import com.orgzly.android.prefs.AppPreferences;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OrgFormatterStyleTextTest extends OrgFormatterTest {
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
}
package com.orgzly.android.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.View;

import com.orgzly.android.Shelf;
import com.orgzly.android.prefs.AppPreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class OrgFormatter {
    private static final String LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto";

    // tel:1234567
    private static final String PLAIN_LINK = "((" + LINK_SCHEMES + "):\\S+)";

    /* Same as the above, but ] ends the link too. Used for bracket links. */
    private static final String BRACKET_LINK = "((" + LINK_SCHEMES + "):[^]\\s]+)";

    // #custom id
    private static final String CUSTOM_ID_LINK = "(#([^]]+))";

    /* Allows anything as a link. Probably needs some constraints.
     * See http://orgmode.org/manual/External-links.html and org-any-link-re
     */
    private static final String BRACKET_ANY_LINK = "(([^]]+))";

    public static SpannableStringBuilder parse(Context context, String s) {
        return parse(context, s, true);
    }

    public static SpannableStringBuilder parse(Context context, String s, boolean linkify) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(s);

        doCustomIdLinkWithName(ssb, CUSTOM_ID_LINK, linkify, context);
        doCustomIdLink(ssb, CUSTOM_ID_LINK, linkify, context);

        doOrgLinksWithName(ssb, BRACKET_LINK, linkify);
        doOrgLinksWithName(ssb, BRACKET_ANY_LINK, false);

        doOrgLinks(ssb, BRACKET_LINK, linkify);

        doPlainLinks(ssb, PLAIN_LINK, linkify);

        doMarkup(ssb, context);

        return ssb;
    }

    /**
     * [[http://link.com][link]]
     */
    private static void doOrgLinksWithName(SpannableStringBuilder ssb, String linkRegex, boolean createLinks) {
        Pattern p = Pattern.compile("\\[\\[" + linkRegex + "\\]\\[([^]]+)\\]\\]");
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String link = m.group(1);
            String name = m.group(3);

            ssb.replace(m.start(), m.end(), name);

            if (createLinks) {
                setUrlSpan(ssb, link, m.start(), m.start() + name.length());
            }

            /* Re-create Matcher, as ssb size is modified. */
            m = p.matcher(ssb);

        }
    }

    /**
     * [[http://link.com]]
     */
    private static void doOrgLinks(SpannableStringBuilder ssb, String linkRegex, boolean createLinks) {
        Pattern p = Pattern.compile("\\[\\[" + linkRegex + "\\]\\]");
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String link = m.group(1);

            ssb.replace(m.start(), m.end(), link);

            if (createLinks) {
                setUrlSpan(ssb, link, m.start(), m.start() + link.length());
            }

            /* Re-create Matcher, as ssb size is modified. */
            m = p.matcher(ssb);
        }
    }

    /**
     * [[#custom id][link]]
     */
    private static void doCustomIdLinkWithName(SpannableStringBuilder ssb, String linkRegex, boolean createLinks, Context context) {
        Pattern p = Pattern.compile("\\[\\[" + linkRegex + "\\]\\[([^]]+)\\]\\]");
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String customId = m.group(2);
            String name = m.group(3);

            ssb.replace(m.start(), m.end(), name);

            if (createLinks) {
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Shelf shelf = new Shelf(context);
                        shelf.openFirstNoteWithProperty("CUSTOM_ID", customId);
                    }
                }, m.start(), m.start() + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            /* Re-create Matcher, as ssb size is modified. */
            m = p.matcher(ssb);

        }
    }

    private static void doCustomIdLink(SpannableStringBuilder ssb, String linkRegex, boolean createLinks, Context context) {
        Pattern p = Pattern.compile("\\[\\[" + linkRegex + "\\]\\]");
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String link = m.group(1);
            String customId = m.group(2);

            ssb.replace(m.start(), m.end(), link);

            if (createLinks) {
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Shelf shelf = new Shelf(context);
                        shelf.openFirstNoteWithProperty("CUSTOM_ID", customId);
                    }
                }, m.start(), m.start() + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            /* Re-create Matcher, as ssb size is modified. */
            m = p.matcher(ssb);
        }
    }

    /**
     * http://link.com
     */
    private static void doPlainLinks(SpannableStringBuilder ssb, String linkRegex, boolean createLinks) {
        if (!createLinks) {
            return;
        }

        Pattern p = Pattern.compile(linkRegex);
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String link = m.group(1);

            /* Only if the first character has no URLSpan. */
            if (ssb.getSpans(m.start(), m.start() + 1, URLSpan.class).length == 0) {
                setUrlSpan(ssb, link, m.start(), m.end());
            }
        }
    }

    private static void setUrlSpan(SpannableStringBuilder ssb, String link, int start, int end) {
        ssb.setSpan(new URLSpan(link), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * You can make words *bold*, /italic/, _underlined_, =verbatim= , ~code~ and +strike-through+.
     */

    private static final Pattern MARKUP_PATTERN;

    private static final String PRE = "- \t('\"{";
    private static final String POST = "- \\t.,:!?;'\")}\\[";
    private static final String BORDER = "\\S";
    private static final String BODY = ".*?(?:\n.*?)?";

    private static String markupRegex(char marker) {
        return "(?:^|[" + PRE + "])([" + marker + "](" + BORDER + "|" + BORDER + BODY + BORDER + ")[" + marker + "])(?:[" + POST + "]|$)";
    }

    static {
        MARKUP_PATTERN = Pattern.compile(
                markupRegex('*') + "|" +
                markupRegex('/') + "|" +
                markupRegex('_') + "|" +
                markupRegex('=') + "|" +
                markupRegex('~') + "|" +
                markupRegex('+'), Pattern.MULTILINE);
    }

    private static void doMarkup(SpannableStringBuilder ssb, Context context) {
        boolean style = context == null || AppPreferences.styleText(context);
        boolean withMarks = context != null && AppPreferences.styledTextWithMarks(context);

        if (style) {
            doMarkup(ssb, withMarks);
        }
    }

    private static void doMarkup(SpannableStringBuilder ssb, boolean withMarks) {
        Matcher m = MARKUP_PATTERN.matcher(ssb);

        while (m.find()) {
            if (m.group(1) != null) {
                m = setMarkupSpan(ssb, m, 1, new StyleSpan(Typeface.BOLD), withMarks);

            } else if (m.group(3) != null) {
                m = setMarkupSpan(ssb, m, 3, new StyleSpan(Typeface.ITALIC), withMarks);

            } else if (m.group(5) != null) {
                m = setMarkupSpan(ssb, m, 5, new UnderlineSpan(), withMarks);

            } else if (m.group(7) != null) {
                m = setMarkupSpan(ssb, m, 7, new TypefaceSpan("monospace"), withMarks);

            } else if (m.group(9) != null) {
                m = setMarkupSpan(ssb, m, 9, new TypefaceSpan("monospace"), withMarks);

            } else if (m.group(11) != null) {
                m = setMarkupSpan(ssb, m, 11, new StrikethroughSpan(), withMarks);
            }
        }
    }

    private static Matcher setMarkupSpan(SpannableStringBuilder ssb, Matcher matcher, int group, Object span, boolean withMarks) {
        if (withMarks) {
            ssb.setSpan(span, matcher.start(group), matcher.end(group), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return matcher;
        }

        /* Next group matches content only, without markers.*/
        String content = matcher.group(group + 1);

        ssb.replace(matcher.start(group), matcher.end(group), content);

        ssb.setSpan(span, matcher.start(group), matcher.start(group)+content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        /* Re-create Matcher, as ssb size is modified. */
        return MARKUP_PATTERN.matcher(ssb);

    }
}

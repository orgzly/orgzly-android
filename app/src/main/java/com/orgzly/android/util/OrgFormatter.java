package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class OrgFormatter {
    private static final String LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto";

    private static final String PLAIN_LINK = "((" + LINK_SCHEMES + "):\\S+)";

    /* Same as the above, but ] ends the link too. */
    private static final String BRACKET_LINK = "((" + LINK_SCHEMES + "):[^]\\s]+)";

    /* Allows anything as a link. Probably needs some constraints.
     * See http://orgmode.org/manual/External-links.html and org-any-link-re
     */
    private static final String BRACKET_ANY_LINK = "(([^]]+))";

    public static SpannableStringBuilder parse(String s) {
        return parse(s, true);
    }

    public static SpannableStringBuilder parse(String s, boolean linkify) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(s);

        doOrgLinksWithName(ssb, BRACKET_LINK, linkify);
        doOrgLinksWithName(ssb, BRACKET_ANY_LINK, false);
        doOrgLinks(ssb, BRACKET_LINK, linkify);
        doPlainLinks(ssb, PLAIN_LINK, linkify);

        return ssb;
    }

    /**
     * [[http://link.com][Link]]
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

            /* Must re-create Matcher, as ssb size is modified. */
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

            /* Must re-create Matcher, as ssb size is modified. */
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
}

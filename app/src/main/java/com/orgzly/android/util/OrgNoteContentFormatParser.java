package com.orgzly.android.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class OrgNoteContentFormatParser {
    private final static String BRACKET_LINK_URL_REGEX = "https?://[^]\\s]+|mailto:[^]\\s]+";

    public static SpannableStringBuilder fromOrg(String s, boolean createLinks) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(s);

        doOrgLinksWithName(ssb, createLinks);
        doOrgLinks(ssb, createLinks);
        doPlainLinks(ssb, createLinks);

        return ssb;
    }

    /**
     * [[http://link.com][Link]]
     */
    private static void doOrgLinksWithName(SpannableStringBuilder ssb, boolean createLinks) {
        Pattern p = Pattern.compile("\\[\\[(" + BRACKET_LINK_URL_REGEX + ")\\]\\[([^]]+)\\]\\]");
        Matcher m = p.matcher(ssb);

        while (m.find()) {
            String link = m.group(1);
            String name = m.group(2);

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
    private static void doOrgLinks(SpannableStringBuilder ssb, boolean createLinks) {
        Pattern p = Pattern.compile("\\[\\[(" + BRACKET_LINK_URL_REGEX + ")\\]\\]");
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
    private static void doPlainLinks(SpannableStringBuilder ssb, boolean createLinks) {
        if (!createLinks) {
            return;
        }
        
        Pattern p = Pattern.compile("(https?://\\S+|mailto:\\S+)");
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
        ssb.setSpan(new URLSpan(link), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        // ssb.setSpan(new BackgroundColorSpan(0xFFCCCCCC), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }
}

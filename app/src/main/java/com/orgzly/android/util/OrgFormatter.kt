package com.orgzly.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.view.View
import com.orgzly.BuildConfig
import com.orgzly.android.ActionService
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.AppPreferences
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 */
object OrgFormatter {
    private val LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto"

    // tel:1234567
    private val PLAIN_LINK = "(($LINK_SCHEMES):\\S+)"

    /* Same as the above, but ] ends the link too. Used for bracket links. */
    private val BRACKET_LINK = "(($LINK_SCHEMES):[^]\\s]+)"

    // #custom id
    private val CUSTOM_ID_LINK = "(#([^]]+))"

    // id:CABA8098-5969-429E-A780-94C8E0A9D206
    private val HD = "[0-9a-fA-F]"
    private val ID_LINK = "(id:($HD{8}-(?:$HD{4}-){3}$HD{12}))"

    /* Allows anything as a link. Probably needs some constraints.
     * See http://orgmode.org/manual/External-links.html and org-any-link-re
     */
    private val BRACKET_ANY_LINK = "(([^]]+))"

    private val PRE = "- \t('\"{"
    private val POST = "- \\t.,:!?;'\")}\\["
    private val BORDER = "\\S"
    private val BODY = ".*?(?:\n.*?)?"

    // Added .{0} for the next find() to match from the beginning
    private fun markupRegex(marker: Char): String =
            "(?:^|.{0}|[$PRE])([$marker]($BORDER|$BORDER$BODY$BORDER)[$marker])(?:[$POST]|$)"

    private val MARKUP_PATTERN = Pattern.compile(
            markupRegex('*') + "|" +
                    markupRegex('/') + "|" +
                    markupRegex('_') + "|" +
                    markupRegex('=') + "|" +
                    markupRegex('~') + "|" +
                    markupRegex('+'), Pattern.MULTILINE)

    private fun linkPattern(str: String) = Pattern.compile(str)
    private fun bracketLinkPattern(str: String) = Pattern.compile("\\[\\[$str]]")
    private fun namedBracketLinkPattern(str: String) = Pattern.compile("\\[\\[$str]\\[([^]]+)]]")

    private val FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

    @JvmOverloads
    fun parse(context: Context, str: String, linkify: Boolean = true): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(str)

        parsePropertyLinks(ssb, CUSTOM_ID_LINK, "CUSTOM_ID", linkify)
        parsePropertyLinks(ssb, ID_LINK, "ID", linkify)

        parseOrgLinksWithName(ssb, BRACKET_LINK, linkify)
        parseOrgLinksWithName(ssb, BRACKET_ANY_LINK, false)

        parseOrgLinks(ssb, BRACKET_LINK, linkify)

        parsePlainLinks(ssb, PLAIN_LINK, linkify)

        parseMarkup(ssb, context)

        return ssb
    }

    /**
     * [[ http://link.com ][ name ]]
     */
    private fun parseOrgLinksWithName(ssb: SpannableStringBuilder, linkRegex: String, createLinks: Boolean) {
        val p = namedBracketLinkPattern(linkRegex)
        val m = p.matcher(ssb)

        while (m.find()) {
            val link = m.group(1)
            val name = m.group(3)

            ssb.replace(m.start(), m.end(), name)

            if (createLinks) {
                setUrlSpan(ssb, link, m.start(), m.start() + name.length)
            }

            m.reset(ssb) // sbb size modified, reset Matcher
        }
    }

    /**
     * [[ http://link.com ]]
     */
    private fun parseOrgLinks(ssb: SpannableStringBuilder, linkRegex: String, createLinks: Boolean) {
        val p = bracketLinkPattern(linkRegex)
        val m = p.matcher(ssb)

        while (m.find()) {
            val link = m.group(1)

            ssb.replace(m.start(), m.end(), link)

            if (createLinks) {
                setUrlSpan(ssb, link, m.start(), m.start() + link.length)
            }

            m.reset(ssb) // sbb size modified, reset Matcher
        }
    }

    /**
     * [[ #custom id ]] and [[ #custom id ][ link ]]
     * [[ id:id ]] and [[ id:id ][ link ]]
     */
    private fun parsePropertyLinks(ssb: SpannableStringBuilder, linkRegex: String, propName: String, createLinks: Boolean) {
        fun p(p: Pattern, propGroup: Int, linkGroup: Int) {
            val m = p.matcher(ssb)

            while (m.find()) {
                val link = m.group(linkGroup)
                val propValue = m.group(propGroup)

                ssb.replace(m.start(), m.end(), link)

                if (createLinks) {
                    ssb.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val intent = Intent(widget.context, ActionService::class.java)
                            intent.action = AppIntent.ACTION_OPEN_NOTE
                            intent.putExtra(AppIntent.EXTRA_PROPERTY_NAME, propName)
                            intent.putExtra(AppIntent.EXTRA_PROPERTY_VALUE, propValue)
                            ActionService.enqueueWork(widget.context, intent)
                        }
                    }, m.start(), m.start() + link.length, FLAGS)
                }

                m.reset(ssb) // sbb size modified, reset Matcher
            }
        }

        p(namedBracketLinkPattern(linkRegex), 2, 3)
        p(bracketLinkPattern(linkRegex), 2, 1)
    }

    /**
     * http://link.com
     */
    private fun parsePlainLinks(ssb: SpannableStringBuilder, linkRegex: String, createLinks: Boolean) {
        if (createLinks) {
            val p = linkPattern(linkRegex)
            val m = p.matcher(ssb)

            while (m.find()) {
                val link = m.group(1)

                // Make sure first character has no URLSpan
                if (ssb.getSpans(m.start(), m.start() + 1, URLSpan::class.java).isEmpty()) {
                    setUrlSpan(ssb, link, m.start(), m.end())
                }
            }
        }
    }

    private fun setUrlSpan(ssb: SpannableStringBuilder, link: String, start: Int, end: Int) {
        ssb.setSpan(URLSpan(link), start, end, FLAGS)
    }

    private fun parseMarkup(ssb: SpannableStringBuilder, context: Context?) {
        /* Parse if context is null or if option is enabled. */
        if (context != null && !AppPreferences.styleText(context)) {
            return
        }

        val withMarks = AppPreferences.styledTextWithMarks(context)

        fun setMarkupSpan(matcher: Matcher, group: Int, span: Any) {
            // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Type matched", withMarks, matcher.start(group), matcher.end(group))

            if (withMarks) {
                ssb.setSpan(span, matcher.start(group), matcher.end(group), FLAGS)

            } else {
                // Next group matches content only, without markers.
                val content = matcher.group(group + 1)

                ssb.replace(matcher.start(group), matcher.end(group), content)

                ssb.setSpan(span, matcher.start(group), matcher.start(group) + content.length, FLAGS)

                matcher.reset(ssb) // sbb size modified, reset Matcher
            }
        }

        val m = MARKUP_PATTERN.matcher(ssb)

        while (m.find()) {
            // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Matched", ssb.toString(), MARKUP_PATTERN, m.groupCount(), m.group(), m.start(), m.end())

            when {
                m.group(1)  != null -> setMarkupSpan(m,  1, StyleSpan(Typeface.BOLD))
                m.group(3)  != null -> setMarkupSpan(m,  3, StyleSpan(Typeface.ITALIC))
                m.group(5)  != null -> setMarkupSpan(m,  5, UnderlineSpan())
                m.group(7)  != null -> setMarkupSpan(m,  7, TypefaceSpan("monospace"))
                m.group(9)  != null -> setMarkupSpan(m,  9, TypefaceSpan("monospace"))
                m.group(11) != null -> setMarkupSpan(m, 11, StrikethroughSpan())
            }
        }
    }

    // private val TAG = OrgFormatter::class.java.name
}

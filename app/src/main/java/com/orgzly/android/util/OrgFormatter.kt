package com.orgzly.android.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.ui.views.style.*
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 *
 */
object OrgFormatter {

    private const val SYSTEM_LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto"

    private const val CUSTOM_LINK_SCHEMES = "id|file|attachment"

    // Supported link schemas for plain links
    private const val LINK_SCHEMES = "(?:$SYSTEM_LINK_SCHEMES|$CUSTOM_LINK_SCHEMES)"

    private val LINK_REGEX =
            """($LINK_SCHEMES:\S+)|(\[\[(.+?)](?:\[(.+?)])?])""".toRegex()

    private const val PRE = "- \t('\"{"
    private const val POST = "- \\t.,:!?;'\")}\\["
    private const val BORDER = "\\S"
    private const val BODY = ".*?(?:\n.*?)?"

    private const val MARKUP_CHARS = "*/_=~+"

    private val MARKUP_PATTERN = Pattern.compile(
            "(?:^|\\G|[$PRE])(([$MARKUP_CHARS])($BORDER|$BORDER$BODY$BORDER)\\2)(?:[$POST]|$)",
            Pattern.MULTILINE)

    const val LAST_REPEAT_PROPERTY = "LAST_REPEAT"

    private const val LOGBOOK_DRAWER_NAME = "LOGBOOK"

    private fun drawerPattern(name: String) = Pattern.compile(
            """^[ \t]*:($name):[ \t]*\n(.*?)\n[ \t]*:END:[ \t]*$""",
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)

    private val ANY_DRAWER_PATTERN = drawerPattern("[-a-zA-Z_0-9]+")

    private val LOGBOOK_DRAWER_PATTERN = drawerPattern(LOGBOOK_DRAWER_NAME)

    private const val PLAIN_LIST_CHARS = "-\\+"
    private val CHECKBOXES_PATTERN = Pattern.compile("""^\s*[$PLAIN_LIST_CHARS]\s+(\[[ X]])""", Pattern.MULTILINE)

    private const val FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

    data class Link(val whole: MatchGroup, val link: MatchGroup, val name: MatchGroup)

    private data class SpanRegion(
            val start: Int,
            val end: Int,
            val content: CharSequence,
            val spans: List<Any?> = listOf())

    private data class Config(
            val style: Boolean = true,
            val withMarks: Boolean = false,
            val foldDrawers: Boolean = true,
            val linkify: Boolean = true,
            val parseCheckboxes: Boolean = true) {

        constructor(context: Context?, linkify: Boolean, parseCheckboxes: Boolean): this(
                context != null && AppPreferences.styleText(context),
                context != null && AppPreferences.styledTextWithMarks(context),
                context != null && AppPreferences.drawersFolded(context),
                linkify,
                parseCheckboxes)
    }

    @JvmStatic
    @JvmOverloads
    fun parse(str: CharSequence, context: Context? = null, linkify: Boolean = true, parseCheckboxes: Boolean = true): SpannableStringBuilder {
        return this.parse(str, Config(context, linkify, parseCheckboxes))
    }

    private fun parse(str: CharSequence, config: Config): SpannableStringBuilder {
        var ssb = SpannableStringBuilder(str)

        /* Must be first, since checkboxes need to know their position in str. */
        if (config.parseCheckboxes) {
            parseCheckboxes(ssb)
        }

        ssb = parseLinks(config, ssb)

        ssb = parseMarkup(ssb, config)

        ssb = parseDrawers(ssb, config.foldDrawers)

        return ssb
    }

    private fun parseLinks(config: Config, ssb: SpannableStringBuilder): SpannableStringBuilder {
        return collectRegions(ssb) { spanRegions ->
            LINK_REGEX.findAll(ssb).forEach { match ->
                val spans = mutableListOf<Any>()

                val link = getLinkFromGroups(match.groups)

                getSpanForLink(config, link.link)?.let { span ->
                    spans.add(span)
                }

                // Additional spans could be added here

                val spanRegion = SpanRegion(
                        link.whole.range.first,
                        link.whole.range.last + 1,
                        link.name.value,
                        spans)

                spanRegions.add(spanRegion)
            }
        }
    }

    private fun getLinkFromGroups(groups: MatchGroupCollection): Link {
        return when {
            groups[1] != null ->
                // http://link.com
                Link(whole = groups[1]!!, link = groups[1]!!, name = groups[1]!!)

            groups[4] != null ->
                // [[http://link.com][name]]
                Link(whole = groups[2]!!, link = groups[3]!!, name = groups[4]!!)

            groups[2] != null ->
                // [[http://link.com]]
                Link(whole = groups[2]!!, link = groups[3]!!, name = groups[3]!!)

            else -> throw IllegalStateException()
        }
    }

    private fun getSpanForLink(config: Config, match: MatchGroup): Any? {
        if (!config.linkify) {
            return null
        }

        val link = match.value

        return when {
            link.startsWith("file:") ->
                FileLinkSpan(link.substring(5))

            link.startsWith("id:") ->
                IdLinkSpan(link.substring(3))

            link.startsWith("attachment:") ->
                AttachmentLinkSpan(link.substring(11))

            link.startsWith("#") ->
                CustomIdLinkSpan(link.substring(1))

            link.matches("^(?:$SYSTEM_LINK_SCHEMES):.+".toRegex()) ->
                URLSpan(link)

            isFile(link) ->
                FileLinkSpan(link)

            else ->
                SearchLinkSpan(link)
        }
    }

    // TODO: Check for existence if not too slow
    private fun isFile(@Suppress("UNUSED_PARAMETER") str: String): Boolean {
        return true
    }

    enum class SpanType {
        BOLD,
        ITALIC,
        UNDERLINE,
        MONOSPACE,
        STRIKETHROUGH
    }

    /**
     * @return Number of types found
     */
    private fun spanTypes(str: String, f: (SpanType) -> Any): Int {
        var found = 0

        for (i in 0 until str.length/2) {
            val fst = str[i]
            val lst = str[str.length - 1 - i]

            if (fst == lst) {
                val type = when (fst) {
                    '*' -> SpanType.BOLD
                    '/' -> SpanType.ITALIC
                    '_' -> SpanType.UNDERLINE
                    '=' -> SpanType.MONOSPACE
                    '~' -> SpanType.MONOSPACE
                    '+' -> SpanType.STRIKETHROUGH
                    else -> return found
                }

                f(type)

                found++
            }
        }

        return found
    }

    private fun newSpan(type: SpanType): Any {
        return when (type) {
            SpanType.BOLD -> StyleSpan(Typeface.BOLD)
            SpanType.ITALIC -> StyleSpan(Typeface.ITALIC)
            SpanType.UNDERLINE -> UnderlineSpan()
            SpanType.MONOSPACE -> TypefaceSpan("monospace")
            SpanType.STRIKETHROUGH -> StrikethroughSpan()
        }
    }

    private fun parseMarkup(ssb: SpannableStringBuilder, config: Config): SpannableStringBuilder {
        if (!config.style) {
            return ssb
        }

        val spanRegions: MutableList<SpanRegion> = mutableListOf()

        fun setMarkupSpan(matcher: Matcher) {
            // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Type matched", matcher.start(group), matcher.end(group))

            val str = matcher.group(1)!!
            val start = matcher.start(1)
            val end = matcher.end(1)

            if (config.withMarks) {
                spanTypes(str) { type ->
                    ssb.setSpan(newSpan(type), start, end, FLAGS)
                }

            } else {
                val spans = mutableListOf<Any>()

                val found = spanTypes(str) {
                    spans.add(newSpan(it))
                }

                // Content only, without markers
                val content = str.substring(found, str.length - found)

                spanRegions.add(SpanRegion(start, end, content, spans))
            }
        }

        val m = MARKUP_PATTERN.matcher(ssb)

        while (m.find()) {
            // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Matched", ssb.toString(), MARKUP_PATTERN, m.groupCount(), m.group(), m.start(), m.end())
            setMarkupSpan(m)
        }

        return buildFromRegions(ssb, spanRegions)
    }

    /**
     * Parse checkboxes and add CheckboxSpans to ssb
     */
    private fun parseCheckboxes(ssb: SpannableStringBuilder) {
        val m = CHECKBOXES_PATTERN.matcher(ssb)

        while (m.find()) {
            val content = m.group(1)

            if (content != null) {
                val start = m.start(1)
                val end = m.end(1)

                ssb.setSpan(CheckboxSpan(content, start, end), start, end, FLAGS)
                ssb.setSpan(TypefaceSpan("monospace"), start, end, FLAGS)
                ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, FLAGS)
            }
        }
    }

    private fun parseDrawers(ssb: SpannableStringBuilder, foldDrawers: Boolean): SpannableStringBuilder {
        val m = ANY_DRAWER_PATTERN.matcher(ssb)

        return collectRegions(ssb) { spanRegions ->
            while (m.find()) {
                val name = m.group(1)!!

                // Use subSequence to keep existing spans
                val contentStart = m.start(2)
                val contentEnd = m.end(2)
                val content = ssb.subSequence(contentStart, contentEnd)


                // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found drawer", name, content, "All:'${m.group()}'")

                val drawerSpanned = TextViewWithMarkup.drawerSpanned(name, content, foldDrawers)

                val start = if (m.group().startsWith("\n")) m.start() + 1 else m.start()
                val end = if (m.group().endsWith("\n")) m.end() - 1 else m.end()

                spanRegions.add(SpanRegion(start, end, drawerSpanned))
            }
        }
    }

    private fun collectRegions(ssb: SpannableStringBuilder, collect: (MutableList<SpanRegion>) -> Any): SpannableStringBuilder {
        val spanRegions: MutableList<SpanRegion> = mutableListOf()

        collect(spanRegions)

        return buildFromRegions(ssb, spanRegions)
    }

    private fun buildFromRegions(ssb: SpannableStringBuilder, spanRegions: MutableList<SpanRegion>): SpannableStringBuilder {
        if (spanRegions.isNotEmpty()) {
            val builder = SpannableStringBuilder()

            var pos = 0

            spanRegions.forEach { region ->
                // Append everything before region
                if (region.start > pos) {
                    builder.append(ssb.subSequence(pos, region.start))
                }

                // Create spanned string
                val str = SpannableString(region.content)

                // Set spans
                region.spans.forEach { span ->
                    str.setSpan(span, 0, str.length, FLAGS)
                }

                // Append spanned string
                builder.append(str)

                // Move current position after region
                pos = region.end
            }

            // Append the rest
            if (pos < ssb.length) {
                builder.append(ssb.subSequence(pos, ssb.length))
            }

            return builder

        } else {
            return ssb
        }
    }

    @JvmStatic
    fun insertLogbookEntryLine(content: String?, entry: String): String {
        return if (content.isNullOrEmpty()) {
            insertLogbookEntryLineWithoutDrawer(content, entry)

        } else {
            val m = LOGBOOK_DRAWER_PATTERN.matcher(content)

            if (m.find()) {
                val start = m.start(2) // Content start
                StringBuilder(content).insert(start, "$entry\n").toString()

            } else {
                insertLogbookEntryLineWithoutDrawer(content, entry)
            }
        }
    }

    private fun insertLogbookEntryLineWithoutDrawer(content: String?, entry: String): String {
        val prefixedContent = if (content.isNullOrEmpty()) "" else "\n\n$content"
        return ":$LOGBOOK_DRAWER_NAME:\n$entry\n:END:$prefixedContent"
    }

    @JvmStatic
    fun stateChangeLine(fromState: String?, toState: String?, time: String): String {
        val from = if (fromState.isNullOrEmpty()) "" else fromState
        val to = if (toState.isNullOrEmpty()) "" else toState

        return String.format("- State %-12s from %-12s %s", "\"$to\"", "\"$from\"", time)
    }

    private val TAG = OrgFormatter::class.java.name
}

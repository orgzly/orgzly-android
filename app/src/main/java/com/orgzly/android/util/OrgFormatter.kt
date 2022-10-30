package com.orgzly.android.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.orgzly.BuildConfig
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.views.style.*
import com.orgzly.org.datetime.OrgDateTime
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 *
 */
object OrgFormatter {

    private const val SYSTEM_LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto"

    private const val CUSTOM_LINK_SCHEMES = "id|file"

    // Supported link schemas for plain links
    private const val LINK_SCHEMES = "(?:$SYSTEM_LINK_SCHEMES|$CUSTOM_LINK_SCHEMES)"

    private val LINK_REGEX =
        """(?<![a-zA-Z0-9_@%:])($LINK_SCHEMES:\S+)|(\[\[(.+?)](?:\[(.+?)])?])""".toRegex()

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

    private const val INACTIVE_DATETIME = "(\\[[0-9]{4,}-[0-9]{2}-[0-9]{2} ?[^\\]\\r\\n>]*?[0-9]{1,2}:[0-9]{2}\\])"
    private val CLOCKED_TIMES_P = Pattern.compile("(CLOCK: *$INACTIVE_DATETIME) *(-- *$INACTIVE_DATETIME)?( *=> *[0-9]{1,4}:[0-9]{2})?[\\r\\n]*")
    private val INACTIVE_DATETIME_PATTERN = Pattern.compile(INACTIVE_DATETIME)

    private const val FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

    data class MatchLink(
        val all: MatchGroup,
        val url: MatchGroup,
        val name: MatchGroup,
        val type: Int)

    private data class SpanRegion(
            val start: Int,
            val end: Int,
            val content: CharSequence,
            val spans: List<Any?> = listOf())

    // TODO: Pass to OrgFormatter, don't pass context
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
        val t0 = System.currentTimeMillis()

        var ssb = SpannableStringBuilder(str)

        /* Must be first, since checkboxes need to know their position in str. */
        if (config.parseCheckboxes) {
            parseCheckboxes(ssb)
        }

        ssb = parseLinks(config, ssb)

        ssb = parseMarkup(ssb, config)

        ssb = parseDrawers(ssb, config.foldDrawers)

        if (BuildConfig.LOG_DEBUG) {
            val t1 = System.currentTimeMillis()
            LogUtils.d(TAG, "Parsed ${str.length} characters in ${t1-t0}ms")
        }

        return ssb
    }

    private fun parseLinks(config: Config, ssb: SpannableStringBuilder): SpannableStringBuilder {
        return collectRegions(ssb) { result ->
            LINK_REGEX.findAll(ssb).forEach { match ->
                val spans = mutableListOf<Any>()

                val matchLink = getLinkFromGroups(match.groups)

                createSpanForLink(config, matchLink)?.let { span ->
                    spans.add(span)
                }

                // Additional spans could be added here

                val spanRegion = SpanRegion(
                        matchLink.all.range.first,
                        matchLink.all.range.last + 1,
                        matchLink.name.value,
                        spans)

                result.add(spanRegion)
            }
        }
    }

    private fun getLinkFromGroups(groups: MatchGroupCollection): MatchLink {
        return when {
            groups[1] != null -> // http://link.com
                MatchLink(
                    all = groups[1]!!,
                    url = groups[1]!!,
                    name = groups[1]!!,
                    type = LinkSpan.TYPE_NO_BRACKETS)

            groups[4] != null -> // [[http://link.com][name]]
                MatchLink(
                    all = groups[2]!!,
                    url = groups[3]!!,
                    name = groups[4]!!,
                    type = LinkSpan.TYPE_BRACKETS_WITH_NAME)

            groups[2] != null -> // [[http://link.com]]
                MatchLink(
                    all = groups[2]!!,
                    url = groups[3]!!,
                    name = groups[3]!!,
                    type = LinkSpan.TYPE_BRACKETS)

            else -> throw IllegalStateException()
        }
    }

    private fun createSpanForLink(config: Config, matchLink: MatchLink): Any? {
        if (!config.linkify) {
            return null
        }

        val linkType = matchLink.type
        val link = matchLink.url.value
        val name = matchLink.name.value

        return when {
            link.startsWith(FileLinkSpan.PREFIX) ->
                FileLinkSpan(linkType, link, name)

            link.startsWith(IdLinkSpan.PREFIX) ->
                IdLinkSpan(linkType, link, name)

            link.startsWith(CustomIdLinkSpan.PREFIX) ->
                CustomIdLinkSpan(linkType, link, name)

            link.matches("^(?:$SYSTEM_LINK_SCHEMES):.+".toRegex()) ->
                UrlLinkSpan(linkType, link, name)

            isFile(link) ->
                FileOrNotLinkSpan(linkType, link, name)

            else ->
                SearchLinkSpan(linkType, link, name)
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
        VERBATIM,
        CODE,
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
                    '=' -> SpanType.VERBATIM
                    '~' -> SpanType.CODE
                    '+' -> SpanType.STRIKETHROUGH
                    else -> return found
                }

                f(type)

                found++
            }
        }

        return found
    }

    private fun newSpan(type: SpanType): CharacterStyle {
        return when (type) {
            SpanType.BOLD -> BoldSpan()
            SpanType.ITALIC -> ItalicSpan()
            SpanType.UNDERLINE -> UnderlinedSpan()
            SpanType.VERBATIM -> VerbatimSpan()
            SpanType.CODE -> CodeSpan()
            SpanType.STRIKETHROUGH -> StrikeSpan()
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
                val spans = mutableListOf<CharacterStyle>()

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

                val drawerSpanned = drawerSpanned(name, content, foldDrawers)

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
                val str = SpannableString(region.content).also { str ->
                    region.spans.forEach { span ->
                        str.setSpan(span, 0, str.length, FLAGS)
                    }
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

    private fun getLastClockInPosition(logbookContent: String): ArrayList<Int> {
        val pos = ArrayList<Int>()

        val m = CLOCKED_TIMES_P.matcher(logbookContent)
        while (m.find()) {
            // Check if we have one entry without clock-out
            if (m.group(3).isNullOrEmpty()) {
                pos.add(m.start(1))
                pos.add(m.end(1))
                break
            }
        }

        return pos
    }

    @JvmStatic
    fun clockIn(content: String?): String {
        val currentTime = OrgDateTime(false).toString()
        val clockStr = "CLOCK: $currentTime"

        return if (content.isNullOrEmpty()) {
            insertLogbookEntryLineWithoutDrawer(content, clockStr)
        } else {
            val m = LOGBOOK_DRAWER_PATTERN.matcher(content)

            if (m.find()) {
                val logbookContent = content.substring(m.start(2), m.end(2))
                val pos = getLastClockInPosition(logbookContent)

                // If we cannot find an already clocked entry, we add one
                if (pos.isEmpty()) {
                    StringBuilder(content).insert(m.start(2), clockStr + "\n").toString()
                } else {
                    content
                }
            } else {
                insertLogbookEntryLineWithoutDrawer(content, clockStr)
            }
        }
    }

    @JvmStatic
    fun clockOut(content: String?): String {
        val clockOut = OrgDateTime(false)
        val currentTime = clockOut.toString()

        return if (content.isNullOrEmpty()) {
            ""
        } else {
            val m = LOGBOOK_DRAWER_PATTERN.matcher(content)

            if (m.find()) {
                val logbookContent = content.substring(m.start(2), m.end(2))
                val pos = getLastClockInPosition(logbookContent)

                // If we find an already clocked entry, we clock-out
                if (pos.isNotEmpty()) {
                    // Find the timestamp to compute the difference
                    val m2 = INACTIVE_DATETIME_PATTERN.matcher(logbookContent.substring(pos[0], pos[1]))

                    if (m2.find()) {
                        val clockIn = OrgDateTime.parseOrNull(m2.group(1))

                        val timeIn = clockIn.calendar
                        val timeOut = clockOut.calendar

                        // Reported times are in milliseconds
                        val diff = timeOut.time.time - timeIn.time.time

                        val minute = 1000 * 60
                        val hour = minute * 60

                        val elapsedHours: Long = diff / hour
                        val diff2 = diff % hour
                        val elapsedMinutes: Long = diff2 / minute

                        StringBuilder(content).insert(m.start(2) + pos[1], "--" + currentTime + " => " + elapsedHours + ":" + String.format("%02d", elapsedMinutes)).toString()

                    } else { // This should never happen, but put as a precaution
                        StringBuilder(content).insert(m.start(2) + pos[1], "--" + currentTime).toString()
                    }

                } else {
                    content
                }
            } else {
                content
            }
        }
    }

    @JvmStatic
    fun clockCancel(content: String?): String {
        return if (content.isNullOrEmpty()) {
            ""
        } else {
            val m = LOGBOOK_DRAWER_PATTERN.matcher(content)

            if (m.find()) {
                val logbookContent = content.substring(m.start(2), m.end(2))
                val pos = getLastClockInPosition(logbookContent)

                // If we find an already clocked entry, we cancel it
                if (pos.isNotEmpty()) {
                    val updatedContent = StringBuilder(content).delete(m.start(2) + pos[0], m.start(2) + pos[1] + 1).toString()

                    // If the LOGBOOK ends up being empty
                    // We delete it like Org would do
                    val endPos = updatedContent.indexOf(":END:", m.start(2))
                    if (endPos == (m.start(2)) ) {
                        StringBuilder(updatedContent).delete(m.start(0), m.start(0) + endPos + ":END:".length + 2).toString()
                    } else {
                        updatedContent
                    }
                } else {
                    content
                }
            } else {
                content
            }
        }
    }

    @JvmStatic
    fun stateChangeLine(fromState: String?, toState: String?, time: String): String {
        val from = if (fromState.isNullOrEmpty()) "" else fromState
        val to = if (toState.isNullOrEmpty()) "" else toState

        return String.format("- State %-12s from %-12s %s", "\"$to\"", "\"$from\"", time)
    }

    fun drawerSpanned(name: String, content: CharSequence, isFolded: Boolean): Spanned {
        val builder = SpannableStringBuilder()

        if (isFolded) {
            builder.append(":$name:…")

            builder.setSpan(
                DrawerMarkerSpan.Start(),
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        } else {
            builder.append(":$name:")

            builder.setSpan(
                DrawerMarkerSpan.Start(),
                0,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            builder.append("\n").append(content).append("\n").append(":END:")
        }

        builder.setSpan(
            DrawerSpan(name, content, isFolded),
            0,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return builder
    }

    fun checkboxSpanned(content: CharSequence, rawStart: Int, rawEnd: Int): Spanned {
        val beginSpannable = SpannableString(content)

        beginSpannable.setSpan(
            CheckboxSpan(content, rawStart, rawEnd),
            0,
            beginSpannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return beginSpannable
    }

    private val TAG = OrgFormatter::class.java.name
}

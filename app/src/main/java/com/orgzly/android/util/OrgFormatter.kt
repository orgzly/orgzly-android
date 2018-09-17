package com.orgzly.android.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Environment
import android.support.v4.content.ContextCompat.startActivity
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import android.view.View
import android.widget.Toast
import com.orgzly.android.App
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.ui.views.style.CheckboxSpan
import java.util.regex.Matcher
import java.util.regex.Pattern
import android.support.v4.content.FileProvider
import com.orgzly.BuildConfig
import com.orgzly.R
import java.io.File



/**
 *
 */
object OrgFormatter {
    private const val LINK_SCHEMES = "https?|mailto|tel|voicemail|geo|sms|smsto|mms|mmsto"

    // tel:1234567
    private const val PLAIN_LINK = "(($LINK_SCHEMES):\\S+)"

    /* Same as the above, but ] ends the link too. Used for bracket links. */
    private const val BRACKET_LINK = "(($LINK_SCHEMES):[^]\\s]+)"

    // #custom id
    private const val CUSTOM_ID_LINK = "(#([^]]+))"

    // id:CABA8098-5969-429E-A780-94C8E0A9D206
    private const val HD = "[0-9a-fA-F]"
    private const val ID_LINK = "(id:($HD{8}-(?:$HD{4}-){3}$HD{12}))"

    /* Allows anything as a link. Probably needs some constraints.
     * See http://orgmode.org/manual/External-links.html and org-any-link-re
     */
    private const val BRACKET_ANY_LINK = "(([^]]+))"

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

    private fun namelessBracketLinkPattern(str: String) = Pattern.compile("\\[\\[$str]]")
    private fun namedBracketLinkPattern(str: String) = Pattern.compile("\\[\\[$str]\\[([^]]+)]]")

    private val CHECKBOXES_PATTERN = Pattern.compile("""^\s*-\s+(\[[ X]])""", Pattern.MULTILINE)

    private val FILELINKS_PATTERN = Pattern.compile("file:([^\\s]+)")

    private const val FLAGS = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

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
    fun parse(str: String, context: Context? = null, linkify: Boolean = true, parseCheckboxes: Boolean = true): SpannableStringBuilder {
        return this.parse(str, Config(context, linkify, parseCheckboxes))
    }

    private fun parse(str: String, config: Config): SpannableStringBuilder {
        var ssb = SpannableStringBuilder(str)

        /* Must be first, since checkboxes need to know their position in str. */
        if (config.parseCheckboxes) {
            parseCheckboxes(ssb)
        }

        ssb = parsePropertyLinks(ssb, CUSTOM_ID_LINK, "CUSTOM_ID", config.linkify)
        ssb = parsePropertyLinks(ssb, ID_LINK, "ID", config.linkify)

        ssb = parseOrgLinksWithName(ssb, BRACKET_LINK, config.linkify)
        ssb = parseOrgLinksWithName(ssb, BRACKET_ANY_LINK, false)

        ssb = parseOrgLinks(ssb, BRACKET_LINK, config.linkify)

        parsePlainLinks(ssb, PLAIN_LINK, config.linkify)

        parseFilelinks(ssb)

        ssb = parseMarkup(ssb, config)

        ssb = parseDrawers(ssb, config.foldDrawers)

        return ssb
    }

//    private fun logSpans(ssb: SpannableStringBuilder) {
//        val spans = ssb.getSpans(0, ssb.length - 1, Any::class.java)
//        LogUtils.d(TAG, "--- Spans ---", spans.size)
//        spans.forEach {
//            LogUtils.d(TAG, "Span", it, it.javaClass.simpleName, ssb.getSpanStart(it), ssb.getSpanEnd(it))
//        }
//    }

    /**
     * [[ http://link.com ][ name ]]
     */
    private fun parseOrgLinksWithName(ssb: SpannableStringBuilder, linkRegex: String, linkify: Boolean): SpannableStringBuilder {
        val p = namedBracketLinkPattern(linkRegex)
        val m = p.matcher(ssb)

        return collectRegions(ssb) { spanRegions ->
            while (m.find()) {
                val link = m.group(1)
                val name = m.group(3)

                val span = if (linkify) URLSpan(link) else null

                spanRegions.add(SpanRegion(m.start(), m.end(), name, listOf(span)))
            }
        }
    }

    /**
     * [[ http://link.com ]]
     */
    private fun parseOrgLinks(ssb: SpannableStringBuilder, linkRegex: String, linkify: Boolean): SpannableStringBuilder {
        val p = namelessBracketLinkPattern(linkRegex)
        val m = p.matcher(ssb)

        return collectRegions(ssb) { spanRegions ->
            while (m.find()) {
                val link = m.group(1)

                val span = if (linkify) URLSpan(link) else null

                spanRegions.add(SpanRegion(m.start(), m.end(), link, listOf(span)))
            }
        }
    }

    /**
     * [[ #custom id ]] and [[ #custom id ][ link ]]
     * [[ id:id ]] and [[ id:id ][ link ]]
     */
    private fun parsePropertyLinks(ssb: SpannableStringBuilder, linkRegex: String, propName: String, linkify: Boolean): SpannableStringBuilder {
        val builder = parsePropertyLinkType(ssb, linkify, propName, namedBracketLinkPattern(linkRegex), 2, 3)
        return parsePropertyLinkType(builder, linkify, propName, namelessBracketLinkPattern(linkRegex), 2, 1)
    }

    private fun parsePropertyLinkType(
            ssb: SpannableStringBuilder,
            linkify: Boolean,
            propName: String,
            pattern: Pattern,
            propValueGroup: Int,
            linkGroup: Int): SpannableStringBuilder {

        val m = pattern.matcher(ssb)

        return collectRegions(ssb) { spanRegions ->
            while (m.find()) {
                val link = m.group(linkGroup)
                val propValue = m.group(propValueGroup)

                val span = if (linkify) {
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            if (widget is TextViewWithMarkup) {
                                widget.openNoteWithProperty(propName, propValue)
                            }
                        }
                    }

                } else {
                    null
                }

                spanRegions.add(SpanRegion(m.start(), m.end(), link, listOf(span)))
            }
        }
    }

    /**
     * http://link.com
     */
    private fun parsePlainLinks(ssb: SpannableStringBuilder, linkRegex: String, linkify: Boolean) {
        if (linkify) {
            val p = Pattern.compile(linkRegex)
            val m = p.matcher(ssb)

            while (m.find()) {
                val link = m.group(1)

                // Make sure first character has no URLSpan already
                if (ssb.getSpans(m.start(), m.start() + 1, URLSpan::class.java).isEmpty()) {
                    ssb.setSpan(URLSpan(link), m.start(), m.end(), FLAGS)
                }
            }
        }
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

            val str = matcher.group(1)
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
            val start = m.start(1)
            val end = m.end(1)
            ssb.setSpan(CheckboxSpan(content, start, end), start, end, FLAGS)
            ssb.setSpan(TypefaceSpan("monospace"), start, end, FLAGS)
            ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, FLAGS)
        }
    }

    /**
     * Parse file paths
     */
    private fun parseFilelinks(ssb: SpannableStringBuilder) {
        val m = FILELINKS_PATTERN.matcher(ssb)

        // Try to find a matching file path
        while (m.find()) {
            val content = m.group()
            val start = m.start()
            val end = m.end()

            // Split the "file:path" expression
            val s = content.split(":")

            // Ensure we have a path component
            if(s.size == 2)
            {
                // Create a ClickableSpan that allows to open the file
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View?) {
                        val context = App.getAppContext()

                        // Get the current activity is available
                        val currentActivity = App.getCurrentActivity()

                        // Check that we have the permission to read external files
                        // or ask for it and run the associated code
                        currentActivity?.runWithPermission(
                            AppPermissions.Usage.EXTERNAL_FILES_ACCESS,
                            Runnable {
                                // Get the file
                                val file = File(Environment.getExternalStorageDirectory(), s[1])

                                // Check file existence, before trying to process it
                                if (file.exists()) {
                                    val contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)

                                    val intent = Intent(Intent.ACTION_VIEW, contentUri)
                                    // Added for support on API 16
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                                    // Try to start an activity for opening the file
                                    try {
                                        startActivity(context, intent, null)
                                    } catch (e: ActivityNotFoundException) {
                                        currentActivity.showSnackbar(R.string.external_file_no_app_found)
                                    }
                                } else {
                                    currentActivity.showSnackbar(context.getString(R.string.file_does_not_exist, file.absolutePath))
                                }
                            }
                        );
                    }
                }

                ssb.setSpan(clickableSpan, start, end, FLAGS)
            }
        }
    }

    private fun parseDrawers(ssb: SpannableStringBuilder, foldDrawers: Boolean): SpannableStringBuilder {
        val m = ANY_DRAWER_PATTERN.matcher(ssb)

        return collectRegions(ssb) { spanRegions ->
            while (m.find()) {
                val name = m.group(1)

                // Use subSequence to keep existing spans
                val contentStart = m.start(2)
                val contentEnd = m.end(2)
                val content = ssb.subSequence(contentStart, contentEnd)


                // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found drawer", name, content, "All:'${m.group(0)}'")

                val drawerSpanned = TextViewWithMarkup.drawerSpanned(name, content, foldDrawers)

                val start = if (m.group(0).startsWith("\n")) m.start() + 1 else m.start()
                val end = if (m.group(0).endsWith("\n")) m.end() - 1 else m.end()

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

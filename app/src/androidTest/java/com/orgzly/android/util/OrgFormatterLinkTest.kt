package com.orgzly.android.util

import android.text.style.URLSpan
import com.orgzly.android.ui.views.style.AttachmentLinkSpan
import com.orgzly.android.ui.views.style.FileLinkSpan
import com.orgzly.android.ui.views.style.FileOrNotLinkSpan
import com.orgzly.android.ui.views.style.IdLinkSpan
import com.orgzly.android.ui.views.style.UrlLinkSpan
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException

@RunWith(value = Parameterized::class)
class OrgFormatterLinkTest(private val param: Parameter) : OrgFormatterTest() {

    data class Span(val start: Int, val end: Int, val klass: Class<*>)

    data class Parameter(
        val inputString: String,
        val outputString: String,
        val expectedSpans: List<Span>)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        File(context.cacheDir, "orgzly-tests").let { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Failed to create $dir")
            }

            MiscUtils.writeStringToFile("Lorem ipsum", File(dir, "document.txt"))

            val classLoader  = javaClass.classLoader
                ?: throw IOException("Failed to get a class loader for $javaClass")

            classLoader.getResourceAsStream("assets/images/logo.png").use { stream ->
                MiscUtils.writeStreamToFile(stream, File(dir, "logo.png"))
            }
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()

        File(context.cacheDir, "orgzly-tests").let { dir ->
            dir.deleteRecursively()
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Parameter> {
            return listOf(
                Parameter("[[orgzly-tests/document.txt]]", "orgzly-tests/document.txt", listOf(Span(0, 25, FileOrNotLinkSpan::class.java))),
                Parameter("[[./document.txt]]", "./document.txt", listOf(Span(0, 14, FileOrNotLinkSpan::class.java))),
                Parameter("[[/document.txt]]", "/document.txt", listOf(Span(0, 13, FileOrNotLinkSpan::class.java))),
                Parameter("[[document.txt]]", "document.txt", listOf(Span(0, 12, FileOrNotLinkSpan::class.java))),

                Parameter("file:orgzly-tests/document.txt", "file:orgzly-tests/document.txt", listOf(Span(0, 30, FileLinkSpan::class.java))),
                Parameter("[[file:orgzly-tests/document.txt]]", "file:orgzly-tests/document.txt", listOf(Span(0, 30, FileLinkSpan::class.java))),
                Parameter("[[file:orgzly-tests/document.txt][Document]]", "Document", listOf(Span(0, 8, FileLinkSpan::class.java))),

                Parameter("attachment:orgzly-tests/document.txt", "attachment:orgzly-tests/document.txt", listOf(Span(0, 36, AttachmentLinkSpan::class.java))),
                Parameter("[[attachment:orgzly-tests/document.txt]]", "attachment:orgzly-tests/document.txt", listOf(Span(0, 36, AttachmentLinkSpan::class.java))),
                Parameter("[[attachment:orgzly-tests/document.txt][Document]]", "Document", listOf(Span(0, 8, AttachmentLinkSpan::class.java))),

                Parameter("id:45DFE015-255E-4B86-B957-F7FD77364DCA", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", listOf(Span(0, 39, IdLinkSpan::class.java))),
                Parameter("[[id:45DFE015-255E-4B86-B957-F7FD77364DCA]]", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", listOf(Span(0, 39, IdLinkSpan::class.java))),
                Parameter("id:foo", "id:foo", listOf(Span(0, 6, IdLinkSpan::class.java))),
                Parameter("[[id:foo]]", "id:foo", listOf(Span(0, 6, IdLinkSpan::class.java))),

                Parameter("mailto:a@b.com", "mailto:a@b.com", listOf(Span(0, 14, UrlLinkSpan::class.java))),
                Parameter("[[mailto:a@b.com]]", "mailto:a@b.com", listOf(Span(0, 14, UrlLinkSpan::class.java))),

                Parameter("[[id:123][[a] b]]", "[a] b", listOf(Span(0, 5, IdLinkSpan::class.java))),
                Parameter("[[id:123][[a] b]] [[./456][[c] d]]", "[a] b [c] d", listOf(Span(0, 5, IdLinkSpan::class.java), Span(6, 11, FileOrNotLinkSpan::class.java))),

                Parameter("[[gnus:msgid][subject]]", "subject", listOf(Span(0, 7, FileOrNotLinkSpan::class.java))),
                Parameter("[[gnus:\\[Gmail\\]/All Mail#msgid][subject]]", "subject", listOf(Span(0, 7, FileOrNotLinkSpan::class.java))),

                Parameter("[[id:a][b]] [[id:1][2]]", "b 2", listOf(Span(0, 1, IdLinkSpan::class.java), Span(2, 3, IdLinkSpan::class.java))),
                Parameter("[[id:a][b]][[id:1][2]]", "b2", listOf(Span(0, 1, IdLinkSpan::class.java), Span(1, 2, IdLinkSpan::class.java))),

                // Do not linkify
                Parameter("strhttps://www.orgzly.com/", "strhttps://www.orgzly.com/", emptyList()),
                Parameter("Need activity with <action android:name=\"android.intent.action.VIEW\"/>", "Need activity with <action android:name=\"android.intent.action.VIEW\"/>", emptyList())
            )
        }
    }

    @Test
    fun testLink() {
        val parseResult = ParseResult(param.inputString)

        val msg = "${param.inputString} -> ${parseResult.outputString}"

        assertThat(
            "Number of spans found is different then expected: $msg",
            parseResult.foundSpans.size,
            equalTo(param.expectedSpans.size))

        assertThat(parseResult.outputString, equalTo(param.outputString))

        for (i in parseResult.foundSpans.indices) {
            assertThat(parseResult.foundSpans[i].start, equalTo(param.expectedSpans[i].start))
            assertThat(parseResult.foundSpans[i].end, equalTo(param.expectedSpans[i].end))

            assertThat(
                "Found span class is different then expected: $msg",
                parseResult.foundSpans[i].span.javaClass.simpleName,
                equalTo(param.expectedSpans[i].klass.simpleName))
        }
    }
}
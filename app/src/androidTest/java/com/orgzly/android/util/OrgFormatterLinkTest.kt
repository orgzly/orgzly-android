package com.orgzly.android.util

import android.os.Environment
import android.text.style.URLSpan
import com.orgzly.android.ui.views.style.FileLinkSpan
import com.orgzly.android.ui.views.style.IdLinkSpan
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.IOException

@RunWith(value = Parameterized::class)
class OrgFormatterLinkTest(private val param: Parameter) : OrgFormatterTest() {

    data class Parameter(
            val input: String,
            val output: String,
            val size: Int,
            val start: Int,
            val end: Int,
            val type: Class<*>? = null)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        File(Environment.getExternalStorageDirectory(), "orgzly-tests").let { dir ->
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

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter("[[orgzly-tests/document.txt]]", "orgzly-tests/document.txt", 1, 0, 25, FileLinkSpan::class.java),
                    Parameter("[[./document.txt]]", "./document.txt", 1, 0, 14, FileLinkSpan::class.java),
                    Parameter("[[/document.txt]]", "/document.txt", 1, 0, 13, FileLinkSpan::class.java),
                    Parameter("[[document.txt]]", "document.txt", 1, 0, 12, FileLinkSpan::class.java),

                    Parameter("file:orgzly-tests/document.txt", "file:orgzly-tests/document.txt", 1, 0, 30, FileLinkSpan::class.java),
                    Parameter("[[file:orgzly-tests/document.txt]]", "file:orgzly-tests/document.txt", 1, 0, 30, FileLinkSpan::class.java),
                    Parameter("[[file:orgzly-tests/document.txt][Document]]", "Document", 1, 0, 8, FileLinkSpan::class.java),

                    Parameter("id:45DFE015-255E-4B86-B957-F7FD77364DCA", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", 1, 0, 39, IdLinkSpan::class.java),
                    Parameter("[[id:45DFE015-255E-4B86-B957-F7FD77364DCA]]", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", 1, 0, 39, IdLinkSpan::class.java),
                    Parameter("id:foo", "id:foo", 1, 0, 6, IdLinkSpan::class.java),
                    Parameter("[[id:foo]]", "id:foo", 1, 0, 6, IdLinkSpan::class.java),

                    Parameter("mailto:a@b.com", "mailto:a@b.com", 1, 0, 14, URLSpan::class.java),
                    Parameter("[[mailto:a@b.com]]", "mailto:a@b.com", 1, 0, 14, URLSpan::class.java)
            )
        }
    }

    @Test
    fun testLink() {
        val spannable = OrgSpannable(param.input)

        assertThat(spannable.string, `is`(param.output))
        assertThat(spannable.spans.size, `is`(param.size))
        assertThat(spannable.spans[0].start, `is`(param.start))
        assertThat(spannable.spans[0].end, `is`(param.end))

        if (param.type != null) {
            assertThat(spannable.spans[0].span.javaClass.simpleName, `is`(param.type.simpleName))
        }
    }
}
package com.orgzly.android.util

import android.os.Environment
import android.text.style.ClickableSpan
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(value = Parameterized::class)
class OrgFormatterLinkTest(private val param: Parameter) : OrgFormatterTest() {

    data class Parameter(
            val input: String,
            val output: String,
            val size: Int,
            val start: Int,
            val end: Int,
            val clickable: Boolean)

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        File(Environment.getExternalStorageDirectory(), "orgzly-tests").let { dir ->
            dir.mkdirs()

            MiscUtils.writeStringToFile("Lorem ipsum", File(dir, "document.txt"))

            javaClass.classLoader.getResourceAsStream("assets/images/logo.png").use { stream ->
                MiscUtils.writeStreamToFile(stream, File(dir, "logo.png"))
            }
        }
    }


    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter("[[orgzly-tests/document.txt]]", "orgzly-tests/document.txt", 1, 0, 25, true),

                    Parameter("file:orgzly-tests/document.txt", "file:orgzly-tests/document.txt", 1, 0, 30, true),
                    Parameter("[[file:orgzly-tests/document.txt]]", "file:orgzly-tests/document.txt", 1, 0, 30, true),
                    Parameter("[[file:orgzly-tests/document.txt][Document]]", "Document", 1, 0, 8, true),

                    Parameter("id:45DFE015-255E-4B86-B957-F7FD77364DCA", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", 1, 0, 39, true),
                    Parameter("[[id:45DFE015-255E-4B86-B957-F7FD77364DCA]]", "id:45DFE015-255E-4B86-B957-F7FD77364DCA", 1, 0, 39, true),

                    Parameter("mailto:a@b.com", "mailto:a@b.com", 1, 0, 14, true),
                    Parameter("[[mailto:a@b.com]]", "mailto:a@b.com", 1, 0, 14, true)
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

        if (param.clickable) {
            assertThat(spannable.spans[0].span, instanceOf(ClickableSpan::class.java))
        } else {
            assertThat(spannable.spans[0].span, not(instanceOf(ClickableSpan::class.java)))
        }
    }
}
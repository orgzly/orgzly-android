package com.orgzly.android.util

import android.os.Environment
import android.text.style.ClickableSpan
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore // TODO: WIP
@Suppress("TestFunctionName")
class OrgFormatterLinkTest : OrgFormatterTest() {
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

    @Test
    fun SquareBrackets_File_Document_Existing() {
        val spannable = OrgSpannable("[[file:orgzly-tests/document.txt]]")

        assertThat(spannable.string, `is`("file:orgzly-tests/document.txt"))
        assertThat(spannable.spans.size, `is`(1))
        assertThat(spannable.spans[0].start, `is`(0))
        assertThat(spannable.spans[0].end, `is`(17))
    }

    @Test
    fun NamedSquareBrackets_File_Document_Existing() {
        val spannable = OrgSpannable("[[file:orgzly-tests/document.txt][Document]]")

        assertThat(spannable.string, `is`("Document"))
        assertThat(spannable.spans.size, `is`(1))
        assertThat(spannable.spans[0].start, `is`(0))
        assertThat(spannable.spans[0].end, `is`(8))
        assertThat(spannable.spans[0].span, instanceOf(ClickableSpan::class.java))
    }

    @Test
    fun PlainLink_Id_UUID_Valid() {
        val spannable = OrgSpannable("id:45DFE015-255E-4B86-B957-F7FD77364DCA")

        assertThat(spannable.string, `is`("id:45DFE015-255E-4B86-B957-F7FD77364DCA"))
        assertThat(spannable.spans.size, `is`(1))
        assertThat(spannable.spans[0].start, `is`(0))
        assertThat(spannable.spans[0].end, `is`(39))
        assertThat(spannable.spans[0].span, instanceOf(ClickableSpan::class.java))
    }

    @Test
    fun SquareBrackets_Id_UUID_Valid() {
        val spannable = OrgSpannable("[[id:45DFE015-255E-4B86-B957-F7FD77364DCA]]")

        assertThat(spannable.string, `is`("id:45DFE015-255E-4B86-B957-F7FD77364DCA"))
        assertThat(spannable.spans.size, `is`(1))
        assertThat(spannable.spans[0].start, `is`(0))
        assertThat(spannable.spans[0].end, `is`(39))
        assertThat(spannable.spans[0].span, instanceOf(ClickableSpan::class.java))
    }
}
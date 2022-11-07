package com.orgzly.android.ui

import android.text.Spannable
import com.orgzly.android.ui.views.richtext.RichText
import com.orgzly.android.ui.views.style.AttachmentLinkSpan
import com.orgzly.android.usecase.FindAttachmentPath
import com.orgzly.android.usecase.UseCaseRunner

object AttachmentSpanLoader {
    /** Find all `attachment:` links and set up the prefix directory based on `ID` property. */
    fun loadAttachmentPaths(noteId: Long, richText: RichText) {
        SpanUtils.forEachSpan(richText.getVisibleText() as Spannable, AttachmentLinkSpan::class.java) { span, _, _ ->
            val prefix = UseCaseRunner.run(FindAttachmentPath(noteId)).userData
            if (prefix != null) {
                span.prefix = prefix as String
            }
        }
    }
}
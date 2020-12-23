package com.orgzly.android.ui

import android.text.Spannable
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.ui.views.style.AttachmentLinkSpan
import com.orgzly.android.usecase.FindAttachmentPath
import com.orgzly.android.usecase.UseCaseRunner

object AttachmentSpanLoader {
    /** Find all `attachment:` links and set up the prefix directory based on `ID` property. */
    fun loadAttachmentPaths(noteId: Long, textWithMarkup: TextViewWithMarkup) {
        SpanUtils.forEachSpan(textWithMarkup.text as Spannable, AttachmentLinkSpan::class.java) { span ->
            val prefix = UseCaseRunner.run(FindAttachmentPath(noteId)).userData
            if (prefix != null) {
                span.prefix = prefix as String
            }
        }
    }
}
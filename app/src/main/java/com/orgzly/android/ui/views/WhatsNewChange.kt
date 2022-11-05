package com.orgzly.android.ui.views

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.orgzly.R
import com.orgzly.android.ui.util.getLayoutInflater
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.util.MiscUtils

class WhatsNewChange(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    init {
        val content = context.styledAttributes(attrs, R.styleable.WhatsNewChange) { typedArray ->
            typedArray.getString(R.styleable.WhatsNewChange_text)
        }

        val layoutView = context.getLayoutInflater().inflate(R.layout.text_list_item, this, true)

        layoutView.findViewById<TextView>(R.id.content).apply {
            text = MiscUtils.fromHtml(content)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
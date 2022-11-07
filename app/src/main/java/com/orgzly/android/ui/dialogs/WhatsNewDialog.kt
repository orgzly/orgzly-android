package com.orgzly.android.ui.dialogs

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.ui.util.getLayoutInflater
import com.orgzly.android.util.MiscUtils

object WhatsNewDialog {
    /**
     * Display dialog with changes.
     */
    fun create(context: Context): AlertDialog {
        val layoutView = context.getLayoutInflater().inflate(R.layout.dialog_whats_new, null, false)

        layoutView.findViewById<TextView>(R.id.dialog_whats_new_intro).apply {
            text = MiscUtils.fromHtml(context.getString(R.string.whats_new_intro))
            movementMethod = LinkMovementMethod.getInstance()
        }

        layoutView.findViewById<TextView>(R.id.dialog_whats_new_outro).apply {
            text = MiscUtils.fromHtml(context.getString(R.string.whats_new_outro))
            movementMethod = LinkMovementMethod.getInstance()
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.whats_new_title)
            .setPositiveButton(R.string.ok, null)
            .setView(layoutView)
            .create()
    }
}
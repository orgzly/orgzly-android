@file:JvmName("SystemActions")

package com.orgzly.android.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent

fun Context.copyPlainTextToClipboard(label: CharSequence, text: CharSequence) {
    getClipboardManager().let { clipboardManager ->
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
}

fun Activity.sharePlainText(text: CharSequence) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

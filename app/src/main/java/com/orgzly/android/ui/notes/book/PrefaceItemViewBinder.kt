package com.orgzly.android.ui.notes.book

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences

class PrefaceItemViewBinder(private val context: Context) {
    fun bind(holder: BookAdapter.PrefaceViewHolder, preface: String?, isPrefaceDisplayed: Boolean) {
        if (!isPrefaceDisplayed) {
            holder.binding.fragmentBookHeaderText.visibility = View.GONE

        } else {
            if (context.getString(R.string.pref_value_preface_in_book_few_lines) ==
                AppPreferences.prefaceDisplay(context)) {

                holder.binding.fragmentBookHeaderText.maxLines = 3
                holder.binding.fragmentBookHeaderText.ellipsize = TextUtils. TruncateAt.END

            } else {
                holder.binding.fragmentBookHeaderText.maxLines = Integer.MAX_VALUE
                holder.binding.fragmentBookHeaderText.ellipsize = null
            }

            if (preface != null) {
                if (AppPreferences.isFontMonospaced(context)) {
                    holder.binding.fragmentBookHeaderText.typeface = Typeface.MONOSPACE
                }
                holder.binding.fragmentBookHeaderText.setSourceText(preface)
            }

            holder.binding.fragmentBookHeaderText.visibility = View.VISIBLE
        }
    }
}
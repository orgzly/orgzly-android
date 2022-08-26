package com.orgzly.android.ui.notes.book

import android.content.Context
import android.graphics.Typeface
import android.view.View
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.usecase.BookUpdatePreface
import com.orgzly.android.usecase.UseCaseRunner

class PrefaceItemViewBinder(private val context: Context) {
    fun bind(holder: BookAdapter.PrefaceViewHolder, bookId: Long, preface: String?, isPrefaceDisplayed: Boolean) {
        holder.binding.itemPrefaceText.apply {
            if (!isPrefaceDisplayed) {
                visibility = View.GONE
                return
            }

            if (context.getString(R.string.pref_value_preface_in_book_few_lines) == AppPreferences.prefaceDisplay(context)) {
                setMaxLines(3)
            } else {
                setMaxLines(Integer.MAX_VALUE)
            }

            if (preface != null) {
                if (AppPreferences.isFontMonospaced(context)) {
                    setTypeface(Typeface.MONOSPACE)
                }
                setSourceText(preface)

                /* If content changes (for example by toggling the checkbox), update the note. */
                setOnUserTextChangeListener { text ->
                    val useCase = BookUpdatePreface(bookId, text)

                    App.EXECUTORS.diskIO().execute {
                        UseCaseRunner.run(useCase)
                    }
                }
            }

            visibility = View.VISIBLE
        }
    }
}
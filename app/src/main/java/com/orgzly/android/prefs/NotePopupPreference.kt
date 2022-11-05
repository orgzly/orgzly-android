package com.orgzly.android.prefs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.IdRes
import androidx.preference.DialogPreference
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.notes.NotePopup
import com.orgzly.android.util.LogUtils

class NotePopupPreference : DialogPreference {
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context!!, attrs, defStyleAttr, defStyleRes
    ) {
        dialogLayoutResource = R.layout.note_popup_pref_dialog
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        dialogLayoutResource = R.layout.note_popup_pref_dialog
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        dialogLayoutResource = R.layout.note_popup_pref_dialog
    }

    constructor(context: Context) : super(context) {
        dialogLayoutResource = R.layout.note_popup_pref_dialog
    }

    override fun getSummary(): CharSequence? {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, key)

        val selectedCount = getSelected(context, key).size
        return context.getString(R.string.argument_selected, selectedCount.toString())
    }

    companion object {
        private val actionsByName = NotePopup.allActions.associateBy { it.name }

        private val dividerLeft = NotePopup.Action(
            R.id.note_popup_divider, R.drawable.ic_swipe_left, "divider")

        private val dividerRight = NotePopup.Action(
            R.id.note_popup_divider, R.drawable.ic_swipe_right, "divider")

        fun getSelected(context: Context, key: String): List<NotePopup.Action> {
            val str = AppPreferences.notePopupActions(context, key)

            return str.split("\\s+".toRegex()).mapNotNull { actionsByName[it] }
        }

        fun getAll(context: Context, key: String): List<NotePopup.Action> {
            val selected = getSelected(context, key)

            val others = NotePopup.allActions - selected.toSet()

            val divider = when (key) {
                context.getString(R.string.pref_key_note_popup_buttons_in_book_left) -> dividerLeft
                context.getString(R.string.pref_key_note_popup_buttons_in_book_right) -> dividerRight
                context.getString(R.string.pref_key_note_popup_buttons_in_query_left) -> dividerLeft
                context.getString(R.string.pref_key_note_popup_buttons_in_query_right) -> dividerRight
                else -> throw IllegalArgumentException()
            }

            return selected + divider + others
        }

        fun setFromAll(context: Context, key: String, all: List<NotePopup.Action>) {
            val selected = all.takeWhile { it.id != R.id.note_popup_divider }
            val value = selected.joinToString(" ") { it.name }

            AppPreferences.notePopupActions(context, key, value)
        }

        private val TAG = NotePopupPreference::class.java.name
    }
}
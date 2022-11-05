package com.orgzly.android.prefs

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat

class IntegerPreferenceFragment : EditTextPreferenceDialogFragmentCompat() {
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        (view.findViewById(android.R.id.edit) as? EditText)?.setOnEditorActionListener { _, _, _ ->
            onDialogClosed(true)
            dialog?.dismiss()
            true
        }
    }

    companion object {
        val FRAGMENT_TAG: String = IntegerPreferenceFragment::class.java.name

        fun getInstance(preference: Preference): PreferenceDialogFragmentCompat {
            return IntegerPreferenceFragment().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, preference.key)
                }
            }
        }
    }
}

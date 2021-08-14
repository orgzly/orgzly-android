package com.orgzly.android.prefs

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.textfield.TextInputLayout
import com.orgzly.R
import com.orgzly.org.OrgStatesWorkflow
import com.orgzly.org.utils.ArrayListSpaceSeparated
import java.util.*

class StatesPreferenceFragment : PreferenceDialogFragmentCompat() {
    private lateinit var todoLayout: TextInputLayout
    private lateinit var todoStates: EditText

    private lateinit var doneLayout: TextInputLayout
    private lateinit var doneStates: EditText

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        todoLayout = view.findViewById(R.id.todo_states_layout)
        todoStates = view.findViewById(R.id.todo_states)

        doneLayout = view.findViewById(R.id.done_states_layout)
        doneStates = view.findViewById(R.id.done_states)

        // Force all uppercase
        todoStates.filters = todoStates.filters + InputFilter.AllCaps()
        doneStates.filters = doneStates.filters + InputFilter.AllCaps()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                validateAndUpdateButton()
            }
        }

        todoStates.addTextChangedListener(watcher)
        doneStates.addTextChangedListener(watcher)

        val value = StateWorkflows(AppPreferences.states(context))

        if (value.size > 0) {
            todoStates.setText(value[0].todoKeywords.toString())
            doneStates.setText(value[0].doneKeywords.toString())
        }

        // Validate states when dialog is displayed for the first time
        validateAndUpdateButton()
    }

    private fun validateAndUpdateButton() {
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = isValid()
    }

    /**
     * Returns `false` is there are duplicate entries.
     */
    private fun isValid(): Boolean {
        todoLayout.error = null
        doneLayout.error = null

        val keywords = HashSet<String>()

        return isUnique(keywords, todoStates.text.toString(), todoLayout)
                && isUnique(keywords, doneStates.text.toString(), doneLayout)
    }

    private fun isUnique(seenKeywords: MutableSet<String>, keywords: String, layout: TextInputLayout): Boolean {
        val context = context

        for (keyword in ArrayListSpaceSeparated(keywords)) {
            val upper = keyword.toUpperCase()

            if (seenKeywords.contains(upper)) {
                if (context != null) {
                    layout.error = context.getString(R.string.duplicate_keywords_not_allowed, upper)
                }
                return false
            }

            seenKeywords.add(upper)
        }

        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val workflow = OrgStatesWorkflow(
                    ArrayListSpaceSeparated(todoStates.text.toString()),
                    ArrayListSpaceSeparated(doneStates.text.toString())
            )

            val value = workflow.toString()
            AppPreferences.states(requireContext(), value)
            preference.summary = value
        }
    }

    companion object {
        val FRAGMENT_TAG: String = StatesPreferenceFragment::class.java.name

        fun getInstance(preference: Preference): PreferenceDialogFragmentCompat {
            val fragment = StatesPreferenceFragment()

            fragment.arguments = Bundle().apply {
                putString("key", preference.key)
            }

            return fragment
        }
    }
}

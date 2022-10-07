package com.orgzly.android.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.ui.util.KeyboardUtils

class SimpleOneLinerDialog : DialogFragment() {
    private lateinit var requestKey: String

    private var title = 0
    private var hint = 0
    private var value: String? = null
    private var positiveButtonText = 0
    private var negativeButtonText = 0
    private var userData: Bundle? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.apply {
            requestKey = getString(ARG_REQUEST_ID, "")

            title = getInt(ARG_TITLE)
            hint = getInt(ARG_HINT)
            value = getString(ARG_VALUE)
            positiveButtonText = getInt(ARG_POSITIVE_BUTTON_TEXT)
            negativeButtonText = getInt(ARG_NEGATIVE_BUTTON_TEXT)
            userData = getBundle(ARG_USER_DATA)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_simple_one_liner, null, false)

        val input = view.findViewById<View>(R.id.dialog_input) as EditText

        if (hint != 0) {
            input.setHint(hint)
        }

        if (value != null) {
            input.setText(value)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (!TextUtils.isEmpty(input.text)) {
                    val result = input.text.toString().trim { it <= ' ' }
                    parentFragmentManager.setFragmentResult(
                        requestKey, bundleOf("value" to result, "user-data" to userData))
                }

                // Closing due to used android:windowSoftInputMode="stateUnchanged"
                KeyboardUtils.closeSoftKeyboard(activity)
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                // Closing due to used android:windowSoftInputMode="stateUnchanged"
                KeyboardUtils.closeSoftKeyboard(activity)
            }
            .create()

        // Perform positive button click on keyboard's action press
        input.setOnEditorActionListener { _, _, _ ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            true
        }

        dialog.setOnShowListener {
            KeyboardUtils.openSoftKeyboard(input)
        }

        return dialog
    }

    companion object {
        private const val ARG_REQUEST_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_HINT = "hint"
        private const val ARG_VALUE = "value"
        private const val ARG_POSITIVE_BUTTON_TEXT = "pos"
        private const val ARG_NEGATIVE_BUTTON_TEXT = "neg"
        private const val ARG_USER_DATA = "bundle"

        /** Name used for [android.app.FragmentManager]. */
        @JvmField
        val FRAGMENT_TAG: String = SimpleOneLinerDialog::class.java.name

        @JvmStatic
        fun getInstance(
            requestKey: String,
            @StringRes title: Int,
            @StringRes positiveButtonText: Int,
            defaultValue: String?,
            userData: Bundle? = null
        ): SimpleOneLinerDialog {

            val bundle = Bundle().apply {
                putString(ARG_REQUEST_ID, requestKey)
                putInt(ARG_TITLE, title)
                putInt(ARG_HINT, R.string.name)
                if (defaultValue != null) {
                    putString(ARG_VALUE, defaultValue)
                }
                putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText)
                putInt(ARG_NEGATIVE_BUTTON_TEXT, R.string.cancel)
                if (userData != null) {
                    putBundle(ARG_USER_DATA, userData)
                }
            }

            return SimpleOneLinerDialog().apply {
                arguments = bundle
            }
        }
    }
}
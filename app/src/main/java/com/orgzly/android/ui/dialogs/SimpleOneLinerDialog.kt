package com.orgzly.android.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.orgzly.R
import com.orgzly.android.ui.util.ActivityUtils.closeSoftKeyboard
import com.orgzly.android.ui.util.ActivityUtils.openSoftKeyboard

class SimpleOneLinerDialog : DialogFragment() {
    private lateinit var listener: Listener

    private var dialogId = 0
    private var title = 0
    private var hint = 0
    private var value: String? = null
    private var positiveButtonText = 0
    private var negativeButtonText = 0
    private var userData: Bundle? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = requireActivity() as Listener

        arguments?.apply {
            dialogId = getInt(ARG_ID)
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

        val dialog = AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positiveButtonText) { _, _ ->
                if (!TextUtils.isEmpty(input.text)) {
                    listener.onSimpleOneLinerDialogValue(
                        dialogId,
                        input.text.toString().trim { it <= ' ' },
                        userData
                    )
                }

                // Closing due to used android:windowSoftInputMode="stateUnchanged"
                closeSoftKeyboard(activity)
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                // Closing due to used android:windowSoftInputMode="stateUnchanged"
                closeSoftKeyboard(activity)
            }
            .create()

        // Perform positive button click on keyboard's action press
        input.setOnEditorActionListener { _, _, _ ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            true
        }

        // Open keyboard when dialog is displayed
        dialog.setOnShowListener {
            openSoftKeyboard(activity, input)
        }

        return dialog
    }

    interface Listener {
        fun onSimpleOneLinerDialogValue(id: Int, value: String, bundle: Bundle?)
    }

    companion object {
        private const val ARG_ID = "id"
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
            id: Int,
            @StringRes title: Int,
            @StringRes hint: Int,
            @StringRes positiveButtonText: Int,
            @StringRes negativeButtonText: Int,
            defaultValue: String?,
            userData: Bundle?
        ): SimpleOneLinerDialog {

            val bundle = Bundle().apply {
                putInt(ARG_ID, id)
                putInt(ARG_TITLE, title)
                if (hint != 0) {
                    putInt(ARG_HINT, hint)
                }
                if (defaultValue != null) {
                    putString(ARG_VALUE, defaultValue)
                }
                putInt(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText)
                putInt(ARG_NEGATIVE_BUTTON_TEXT, negativeButtonText)
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
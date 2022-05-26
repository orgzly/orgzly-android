package com.orgzly.android.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.ui.NoteStates

object NoteStateDialog {
    @JvmStatic
    fun show(context: Context, currentState: String?, onSelection: (String) -> Unit, onClear: () -> Unit): AlertDialog {
        val states = NoteStates.fromPreferences(context)

        val currentStateIndex = if (currentState != null) states.indexOf(currentState) else -1

        return MaterialAlertDialogBuilder(context)
                .setTitle(R.string.state)
                .setSingleChoiceItems(states.array, currentStateIndex) { d, which ->
                    onSelection(states[which])
                    d.dismiss()
                }
                .setNeutralButton(R.string.clear) { _, _ ->
                    onClear()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }
}

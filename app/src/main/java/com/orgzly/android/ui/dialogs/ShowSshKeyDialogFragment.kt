package com.orgzly.android.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.git.SshKey.sshPublicKey
import com.orgzly.android.ui.SshKeygenActivity

class ShowSshKeyDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        return MaterialAlertDialogBuilder(activity).run {
            setMessage(getString(R.string.ssh_keygen_message, sshPublicKey))
            setTitle(R.string.your_public_key)
            setNegativeButton(R.string.not_now) { _, _ ->
                (activity as? SshKeygenActivity)?.finish()
            }
            setPositiveButton(R.string.ssh_keygen_share) { _, _ ->
                val sendIntent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, sshPublicKey)
                    }
                startActivity(Intent.createChooser(sendIntent, null))
                (activity as? SshKeygenActivity)?.finish()
            }
            create()
        }
    }
}

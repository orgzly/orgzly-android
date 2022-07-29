package com.orgzly.android.git

import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.ui.SshKeygenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.annotations.NonNull
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.internal.transport.sshd.OpenSshServerKeyDatabase
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import java.io.File
import java.security.KeyPair

class GitSshKeyTransportSetter: GitTransportSetter {
    private val configCallback: TransportConfigCallback
    private val activity = App.getCurrentActivity()
    private val context = App.getAppContext()

    init {
        val factory: SshSessionFactory = object : SshdSessionFactory(null, null) {

            override fun getHomeDirectory(): File { return context.filesDir }

            override fun getDefaultPreferredAuthentications(): String { return "publickey" }

            override fun createServerKeyDatabase(
                @NonNull homeDir: File,
                @NonNull sshDir: File
            ): ServerKeyDatabase {
                // We override this method because we want to set "askAboutNewFile" to False.
                return OpenSshServerKeyDatabase(
                    false,
                    getDefaultKnownHostsFiles(sshDir)
                )
            }

            override fun getDefaultKeys(@NonNull sshDir: File): Iterable<KeyPair>? {
                return if (SshKey.exists) {
                    listOf(SshKey.getKeyPair())
                } else {
                    onMissingSshKeyFile()
                    null
                }
            }
        }

        SshSessionFactory.setInstance(factory)

        // org.apache.sshd.common.config.keys.IdentityUtils freaks out if user.home is not set
        System.setProperty("user.home", context.filesDir.toString())

        configCallback = TransportConfigCallback { transport: Transport ->
            val sshTransport = transport as SshTransport
            sshTransport.sshSessionFactory = factory
        }
    }

    override fun setTransport(tc: TransportCommand<*, *>): TransportCommand<*, *> {
        tc.setTransportConfigCallback(configCallback)
        tc.setCredentialsProvider(SshCredentialsProvider())
        return tc
    }

    private fun onMissingSshKeyFile() {
        if (activity != null) {
            val builder = MaterialAlertDialogBuilder(activity)
                .setMessage(R.string.git_ssh_on_missing_key_dialog_text)
                .setTitle(R.string.git_ssh_on_missing_key_dialog_title)
            builder.setPositiveButton(activity.getString(R.string.yes)) { _, _ ->
                val intent =
                    Intent(activity.applicationContext, SshKeygenActivity::class.java)
                activity.startActivity(intent)
            }
            builder.setNegativeButton(activity.getString(R.string.not_now)) {
                    dialog, _ -> dialog.dismiss()
            }
            runBlocking(Dispatchers.Main) { activity.alertDialog = builder.show() }
        }
    }
}
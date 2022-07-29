package com.orgzly.android.ui

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.orgzly.R
import com.orgzly.android.git.SshKey
import com.orgzly.android.ui.dialogs.ShowSshKeyDialogFragment
import com.orgzly.android.util.BiometricAuthenticator
import com.orgzly.databinding.ActivitySshKeygenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private enum class KeyGenType(val generateKey: suspend (requireAuthentication: Boolean) -> Unit) {
    Rsa({ requireAuthentication ->
        SshKey.generateKeystoreNativeKey(SshKey.Algorithm.Rsa, requireAuthentication)
    }),
    Ecdsa({ requireAuthentication ->
        SshKey.generateKeystoreNativeKey(SshKey.Algorithm.Ecdsa, requireAuthentication)
    }),
    Ed25519({ requireAuthentication ->
        SshKey.generateKeystoreWrappedEd25519Key(requireAuthentication)
    }),
}

class SshKeygenActivity : CommonActivity() {

    private var keyGenType = KeyGenType.Ecdsa
    private lateinit var binding: ActivitySshKeygenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySshKeygenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        with(binding) {
            generate.setOnClickListener {
                if (SshKey.exists) {
                    MaterialAlertDialogBuilder(this@SshKeygenActivity).run {
                        setTitle(R.string.ssh_keygen_existing_title)
                        setMessage(R.string.ssh_keygen_existing_message)
                        setPositiveButton(R.string.ssh_keygen_existing_replace) { _, _ ->
                            lifecycleScope.launch { generate() }
                        }
                        setNegativeButton(R.string.ssh_keygen_existing_keep) { _, _ ->
                            setResult(RESULT_CANCELED)
                        }
                        show()
                    }
                } else {
                    lifecycleScope.launch { generate() }
                }
            }
            keyTypeGroup.check(R.id.key_type_ecdsa)
            keyTypeExplanation.setText(R.string.ssh_keygen_explanation_ecdsa)
            keyTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    keyGenType =
                        when (checkedId) {
                            R.id.key_type_ed25519 -> KeyGenType.Ed25519
                            R.id.key_type_ecdsa -> KeyGenType.Ecdsa
                            R.id.key_type_rsa -> KeyGenType.Rsa
                            else -> throw IllegalStateException("Impossible key type selection")
                        }
                    keyTypeExplanation.setText(
                        when (keyGenType) {
                            KeyGenType.Ed25519 -> R.string.ssh_keygen_explanation_ed25519
                            KeyGenType.Ecdsa -> R.string.ssh_keygen_explanation_ecdsa
                            KeyGenType.Rsa -> R.string.ssh_keygen_explanation_rsa
                        }
                    )
                }
            }
            val keyguardManager: KeyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyRequireAuthentication.isEnabled = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                false
            } else {
                keyguardManager.isDeviceSecure
            }
            keyRequireAuthentication.isChecked = keyRequireAuthentication.isEnabled
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun generate() {
        binding.generate.apply {
            text = getString(R.string.ssh_keygen_generating_progress)
            isEnabled = false
        }
        val biometricAuthenticator = BiometricAuthenticator(this)
        val result: Result<Unit> = runCatching {
            val requireAuthentication = binding.keyRequireAuthentication.isChecked
            if (requireAuthentication) {
                withContext(Dispatchers.Main) {
                    val result = biometricAuthenticator.authenticate(getString(R.string.biometric_prompt_title_ssh_keygen))
                    if (result != null)
                        throw UserNotAuthenticatedException(result)
                }
            }
            keyGenType.generateKey(requireAuthentication)
        }
        binding.generate.apply {
            text = getString(R.string.ssh_keygen_generate)
            isEnabled = true
        }
        result.fold(
            onSuccess = { ShowSshKeyDialogFragment().show(supportFragmentManager, "public_key") },
            onFailure = { e ->
                e.printStackTrace()
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.error_generate_ssh_key))
                    .setMessage(getString(R.string.ssh_key_error_dialog_text) + e.message)
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        setResult(RESULT_OK)
                    }
                    .show()
            },
        )
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService<InputMethodManager>() ?: return
        var view = currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

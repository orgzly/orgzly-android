package com.orgzly.android.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.orgzly.R
import com.orgzly.android.ui.CommonActivity
import java.io.IOException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BiometricAuthenticator(private val callingActivity: CommonActivity) {
    private lateinit var biometricManager: BiometricManager
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    suspend fun authenticate(promptText: String) : String? {

        // Initialize BiometricManager for checking biometrics availability
        biometricManager = BiometricManager.from(callingActivity)

        // Initialize PromptInfo to set title, subtitle, and authenticators of the biometric prompt
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptText)
            .setAllowedAuthenticators(authenticators)
            .build()

        if (biometricManager.canAuthenticate(authenticators) !in listOf(
                BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_STATUS_UNKNOWN
            )) {
            throw IOException(
                callingActivity.getString(R.string.biometric_auth_not_available)
            )
        }

        return suspendCoroutine { continuation ->
            // Initialize BiometricPrompt to setup success & error callbacks of biometric prompt
            executor = ContextCompat.getMainExecutor(callingActivity)
            biometricPrompt =
                BiometricPrompt(
                    callingActivity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            continuation.resume(errString.toString())
                        }
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            continuation.resume(null)
                        }
                    },
                )
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
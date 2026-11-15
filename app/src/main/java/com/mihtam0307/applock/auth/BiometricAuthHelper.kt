package com.mihtam0307.applock.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mihtam0307.applock.R

/**
 * Bọc BiometricPrompt của hệ thống để xác thực vân tay / khuôn mặt.
 * Việc xác thực sinh trắc học thật sự do hệ điều hành xử lý (so khớp mẫu
 * đã đăng ký trong Cài đặt máy), ứng dụng chỉ nhận kết quả thành công/thất bại.
 */
object BiometricAuthHelper {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                onFail()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFail()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText(activity.getString(R.string.btn_cancel))
            .build()

        prompt.authenticate(info)
    }
}

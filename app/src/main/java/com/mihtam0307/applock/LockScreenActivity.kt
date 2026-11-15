package com.mihtam0307.applock

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mihtam0307.applock.auth.BiometricAuthHelper
import com.mihtam0307.applock.auth.PrefsManager
import com.mihtam0307.applock.camera.IntruderCameraHelper
import com.mihtam0307.applock.data.AccessLogEntity
import com.mihtam0307.applock.data.AppDatabase
import com.mihtam0307.applock.ui.PatternLockView
import kotlinx.coroutines.launch

/**
 * Màn khoá hiện đè lên khi người dùng mở 1 ứng dụng đã bị khoá.
 * Tuỳ loại khoá đã chọn cho app đó mà hiện bàn phím PIN, khung vẽ hình,
 * hoặc gọi thẳng BiometricPrompt (vân tay / gương mặt) của hệ thống.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var targetPackage: String
    private lateinit var lockType: LockType
    private lateinit var appLabel: String

    private val pinBuilder = StringBuilder()
    private var firstPinLength = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)
        prefs = PrefsManager(this)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        lockType = LockType.valueOf(intent.getStringExtra(EXTRA_LOCK_TYPE) ?: LockType.NONE.name)

        appLabel = runCatching { packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(targetPackage, 0)
        ).toString() }.getOrDefault(targetPackage)

        findViewById<TextView>(R.id.tvLockAppLabel).text = appLabel
        runCatching {
            findViewById<ImageView>(R.id.ivLockAppIcon).setImageDrawable(
                packageManager.getApplicationIcon(targetPackage)
            )
        }

        when (lockType) {
            LockType.PIN -> setupPin()
            LockType.PATTERN -> setupPattern()
            LockType.FINGERPRINT, LockType.FACE -> setupBiometric()
            LockType.NONE -> finish()
        }
    }

    // ---------- PIN ----------

    private fun setupPin() {
        findViewById<LinearLayout>(R.id.pinContainer).visibility = android.view.View.VISIBLE
        val tvDots = findViewById<TextView>(R.id.tvPinDots)

        mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3", R.id.btn4 to "4",
            R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9"
        ).forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener {
                if (pinBuilder.length < firstPinLength) {
                    pinBuilder.append(digit)
                    tvDots.text = "●".repeat(pinBuilder.length)
                    if (pinBuilder.length == firstPinLength) {
                        val entered = pinBuilder.toString()
                        pinBuilder.clear()
                        tvDots.text = ""
                        if (prefs.verifyMasterPin(entered)) onAuthSuccess() else onAuthFailed()
                    }
                }
            }
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                tvDots.text = "●".repeat(pinBuilder.length)
            }
        }
    }

    // ---------- Hình ----------

    private fun setupPattern() {
        val patternView = findViewById<PatternLockView>(R.id.patternView)
        patternView.visibility = android.view.View.VISIBLE
        patternView.onPatternComplete = { pattern ->
            if (prefs.verifyMasterPattern(pattern)) onAuthSuccess() else onAuthFailed()
        }
    }

    // ---------- Sinh trắc học ----------

    private fun setupBiometric() {
        findViewById<LinearLayout>(R.id.bioContainer).visibility = android.view.View.VISIBLE
        findViewById<Button>(R.id.btnRetryBio).setOnClickListener { triggerBiometric() }
        triggerBiometric()
    }

    private fun triggerBiometric() {
        BiometricAuthHelper.authenticate(
            this,
            onSuccess = { onAuthSuccess() },
            onFail = { onAuthFailed() }
        )
    }

    // ---------- Kết quả ----------

    private fun onAuthSuccess() {
        prefs.markUnlockedForSession(targetPackage)
        logAccess(success = true, photoPath = null)
        finish()
    }

    private fun onAuthFailed() {
        findViewById<TextView>(R.id.tvLockError).visibility = android.view.View.VISIBLE
        IntruderCameraHelper.capture(this, this) { photoPath ->
            logAccess(success = false, photoPath = photoPath)
        }
    }

    private fun logAccess(success: Boolean, photoPath: String?) {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@LockScreenActivity).accessLogDao().insert(
                AccessLogEntity(
                    packageName = targetPackage,
                    appLabel = appLabel,
                    timestamp = System.currentTimeMillis(),
                    success = success,
                    photoPath = photoPath
                )
            )
        }
    }

    override fun onBackPressed() {
        // Chặn nút Back thoát khỏi màn khoá mà chưa xác thực — đưa về màn hình chính của thiết bị.
        moveTaskToBack(true)
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LOCK_TYPE = "extra_lock_type"
    }
}

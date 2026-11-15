package com.mihtam0307.applock

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
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
 *
 * Mỗi lần xác thực (dù đúng hay sai) đều được chụp 1 ảnh camera trước và
 * ghi vào lịch sử truy cập. Nhập sai PIN/Hình nhiều lần liên tiếp sẽ bị
 * khoá tạm tăng dần (chống dò mật khẩu).
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var targetPackage: String
    private lateinit var lockType: LockType
    private lateinit var appLabel: String

    private val pinBuilder = StringBuilder()
    private var firstPinLength = 6
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Chặn chụp / ghi màn hình và chặn xem trước trong Recents ngay tại màn khoá.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

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
        findViewById<LinearLayout>(R.id.pinContainer).visibility = View.VISIBLE
        val tvDots = findViewById<TextView>(R.id.tvPinDots)

        val remaining = prefs.getLockoutRemainingMillis()
        if (remaining > 0) showLockout(remaining)

        mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3", R.id.btn4 to "4",
            R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9"
        ).forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener {
                if (isLockedOut()) return@setOnClickListener
                if (pinBuilder.length < firstPinLength) {
                    pinBuilder.append(digit)
                    tvDots.text = "●".repeat(pinBuilder.length)
                    if (pinBuilder.length == firstPinLength) {
                        val entered = pinBuilder.toString()
                        pinBuilder.clear()
                        tvDots.text = ""
                        onAuthResult(prefs.verifyMasterPin(entered))
                    }
                }
            }
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (isLockedOut()) return@setOnClickListener
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                tvDots.text = "●".repeat(pinBuilder.length)
            }
        }
    }

    // ---------- Hình ----------

    private fun setupPattern() {
        val patternView = findViewById<PatternLockView>(R.id.patternView)
        patternView.visibility = View.VISIBLE

        val remaining = prefs.getLockoutRemainingMillis()
        if (remaining > 0) showLockout(remaining)

        patternView.onPatternComplete = { pattern ->
            if (!isLockedOut()) onAuthResult(prefs.verifyMasterPattern(pattern))
        }
    }

    // ---------- Sinh trắc học ----------

    private fun setupBiometric() {
        findViewById<LinearLayout>(R.id.bioContainer).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnRetryBio).setOnClickListener { triggerBiometric() }
        triggerBiometric()
    }

    private fun triggerBiometric() {
        BiometricAuthHelper.authenticate(
            this,
            onSuccess = { onAuthResult(true) },
            onFail = { onAuthResult(false) }
        )
    }

    // ---------- Khoá tạm chống dò mật khẩu ----------

    private fun isLockedOut(): Boolean = prefs.getLockoutRemainingMillis() > 0

    private fun showLockout(remainingMs: Long) {
        val tvError = findViewById<TextView>(R.id.tvLockError)
        tvError.visibility = View.VISIBLE
        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(msLeft: Long) {
                val s = (msLeft / 1000L) + 1
                tvError.text = getString(R.string.lockout_message, s)
            }
            override fun onFinish() {
                tvError.visibility = View.INVISIBLE
            }
        }.start()
    }

    // ---------- Kết quả xác thực ----------

    /**
     * success = đã nhập đúng mật khẩu/sinh trắc học hay chưa.
     * Luôn chụp ảnh camera trước (đúng lẫn sai) và ghi lịch sử truy cập.
     */
    private fun onAuthResult(success: Boolean) {
        val lockoutMs = if (!success && (lockType == LockType.PIN || lockType == LockType.PATTERN))
            prefs.registerFailedAttempt() else 0L
        if (success) prefs.resetFailedAttempts()

        IntruderCameraHelper.capture(this, this) { photoPath ->
            logAccess(success, photoPath)
            if (success) {
                prefs.markUnlockedForSession(targetPackage)
                finish()
            } else {
                findViewById<TextView>(R.id.tvLockError).visibility = View.VISIBLE
                if (lockoutMs > 0) showLockout(lockoutMs)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LOCK_TYPE = "extra_lock_type"
    }
}

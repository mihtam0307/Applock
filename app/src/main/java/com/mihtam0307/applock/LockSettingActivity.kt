package com.mihtam0307.applock

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mihtam0307.applock.auth.BiometricAuthHelper
import com.mihtam0307.applock.auth.PrefsManager
import com.mihtam0307.applock.ui.PatternLockView

class LockSettingActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var packageNameTarget: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_setting)
        prefs = PrefsManager(this)

        packageNameTarget = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val label = intent.getStringExtra(EXTRA_LABEL) ?: packageNameTarget

        findViewById<TextView>(R.id.tvAppLabel).text = label
        runCatching {
            findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(
                packageManager.getApplicationIcon(packageNameTarget)
            )
        }

        val rg = findViewById<RadioGroup>(R.id.rgLockType)
        when (prefs.getLockType(packageNameTarget)) {
            LockType.NONE -> rg.check(R.id.rbNone)
            LockType.PIN -> rg.check(R.id.rbPin)
            LockType.PATTERN -> rg.check(R.id.rbPattern)
            LockType.FINGERPRINT -> rg.check(R.id.rbFingerprint)
            LockType.FACE -> rg.check(R.id.rbFace)
        }

        findViewById<Button>(R.id.btnSaveLock).setOnClickListener {
            val selected = when (rg.checkedRadioButtonId) {
                R.id.rbPin -> LockType.PIN
                R.id.rbPattern -> LockType.PATTERN
                R.id.rbFingerprint -> LockType.FINGERPRINT
                R.id.rbFace -> LockType.FACE
                else -> LockType.NONE
            }
            applySelection(selected)
        }
    }

    private fun applySelection(type: LockType) {
        when (type) {
            LockType.NONE -> saveAndFinish(type)

            LockType.PIN -> {
                if (!prefs.hasMasterPin()) {
                    Toast.makeText(this, "Chưa thiết lập mã số chủ", Toast.LENGTH_SHORT).show()
                    return
                }
                saveAndFinish(type)
            }

            LockType.PATTERN -> {
                if (!prefs.hasMasterPattern()) {
                    showSetupPatternDialog { saveAndFinish(type) }
                } else {
                    saveAndFinish(type)
                }
            }

            LockType.FINGERPRINT, LockType.FACE -> {
                if (!BiometricAuthHelper.isAvailable(this)) {
                    Toast.makeText(
                        this,
                        "Thiết bị chưa đăng ký vân tay / khuôn mặt trong Cài đặt hệ thống",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                saveAndFinish(type)
            }
        }
    }

    private fun saveAndFinish(type: LockType) {
        prefs.setLockType(packageNameTarget, type)
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Lần đầu chọn khoá Hình: bắt vẽ 2 lần giống nhau để tạo mật khẩu hình chủ. */
    private fun showSetupPatternDialog(onDone: () -> Unit) {
        val view = PatternLockView(this)
        view.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 700
        )
        var firstPattern: String? = null

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.draw_pattern)
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(R.string.btn_cancel) { d, _ -> d.dismiss() }
            .create()

        view.onPatternComplete = { pattern ->
            if (firstPattern == null) {
                firstPattern = pattern
                dialog.setTitle(getString(R.string.confirm_pattern))
                view.reset()
            } else {
                if (firstPattern == pattern) {
                    prefs.setMasterPattern(pattern)
                    dialog.dismiss()
                    onDone()
                } else {
                    Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                    firstPattern = null
                    dialog.setTitle(getString(R.string.draw_pattern))
                    view.reset()
                }
            }
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LABEL = "extra_label"
    }
}

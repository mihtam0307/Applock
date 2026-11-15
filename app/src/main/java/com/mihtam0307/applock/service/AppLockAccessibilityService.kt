package com.mihtam0307.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mihtam0307.applock.LockScreenActivity
import com.mihtam0307.applock.LockType
import com.mihtam0307.applock.auth.PrefsManager

/**
 * Theo dõi sự kiện đổi cửa sổ (ứng dụng foreground) của hệ thống.
 * Khi phát hiện ứng dụng nằm trong danh sách bị khoá và chưa được mở khoá
 * trong phiên hiện tại, lập tức hiện màn LockScreenActivity đè lên trên.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefs: PrefsManager
    private var lastPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return // bỏ qua chính app Khoá An Toàn
        if (pkg == lastPackage) return

        // Rời khỏi app đã mở khoá trong phiên này -> yêu cầu xác thực lại lần sau
        if (lastPackage != null && lastPackage != pkg) {
            prefs.clearSession(lastPackage!!)
        }
        lastPackage = pkg

        val lockType = prefs.getLockType(pkg)
        if (lockType != LockType.NONE && !prefs.isUnlockedForSession(pkg)) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                putExtra(LockScreenActivity.EXTRA_PACKAGE, pkg)
                putExtra(LockScreenActivity.EXTRA_LOCK_TYPE, lockType.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}
}

package com.mihtam0307.applock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mihtam0307.applock.LockType
import com.mihtam0307.applock.R
import com.mihtam0307.applock.auth.PrefsManager

/**
 * Lắng nghe mọi thông báo hệ thống. Nếu thông báo đến từ 1 ứng dụng đang bị khoá
 * (ví dụ app nhắn tin) và app đó chưa được mở khoá trong phiên hiện tại, thông báo
 * gốc sẽ bị gỡ ngay và thay bằng 1 thông báo chung chung "Có tin nhắn mới - Đã khoá",
 * để nội dung tin nhắn không bị lộ ra ngoài màn khoá / thanh thông báo.
 */
class NotificationGuardService : NotificationListenerService() {

    private lateinit var prefs: PrefsManager

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(applicationContext)
        createChannelIfNeeded()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName) return

        val lockType = prefs.getLockType(pkg)
        if (lockType == LockType.NONE || prefs.isUnlockedForSession(pkg)) return

        // Gỡ thông báo gốc (có thể chứa nội dung tin nhắn) khỏi thanh thông báo
        cancelNotification(sbn.key)

        val manager = getSystemService(NotificationManager::class.java)
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sbn.packageName)
            .setContentText("Có thông báo mới - đã khoá")
            .setAutoCancel(true)
            .build()
        manager?.notify(pkg.hashCode(), notif)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Thông báo đã khoá", NotificationManager.IMPORTANCE_DEFAULT
            )
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "locked_notifications"
    }
}

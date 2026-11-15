package com.mihtam0307.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dịch vụ Accessibility và NotificationListener do hệ thống tự khởi động lại
 * sau khi khởi động máy (nếu người dùng đã cấp quyền trước đó).
 * Receiver này chỉ giữ chỗ để mở rộng về sau (ví dụ kiểm tra lại trạng thái quyền).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Không cần xử lý gì thêm ở bản hiện tại.
    }
}

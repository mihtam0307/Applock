package com.mihtam0307.applock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Khi được kích hoạt làm Device Admin, Android sẽ không cho gỡ cài đặt app
 * này theo cách thông thường (nút Gỡ cài đặt / App info) cho tới khi quyền
 * Device Admin bị tắt trước — việc tắt quyền này cũng được chặn lại bằng màn
 * xác thực trong SettingsActivity/MainActivity trước khi gọi removeActiveAdmin().
 *
 * Lưu ý giới hạn thực tế: đây là mức bảo vệ chuẩn của Android dành cho app
 * thường. Người dùng vẫn có thể vào Cài đặt hệ thống > Ứng dụng quản trị thiết bị
 * để tắt quyền trực tiếp qua hệ thống (ngoài tầm kiểm soát của app) — Android chỉ
 * cho một app chặn hoàn toàn việc này nếu được cấp quyền "Device Owner" cấp cao
 * hơn, phải thiết lập qua ADB lúc cài máy, không áp dụng được cho app cài thông
 * thường.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Đã bật chống gỡ cài đặt", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Đã tắt chống gỡ cài đặt", Toast.LENGTH_SHORT).show()
    }
}

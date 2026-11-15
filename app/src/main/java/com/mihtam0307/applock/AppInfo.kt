package com.mihtam0307.applock

import android.graphics.drawable.Drawable

/**
 * Thông tin hiển thị của 1 ứng dụng đã cài trên máy, kèm trạng thái khoá hiện tại.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    var lockType: LockType = LockType.NONE
)

/** Các loại khoá mà một ứng dụng có thể dùng. */
enum class LockType {
    NONE, PIN, PATTERN, FINGERPRINT, FACE
}

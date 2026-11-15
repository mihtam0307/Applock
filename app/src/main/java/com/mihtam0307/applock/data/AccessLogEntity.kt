package com.mihtam0307.applock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1 dòng lịch sử truy cập: ứng dụng nào, lúc nào, có mở thành công không,
 * và đường dẫn ảnh chụp người thao tác (nếu nhập sai mật khẩu).
 */
@Entity(tableName = "access_log")
data class AccessLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val timestamp: Long,
    val success: Boolean,
    val photoPath: String? = null
)

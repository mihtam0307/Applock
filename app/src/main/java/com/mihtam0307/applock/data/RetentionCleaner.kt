package com.mihtam0307.applock.data

import android.content.Context
import com.mihtam0307.applock.camera.IntruderCameraHelper
import java.util.concurrent.TimeUnit

/**
 * Dọn ảnh chụp + dòng lịch sử truy cập cũ hơn N ngày (mặc định 30 ngày),
 * đúng theo yêu cầu "chỉ lưu ảnh tối đa trong 30 ngày". Gọi hàm cleanup()
 * 1 lần mỗi khi mở app (ví dụ trong MainActivity.onCreate).
 */
object RetentionCleaner {

    private const val DEFAULT_MAX_AGE_DAYS = 30L

    suspend fun cleanup(context: Context, maxAgeDays: Long = DEFAULT_MAX_AGE_DAYS) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays)
        val dao = AppDatabase.getInstance(context).accessLogDao()
        val oldPhotoPaths = dao.getPhotoPathsOlderThan(cutoff)
        oldPhotoPaths.forEach { path -> IntruderCameraHelper.deletePhoto(path) }
        dao.deleteOlderThan(cutoff)
    }
}

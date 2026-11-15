package com.mihtam0307.applock.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mihtam0307.applock.LockType
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Lưu mật khẩu chủ (PIN / hình) và danh sách ứng dụng bị khoá.
 * Dữ liệu được lưu trong EncryptedSharedPreferences (mã hoá bằng Android Keystore),
 * mật khẩu được băm bằng PBKDF2WithHmacSHA256 (120.000 vòng) + salt ngẫu nhiên,
 * không bao giờ lưu dạng gốc. So khớp hash dùng MessageDigest.isEqual (hằng thời gian)
 * để tránh bị dò qua chênh lệch thời gian xử lý.
 * Có thêm cơ chế khoá tạm (lockout) tăng dần sau nhiều lần nhập sai liên tiếp.
 */
class PrefsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "applock_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---------- Băm mật khẩu (PBKDF2) ----------

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(KEY_SALT, null)
        if (salt == null) {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            salt = bytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY_SALT, salt).apply()
        }
        return salt
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }

    private fun hash(value: String): String {
        val saltBytes = hexToBytes(getOrCreateSalt())
        val spec = PBEKeySpec(value.toCharArray(), saltBytes, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /** So sánh 2 chuỗi hash theo kiểu hằng thời gian, tránh timing attack. */
    private fun constantTimeEquals(a: String?, b: String): Boolean {
        if (a == null) return false
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    // ---------- Mật khẩu chủ ----------

    fun isOnboarded(): Boolean = prefs.contains(KEY_MASTER_PIN) || prefs.contains(KEY_MASTER_PATTERN)

    fun setMasterPin(pin: String) {
        prefs.edit().putString(KEY_MASTER_PIN, hash(pin)).apply()
    }

    fun hasMasterPin(): Boolean = prefs.contains(KEY_MASTER_PIN)

    fun verifyMasterPin(pin: String): Boolean =
        constantTimeEquals(prefs.getString(KEY_MASTER_PIN, null), hash(pin))

    fun setMasterPattern(pattern: String) {
        prefs.edit().putString(KEY_MASTER_PATTERN, hash(pattern)).apply()
    }

    fun hasMasterPattern(): Boolean = prefs.contains(KEY_MASTER_PATTERN)

    fun verifyMasterPattern(pattern: String): Boolean =
        constantTimeEquals(prefs.getString(KEY_MASTER_PATTERN, null), hash(pattern))

    // ---------- Khoá tạm sau nhiều lần nhập sai (chống dò mật khẩu) ----------

    /**
     * Gọi khi người dùng nhập sai PIN/Hình. Tăng bộ đếm và, nếu đã vượt
     * ngưỡng, đặt thời điểm hết khoá tạm. Trả về số mili-giây bị khoá
     * (0 nếu lần này chưa bị khoá).
     */
    fun registerFailedAttempt(): Long {
        val count = prefs.getInt(KEY_FAILED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAILED_COUNT, count).apply()
        val idx = (count - 1).coerceAtMost(LOCKOUT_SCHEDULE_MS.size - 1)
        val lockMs = LOCKOUT_SCHEDULE_MS[idx]
        if (lockMs > 0) {
            prefs.edit().putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockMs).apply()
        }
        return lockMs
    }

    fun resetFailedAttempts() {
        prefs.edit().remove(KEY_FAILED_COUNT).remove(KEY_LOCKOUT_UNTIL).apply()
    }

    /** Số mili-giây còn lại đang bị khoá tạm (0 nếu không bị khoá). */
    fun getLockoutRemainingMillis(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remain = until - System.currentTimeMillis()
        return if (remain > 0) remain else 0L
    }

    // ---------- Danh sách ứng dụng bị khoá ----------

    fun getLockType(packageName: String): LockType {
        val name = prefs.getString(PREFIX_APP + packageName, LockType.NONE.name) ?: LockType.NONE.name
        return runCatching { LockType.valueOf(name) }.getOrDefault(LockType.NONE)
    }

    fun setLockType(packageName: String, type: LockType) {
        if (type == LockType.NONE) {
            prefs.edit().remove(PREFIX_APP + packageName).apply()
        } else {
            prefs.edit().putString(PREFIX_APP + packageName, type.name).apply()
        }
    }

    fun getAllLockedPackages(): Set<String> =
        prefs.all.keys
            .filter { it.startsWith(PREFIX_APP) }
            .map { it.removePrefix(PREFIX_APP) }
            .toSet()

    // ---------- Phiên đã mở khoá (để không hỏi lại liên tục trong cùng phiên) ----------

    fun markUnlockedForSession(packageName: String) {
        unlockedThisSession.add(packageName)
    }

    fun isUnlockedForSession(packageName: String): Boolean = unlockedThisSession.contains(packageName)

    fun clearSession(packageName: String) {
        unlockedThisSession.remove(packageName)
    }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_MASTER_PIN = "master_pin"
        private const val KEY_MASTER_PATTERN = "master_pattern"
        private const val KEY_FAILED_COUNT = "failed_count"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val PREFIX_APP = "app_lock_"
        private const val PBKDF2_ITERATIONS = 120_000

        // 4 lần sai đầu: chưa khoá. Từ lần thứ 5 trở đi: khoá tăng dần.
        private val LOCKOUT_SCHEDULE_MS = longArrayOf(
            0, 0, 0, 0,
            30_000L,        // lần sai thứ 5
            60_000L,        // lần sai thứ 6
            5 * 60_000L,    // lần sai thứ 7
            15 * 60_000L    // lần sai thứ 8 trở đi (mức trần)
        )

        // Bộ nhớ tạm trong RAM (mất khi tắt app) đánh dấu app nào vừa được mở khoá,
        // tránh bắt nhập lại mật khẩu mỗi lần chuyển qua chuyển lại trong lúc đang dùng.
        private val unlockedThisSession = mutableSetOf<String>()
    }
}

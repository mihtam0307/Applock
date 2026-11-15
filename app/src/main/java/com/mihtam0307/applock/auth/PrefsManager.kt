package com.mihtam0307.applock.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mihtam0307.applock.LockType
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Lưu mật khẩu chủ (PIN / hình) và danh sách ứng dụng bị khoá.
 * Dữ liệu được lưu trong EncryptedSharedPreferences (mã hoá bằng Android Keystore),
 * mật khẩu được băm (SHA-256 + salt) trước khi lưu, không bao giờ lưu dạng gốc.
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

    // ---------- Băm mật khẩu ----------

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

    private fun hash(value: String): String {
        val salt = getOrCreateSalt()
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + value).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ---------- Mật khẩu chủ ----------

    fun isOnboarded(): Boolean = prefs.contains(KEY_MASTER_PIN) || prefs.contains(KEY_MASTER_PATTERN)

    fun setMasterPin(pin: String) {
        prefs.edit().putString(KEY_MASTER_PIN, hash(pin)).apply()
    }

    fun hasMasterPin(): Boolean = prefs.contains(KEY_MASTER_PIN)

    fun verifyMasterPin(pin: String): Boolean = prefs.getString(KEY_MASTER_PIN, null) == hash(pin)

    fun setMasterPattern(pattern: String) {
        prefs.edit().putString(KEY_MASTER_PATTERN, hash(pattern)).apply()
    }

    fun hasMasterPattern(): Boolean = prefs.contains(KEY_MASTER_PATTERN)

    fun verifyMasterPattern(pattern: String): Boolean =
        prefs.getString(KEY_MASTER_PATTERN, null) == hash(pattern)

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
        private const val PREFIX_APP = "app_lock_"

        // Bộ nhớ tạm trong RAM (mất khi tắt app) đánh dấu app nào vừa được mở khoá,
        // tránh bắt nhập lại mật khẩu mỗi lần chuyển qua chuyển lại trong lúc đang dùng.
        private val unlockedThisSession = mutableSetOf<String>()
    }
}

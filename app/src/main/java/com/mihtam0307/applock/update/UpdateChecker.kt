package com.mihtam0307.applock.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Kiểm tra bản phát hành mới nhất trên GitHub Releases của repo, so với
 * versionName hiện tại của app. Chạy trên luồng nền, âm thầm bỏ qua nếu
 * không có mạng. Gọi callback trên luồng chính khi có bản mới hơn.
 *
 * ĐỔI "REPO" bên dưới thành đúng "tài_khoản/tên_repo" GitHub của bạn nếu khác.
 */
object UpdateChecker {

    private const val REPO = "mihtam0307/AppLock"
    private const val TIMEOUT_MS = 8000

    fun checkForUpdate(context: Context, onUpdateAvailable: (versionTag: String, releaseUrl: String) -> Unit) {
        Thread {
            runCatching {
                val url = URL("https://api.github.com/repos/$REPO/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name").removePrefix("v")
                    val releaseUrl = json.optString("html_url")
                    val currentVersion = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName ?: "0"
                    if (tag.isNotEmpty() && isNewer(tag, currentVersion)) {
                        Handler(Looper.getMainLooper()).post { onUpdateAvailable(tag, releaseUrl) }
                    }
                }
                conn.disconnect()
            }
        }.start()
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}

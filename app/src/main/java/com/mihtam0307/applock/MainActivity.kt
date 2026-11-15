package com.mihtam0307.applock

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mihtam0307.applock.auth.PrefsManager
import com.mihtam0307.applock.data.RetentionCleaner
import com.mihtam0307.applock.location.DeviceLocationActivity
import com.mihtam0307.applock.update.UpdateChecker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PrefsManager(this)
        if (!prefs.isOnboarded()) {
            startActivity(Intent(this, SetupMasterActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, com.mihtam0307.applock.history.HistoryActivity::class.java))
        }
        findViewById<Button>(R.id.btnDeviceLocation).setOnClickListener {
            startActivity(Intent(this, DeviceLocationActivity::class.java))
        }

        // Dọn ảnh + dòng lịch sử cũ hơn 30 ngày mỗi khi mở app.
        lifecycleScope.launch { RetentionCleaner.cleanup(this@MainActivity) }

        loadAppList()
        checkPermissions()
        checkForUpdateIfDue()
    }

    /** Kiểm tra bản cập nhật mới trên GitHub, tối đa 1 lần / 24 giờ để tránh gọi mạng liên tục. */
    private fun checkForUpdateIfDue() {
        val updatePrefs = getSharedPreferences("update_check", MODE_PRIVATE)
        val lastCheck = updatePrefs.getLong("last_check", 0L)
        if (System.currentTimeMillis() - lastCheck < 24L * 60 * 60 * 1000) return
        updatePrefs.edit().putLong("last_check", System.currentTimeMillis()).apply()

        UpdateChecker.checkForUpdate(this) { versionTag, releaseUrl ->
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.update_available_title)
                    .setMessage(getString(R.string.update_available_message, versionTag))
                    .setPositiveButton(R.string.btn_update_now) { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                    }
                    .setNegativeButton(R.string.btn_update_later, null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        loadAppList() // cập nhật lại nhãn khoá sau khi quay lại từ màn thiết lập
    }

    private fun loadAppList() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val apps = resolved
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val pkg = it.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm),
                    lockType = prefs.getLockType(pkg)
                )
            }
            .sortedBy { it.label.lowercase() }

        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = AppListAdapter(apps) { app ->
                val i = Intent(this@MainActivity, LockSettingActivity::class.java)
                i.putExtra(LockSettingActivity.EXTRA_PACKAGE, app.packageName)
                i.putExtra(LockSettingActivity.EXTRA_LABEL, app.label)
                startActivity(i)
            }
        }
    }

    /** Kiểm tra quyền Accessibility (bắt buộc để phát hiện app đang mở) và Notification Listener. */
    private fun checkPermissions() {
        val banner = findViewById<LinearLayout>(R.id.permissionBanner)
        val missing = mutableListOf<String>()
        if (!isAccessibilityEnabled()) missing.add("Trợ năng (Accessibility)")
        if (!isNotificationListenerEnabled()) missing.add("Truy cập thông báo")

        if (missing.isEmpty()) {
            banner.visibility = android.view.View.GONE
            return
        }
        banner.visibility = android.view.View.VISIBLE
        findViewById<TextView>(R.id.tvPermissionWarning).text =
            "Cần cấp quyền: " + missing.joinToString(", ") + " để khoá app hoạt động đầy đủ."

        findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.contains("$packageName/${packageName}.service.AppLockAccessibilityService")
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }
}

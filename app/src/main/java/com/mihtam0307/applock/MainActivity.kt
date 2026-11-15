package com.mihtam0307.applock

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mihtam0307.applock.auth.PrefsManager

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

        loadAppList()
        checkPermissions()
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

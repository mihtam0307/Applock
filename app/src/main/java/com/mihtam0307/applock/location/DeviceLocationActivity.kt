package com.mihtam0307.applock.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mihtam0307.applock.R

/**
 * Xem vị trí hiện tại của CHÍNH thiết bị đang chạy app (giống "định vị thiết
 * bị" kiểu chống trộm, nhưng chỉ xem được khi đang cầm máy này trong tay —
 * không gửi/điều khiển vị trí tới máy nào khác).
 */
class DeviceLocationActivity : AppCompatActivity(), LocationListener {

    private lateinit var tvStatus: TextView
    private var lastLocation: Location? = null
    private lateinit var locationManager: LocationManager

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startLocating() else showDenied() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_location)
        title = getString(R.string.title_device_location)

        tvStatus = findViewById(R.id.tvLocationStatus)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        findViewById<Button>(R.id.btnRefreshLocation).setOnClickListener { ensurePermissionThenLocate() }
        findViewById<Button>(R.id.btnOpenInMaps).setOnClickListener {
            lastLocation?.let {
                val uri = Uri.parse("geo:${it.latitude},${it.longitude}?q=${it.latitude},${it.longitude}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        ensurePermissionThenLocate()
    }

    private fun ensurePermissionThenLocate() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startLocating() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startLocating() {
        tvStatus.text = getString(R.string.location_fetching)
        findViewById<Button>(R.id.btnOpenInMaps).isEnabled = false
        runCatching {
            val providers = locationManager.getProviders(true)
            val best = providers.mapNotNull { locationManager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
            if (best != null) onLocationChanged(best)
            for (provider in providers) {
                locationManager.requestLocationUpdates(provider, 0L, 0f, this, Looper.getMainLooper())
            }
        }.onFailure { tvStatus.text = getString(R.string.location_unavailable) }
    }

    private fun showDenied() {
        tvStatus.text = getString(R.string.location_permission_denied)
    }

    override fun onLocationChanged(location: Location) {
        lastLocation = location
        tvStatus.text = "Vĩ độ: ${location.latitude}\nKinh độ: ${location.longitude}\n" +
            "Độ chính xác: ~${location.accuracy.toInt()} m"
        findViewById<Button>(R.id.btnOpenInMaps).isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { locationManager.removeUpdates(this) }
    }
}

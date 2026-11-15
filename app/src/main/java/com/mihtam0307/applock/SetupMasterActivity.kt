package com.mihtam0307.applock

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mihtam0307.applock.auth.PrefsManager

/** Màn hình đầu tiên: bắt người dùng đặt mã số chủ trước khi khoá bất kỳ app nào. */
class SetupMasterActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_master)
        prefs = PrefsManager(this)

        val etPin = findViewById<EditText>(R.id.etMasterPin)
        val etConfirm = findViewById<EditText>(R.id.etMasterPinConfirm)
        val tvError = findViewById<TextView>(R.id.tvSetupError)
        val btnSave = findViewById<Button>(R.id.btnSetupSave)

        btnSave.setOnClickListener {
            val pin = etPin.text.toString()
            val confirm = etConfirm.text.toString()
            if (pin.length != 6) {
                tvError.text = "Mã số cần đúng 6 số"
                tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }
            if (pin != confirm) {
                tvError.text = getString(R.string.wrong_password)
                tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }
            prefs.setMasterPin(pin)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}

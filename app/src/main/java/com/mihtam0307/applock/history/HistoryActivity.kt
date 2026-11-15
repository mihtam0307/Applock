package com.mihtam0307.applock.history

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mihtam0307.applock.R
import com.mihtam0307.applock.data.AppDatabase
import kotlinx.coroutines.launch

/** Liệt kê toàn bộ lượt truy cập (thành công & thất bại) vào các ứng dụng đã khoá. */
class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvHistoryEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val logs = AppDatabase.getInstance(this@HistoryActivity).accessLogDao().getAll()
            if (logs.isEmpty()) {
                tvEmpty.visibility = android.view.View.VISIBLE
                rv.visibility = android.view.View.GONE
            } else {
                rv.adapter = HistoryAdapter(logs)
            }
        }
    }
}

package com.mihtam0307.applock.history

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mihtam0307.applock.R
import com.mihtam0307.applock.camera.IntruderCameraHelper
import com.mihtam0307.applock.data.AccessLogEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TYPE_HEADER = 0
private const val TYPE_ENTRY = 1

/**
 * Hiển thị lịch sử truy cập, gộp theo từng ngày (tiêu đề "Hôm nay", "Hôm qua"
 * hoặc dd/MM/yyyy) để người dùng xem lại theo từng ngày dễ hơn.
 */
class HistoryAdapter(logs: List<AccessLogEntity>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val items: List<HistoryListItem> = buildGroupedList(logs)

    private fun dayLabel(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        fun sameDay(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            sameDay(cal, today) -> "Hôm nay"
            sameDay(cal, yesterday) -> "Hôm qua"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun buildGroupedList(logs: List<AccessLogEntity>): List<HistoryListItem> {
        val result = mutableListOf<HistoryListItem>()
        var lastLabel: String? = null
        for (log in logs) {
            val label = dayLabel(log.timestamp)
            if (label != lastLabel) {
                result.add(HistoryListItem.DateHeader(label))
                lastLabel = label
            }
            result.add(HistoryListItem.Entry(log))
        }
        return result
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is HistoryListItem.DateHeader -> TYPE_HEADER
            is HistoryListItem.Entry -> TYPE_ENTRY
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_history_header, parent, false))
        } else {
            EntryVH(inflater.inflate(R.layout.item_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryListItem.DateHeader -> (holder as HeaderVH).bind(item.dateLabel)
            is HistoryListItem.Entry -> (holder as EntryVH).bind(item.log, timeFmt)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvHistoryHeaderDate)
        fun bind(label: String) { tv.text = label }
    }

    class EntryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val photo: ImageView = view.findViewById(R.id.ivHistoryPhoto)
        private val app: TextView = view.findViewById(R.id.tvHistoryApp)
        private val time: TextView = view.findViewById(R.id.tvHistoryTime)
        private val result: TextView = view.findViewById(R.id.tvHistoryResult)

        fun bind(log: AccessLogEntity, timeFmt: SimpleDateFormat) {
            app.text = log.appLabel
            time.text = timeFmt.format(Date(log.timestamp))

            if (log.success) {
                result.text = itemView.context.getString(R.string.history_success)
                result.setTextColor(ContextCompat.getColor(itemView.context, R.color.success))
            } else {
                result.text = itemView.context.getString(R.string.history_failed)
                result.setTextColor(ContextCompat.getColor(itemView.context, R.color.danger))
            }

            if (!log.photoPath.isNullOrEmpty()) {
                val bitmap = runCatching {
                    IntruderCameraHelper.openDecryptedStream(itemView.context, log.photoPath).use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
                if (bitmap != null) {
                    photo.setImageBitmap(bitmap)
                    photo.visibility = View.VISIBLE
                } else {
                    photo.visibility = View.GONE
                }
            } else {
                photo.visibility = View.GONE
            }
        }
    }
}

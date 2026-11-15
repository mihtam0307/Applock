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
import com.mihtam0307.applock.data.AccessLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val items: List<AccessLogEntity>) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val fmt = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.ivHistoryPhoto)
        val app: TextView = view.findViewById(R.id.tvHistoryApp)
        val time: TextView = view.findViewById(R.id.tvHistoryTime)
        val result: TextView = view.findViewById(R.id.tvHistoryResult)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val log = items[position]
        holder.app.text = log.appLabel
        holder.time.text = fmt.format(Date(log.timestamp))

        if (log.success) {
            holder.result.text = holder.itemView.context.getString(R.string.history_success)
            holder.result.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.success))
        } else {
            holder.result.text = holder.itemView.context.getString(R.string.history_failed)
            holder.result.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.danger))
        }

        if (!log.photoPath.isNullOrEmpty()) {
            val bitmap = runCatching { BitmapFactory.decodeFile(log.photoPath) }.getOrNull()
            if (bitmap != null) {
                holder.photo.setImageBitmap(bitmap)
                holder.photo.visibility = View.VISIBLE
            } else {
                holder.photo.visibility = View.GONE
            }
        } else {
            holder.photo.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size
}

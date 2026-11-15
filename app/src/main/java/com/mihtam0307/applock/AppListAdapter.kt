package com.mihtam0307.applock

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivAppIcon)
        val label: TextView = view.findViewById(R.id.tvAppLabel)
        val badge: TextView = view.findViewById(R.id.tvLockBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.badge.text = holder.itemView.context.getString(labelFor(app.lockType))
        holder.itemView.setOnClickListener { onClick(app) }
    }

    private fun labelFor(type: LockType): Int = when (type) {
        LockType.NONE -> R.string.lock_type_none
        LockType.PIN -> R.string.lock_type_pin
        LockType.PATTERN -> R.string.lock_type_pattern
        LockType.FINGERPRINT -> R.string.lock_type_fingerprint
        LockType.FACE -> R.string.lock_type_face
    }

    override fun getItemCount(): Int = items.size
}

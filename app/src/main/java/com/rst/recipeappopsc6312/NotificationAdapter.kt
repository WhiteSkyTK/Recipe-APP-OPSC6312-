package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(private var notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    // This class holds the views for each individual item in the list.
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imageViewNotificationIcon)
        val title: TextView = itemView.findViewById(R.id.textViewNotificationTitle)
        val timestamp: TextView = itemView.findViewById(R.id.textViewNotificationTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // This inflates the item_notification.xml layout for each row.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        // ++ FIX 1: Convert the icon name (String) to a drawable resource ID (Int) ++
        val iconResId = holder.itemView.context.resources.getIdentifier(
            notification.iconName, "drawable", holder.itemView.context.packageName
        )
        // Use a fallback icon if the name doesn't match any drawable
        holder.icon.setImageResource(if (iconResId != 0) iconResId else R.drawable.ic_alert)

        holder.title.text = notification.title

        // ++ FIX 2: Format the Date object into a readable String ++
        holder.timestamp.text = if (notification.timestamp != null) {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.format(notification.timestamp)
        } else {
            "Just now"
        }
    }

    override fun getItemCount(): Int {
        // This tells the RecyclerView how many items are in the list.
        return notifications.size
    }

    fun updateData(newNotifications: List<Notification>) {
        this.notifications = newNotifications
        notifyDataSetChanged()
    }
}
package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notifications: List<Notification>) :
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
        // This binds the data from your list to the views in the ViewHolder.
        val notification = notifications[position]
        holder.icon.setImageResource(notification.iconResId)
        holder.title.text = notification.title
        holder.timestamp.text = notification.timestamp
    }

    override fun getItemCount(): Int {
        // This tells the RecyclerView how many items are in the list.
        return notifications.size
    }
}
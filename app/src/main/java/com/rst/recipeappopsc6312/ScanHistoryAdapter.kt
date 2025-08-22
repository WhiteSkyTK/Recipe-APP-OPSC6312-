package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class ScanHistoryAdapter(
    private var historyList: List<ScanHistoryItem>,
    private val onClick: (ScanHistoryItem) -> Unit
) : RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageViewHistory)
        val title: TextView = itemView.findViewById(R.id.textViewHistoryTitle)
        val timestamp: TextView = itemView.findViewById(R.id.textViewHistoryTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.title.text = item.title

        // Format the Date object into a readable string
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        holder.timestamp.text = formatter.format(item.timestamp)

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.placeholder_image) // A default placeholder
            .error(R.drawable.placeholder_image)       // An image to show on error
            .into(holder.image)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = historyList.size

    fun updateHistory(newHistory: List<ScanHistoryItem>) {
        this.historyList = newHistory
        notifyDataSetChanged()
    }
}
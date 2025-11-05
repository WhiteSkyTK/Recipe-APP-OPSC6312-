package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BadgesAdapter(private var badges: List<BadgeDisplayInfo>) : RecyclerView.Adapter<BadgesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imageViewBadgeIcon)
        val title: TextView = itemView.findViewById(R.id.textViewBadgeTitle)
        val description: TextView = itemView.findViewById(R.id.textViewBadgeDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val badgeInfo = badges[position]
        holder.title.text = badgeInfo.badge.title
        holder.description.text = badgeInfo.badge.description

        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(badgeInfo.badge.iconName, "drawable", context.packageName)
        if (resId != 0) {
            holder.icon.setImageResource(resId)
        }

        // If the badge is earned, make it fully visible. Otherwise, make it look disabled.
        if (badgeInfo.isEarned) {
            holder.itemView.alpha = 1.0f
            holder.icon.alpha = 1.0f
        } else {
            holder.itemView.alpha = 0.5f
            holder.icon.alpha = 0.5f
        }
    }

    override fun getItemCount() = badges.size

    fun updateData(newBadges: List<BadgeDisplayInfo>) {
        this.badges = newBadges
        notifyDataSetChanged()
    }
}


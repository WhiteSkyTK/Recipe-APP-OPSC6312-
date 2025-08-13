package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DietAdapter(private val onClick: (Diet) -> Unit) :
    ListAdapter<Diet, DietAdapter.DietViewHolder>(DietDiffCallback) {

    inner class DietViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.imageViewDiet)
        private val name: TextView = itemView.findViewById(R.id.textViewDietName)

        fun bind(diet: Diet) {
            name.text = diet.name
            itemView.isSelected = diet.isSelected
            Glide.with(itemView.context)
                .load(diet.imageResId)
                .override(100, 100) // Resize for performance
                .into(image)

            itemView.setOnClickListener {
                onClick(diet)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diet, parent, false)
        return DietViewHolder(view)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// DiffUtil object to calculate list changes efficiently
object DietDiffCallback : DiffUtil.ItemCallback<Diet>() {
    override fun areItemsTheSame(oldItem: Diet, newItem: Diet): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Diet, newItem: Diet): Boolean {
        return oldItem == newItem
    }
}
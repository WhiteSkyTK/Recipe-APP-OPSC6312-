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

class CuisineAdapter(private val onClick: (Cuisine) -> Unit) :
    ListAdapter<Cuisine, CuisineAdapter.CuisineViewHolder>(CuisineDiffCallback) {

    inner class CuisineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.imageViewCuisine)
        private val name: TextView = itemView.findViewById(R.id.textViewCuisineName)

        fun bind(cuisine: Cuisine) {
            name.text = cuisine.name
            itemView.isSelected = cuisine.isSelected
            Glide.with(itemView.context)
                .load(cuisine.imageResId)
                .override(100, 100)
                .into(image)

            itemView.setOnClickListener {
                onClick(cuisine)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuisineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuisine, parent, false)
        return CuisineViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuisineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// This object tells the ListAdapter how to calculate changes efficiently.
object CuisineDiffCallback : DiffUtil.ItemCallback<Cuisine>() {
    override fun areItemsTheSame(oldItem: Cuisine, newItem: Cuisine): Boolean {
        return oldItem.name == newItem.name // Use a unique ID if you have one
    }

    override fun areContentsTheSame(oldItem: Cuisine, newItem: Cuisine): Boolean {
        return oldItem == newItem
    }
}

package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CuisineAdapter(private val cuisineList: List<Cuisine>) :
    RecyclerView.Adapter<CuisineAdapter.CuisineViewHolder>() {

    inner class CuisineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageViewCuisine)
        val name: TextView = itemView.findViewById(R.id.textViewCuisineName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position)
                }
            }
        }
    }

    private fun toggleSelection(position: Int) {
        val cuisine = cuisineList[position]
        cuisine.isSelected = !cuisine.isSelected
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuisineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuisine, parent, false)
        return CuisineViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuisineViewHolder, position: Int) {
        val cuisine = cuisineList[position]
        holder.image.setImageResource(cuisine.imageResId)
        holder.name.text = cuisine.name
        holder.itemView.isSelected = cuisine.isSelected

        Glide.with(holder.itemView.context)
            .load(cuisine.imageResId)
            .override(100, 100) // Resize to 100x100 pixels
            .into(holder.image)
    }

    override fun getItemCount(): Int = cuisineList.size

    fun getSelectedCuisines(): List<Cuisine> {
        return cuisineList.filter { it.isSelected }
    }

    fun selectAll(select: Boolean) {
        for (cuisine in cuisineList) {
            cuisine.isSelected = select
        }
        notifyDataSetChanged()
    }
}
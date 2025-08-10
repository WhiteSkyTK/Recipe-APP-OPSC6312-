package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CuisineAdapter(private val cuisineList: List<Cuisine>) :
    RecyclerView.Adapter<CuisineAdapter.CuisineViewHolder>() {

    inner class CuisineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageViewCuisine)
        val name: TextView = itemView.findViewById(R.id.textViewCuisineName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val cuisine = cuisineList[position]
                    cuisine.isSelected = !cuisine.isSelected
                    itemView.isSelected = cuisine.isSelected
                }
            }
        }
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
    }

    override fun getItemCount(): Int = cuisineList.size

    fun getSelectedCuisines(): List<Cuisine> {
        return cuisineList.filter { it.isSelected }
    }
}
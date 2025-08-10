package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DietAdapter(private val dietList: List<Diet>) :
    RecyclerView.Adapter<DietAdapter.DietViewHolder>() {

    inner class DietViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imageViewDiet)
        val name: TextView = itemView.findViewById(R.id.textViewDietName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val diet = dietList[position]
                    diet.isSelected = !diet.isSelected
                    itemView.isSelected = diet.isSelected
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diet, parent, false)
        return DietViewHolder(view)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        val diet = dietList[position]
        holder.image.setImageResource(diet.imageResId)
        holder.name.text = diet.name
        holder.itemView.isSelected = diet.isSelected
    }

    override fun getItemCount(): Int = dietList.size
}
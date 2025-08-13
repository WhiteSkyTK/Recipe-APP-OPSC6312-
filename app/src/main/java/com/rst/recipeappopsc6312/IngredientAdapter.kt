// 1. THE COMPLETE IngredientAdapter.kt
// This adapter is responsible for displaying the list of ingredients.

package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IngredientAdapter(private var ingredientList: List<Ingredient>) :
    RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewIngredientName)
        val quantityTextView: TextView = itemView.findViewById(R.id.textViewIngredientQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]
        holder.nameTextView.text = ingredient.name
        holder.quantityTextView.text = ingredient.quantity
    }

    override fun getItemCount(): Int = ingredientList.size

    fun updateIngredients(newIngredients: List<Ingredient>) {
        this.ingredientList = newIngredients
        notifyDataSetChanged()
    }
}
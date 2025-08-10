package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class CategoryAdapter(private val categoryList: List<Category>) :
    RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false) as Chip
        return ViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoryList[position]
        holder.chip.text = category.name
        holder.chip.isChecked = category.isSelected

        holder.chip.setOnCheckedChangeListener { _, isChecked ->
            category.isSelected = isChecked
            // Here you would trigger the logic to filter recipes based on this category
        }
    }

    override fun getItemCount() = categoryList.size
}
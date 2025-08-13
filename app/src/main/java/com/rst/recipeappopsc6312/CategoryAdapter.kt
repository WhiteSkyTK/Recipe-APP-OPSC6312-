package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class CategoryAdapter(
    private val categoryList: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip) {
        init {
            chip.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCategoryClick(categoryList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false) as Chip
        return ViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoryList[position]
        holder.chip.text = category.name
        holder.chip.isChecked = category.isSelected
    }

    override fun getItemCount() = categoryList.size
}
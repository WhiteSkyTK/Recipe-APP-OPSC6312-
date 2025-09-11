package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class CategoryAdapter(
    private var categoryList: List<Category>,
    var onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(category: Category, position: Int) {
            chip.text = category.name
            // Set checked state based on the tracked position
            chip.isChecked = (position == selectedPosition)

            chip.setOnClickListener {
                // When a chip is clicked, update the selected position
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                // Redraw the old and new selected items for an efficient update
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                // Call the click listener to trigger the filtering in the fragment
                onCategoryClick(categoryList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false) as Chip
        return ViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categoryList[position], position)
    }

    override fun getItemCount() = categoryList.size

    fun updateData(newCategories: List<Category>) {
        this.categoryList = newCategories
        // Reset selection to "All" when data is refreshed
        this.selectedPosition = 0
        notifyDataSetChanged()
    }
}
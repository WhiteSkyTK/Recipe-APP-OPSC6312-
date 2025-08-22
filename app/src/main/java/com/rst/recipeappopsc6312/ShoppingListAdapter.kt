package com.rst.recipeappopsc6312

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

// The adapter now takes lambdas to report user actions to the ViewModel
class ShoppingListAdapter(
    private val onItemClicked: (ShoppingItem) -> Unit,
    private val onItemLongClicked: (ShoppingItem) -> Unit,
    private val onCheckboxChanged: (item: ShoppingItem, isChecked: Boolean) -> Unit
) : ListAdapter<ShoppingItem, ShoppingListAdapter.ViewHolder>(ShoppingItemDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.shopping_item_checkbox)

        fun bind(item: ShoppingItem) {
            // Set the checkbox text and checked state from the item data
            checkBox.text = item.name
            checkBox.isChecked = item.isChecked

            // Visually show if an item is selected (e.g., for multi-delete)
            // Note: Your item layout (item_shopping_list.xml) needs a selectable background for this to work
            itemView.isActivated = item.isSelected

            // Set listeners that call the lambdas, passing the specific item
            itemView.setOnClickListener { onItemClicked(item) }
            itemView.setOnLongClickListener {
                onItemLongClicked(item)
                true // Must return true to indicate the event was consumed
            }

            // Important: Use a listener that is not the one on the view
            // to avoid triggering both on click.
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckboxChanged(item, isChecked)
            }

            // Apply strikethrough based on the checked state
            setStrikeThrough(checkBox, item.isChecked)
        }

        private fun setStrikeThrough(checkBox: MaterialCheckBox, isChecked: Boolean) {
            if (isChecked) {
                checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil helps the ListAdapter efficiently update the list
    class ShoppingItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
        override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
            return oldItem == newItem
        }
    }
}
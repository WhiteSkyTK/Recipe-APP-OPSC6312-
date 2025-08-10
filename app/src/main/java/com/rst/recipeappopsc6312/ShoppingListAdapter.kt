package com.rst.recipeappopsc6312

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

class ShoppingListAdapter(private val items: MutableList<ShoppingItem>) :
    RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    inner class ViewHolder(val checkBox: MaterialCheckBox) : RecyclerView.ViewHolder(checkBox)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val checkBox = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false) as MaterialCheckBox
        return ViewHolder(checkBox)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.checkBox.text = "${position + 1}. ${item.name}"
        holder.checkBox.isChecked = item.isChecked

        // Apply or remove strikethrough based on checked state
        setStrikeThrough(holder.checkBox, item.isChecked)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            setStrikeThrough(holder.checkBox, isChecked)
            // Here you would also update the item's status in your Supabase database
        }
    }

    private fun setStrikeThrough(checkBox: MaterialCheckBox, isChecked: Boolean) {
        if (isChecked) {
            checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount() = items.size
}
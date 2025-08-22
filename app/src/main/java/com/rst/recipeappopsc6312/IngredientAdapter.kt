package com.rst.recipeappopsc6312

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IngredientAdapter(private var ingredientList: MutableList<Ingredient>) :
    RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    private var isSelectionMode = false
    val selectedIngredients = mutableSetOf<Ingredient>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewIngredientName)
        val quantityTextView: TextView = itemView.findViewById(R.id.textViewIngredientQuantity)
        val checkBox: CheckBox = itemView.findViewById(R.id.ingredient_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]
        holder.nameTextView.text = ingredient.name

        // ++ THIS IS THE NEW LOGIC FOR UNIT CONVERSION ++
        val context = holder.itemView.context
        // 1. Get the user's preferred unit system from SharedPreferences
        val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val preferredSystem = prefs.getString("UnitSystem", UnitConverter.METRIC) ?: UnitConverter.METRIC

        // 2. Safely parse the quantity to a number
        val amount = ingredient.quantity.toDoubleOrNull() ?: 0.0

        // 3. Call the converter and set the text
        val convertedQuantity = UnitConverter.convert(amount, ingredient.unit, preferredSystem)
        holder.quantityTextView.text = convertedQuantity

        // --- The rest of your selection logic is correct ---
        if (isSelectionMode) {
            holder.nameTextView.visibility = View.INVISIBLE
            holder.checkBox.visibility = View.VISIBLE
            holder.checkBox.text = ingredient.name
            holder.checkBox.isChecked = selectedIngredients.contains(ingredient)

            holder.itemView.setOnClickListener { toggleSelection(ingredient, holder.checkBox) }
            holder.checkBox.setOnClickListener { toggleSelection(ingredient, holder.checkBox) }
        } else {
            holder.nameTextView.visibility = View.VISIBLE
            holder.checkBox.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            holder.checkBox.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = ingredientList.size

    private fun toggleSelection(ingredient: Ingredient, checkBox: CheckBox) {
        if (selectedIngredients.contains(ingredient)) {
            selectedIngredients.remove(ingredient)
            checkBox.isChecked = false
        } else {
            selectedIngredients.add(ingredient)
            checkBox.isChecked = true
        }
    }

    fun updateIngredients(newIngredients: List<Ingredient>) {
        this.ingredientList = newIngredients.toMutableList()
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedIngredients.clear()
        }
        notifyDataSetChanged() // Redraw the list to show/hide checkboxes
    }
}
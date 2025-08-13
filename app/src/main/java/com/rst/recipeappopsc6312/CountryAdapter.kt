package com.rst.recipeappopsc6312

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CountryAdapter(
    private var countryList: List<Country>,
    private val onCountryClicked: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>(), Filterable {

    var countryFilterList: List<Country> = countryList
    private var selectedPosition = RecyclerView.NO_POSITION

    inner class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flag: ImageView = itemView.findViewById(R.id.imageViewFlag)
        val name: TextView = itemView.findViewById(R.id.textViewCountryName)
        val code: TextView = itemView.findViewById(R.id.textViewCountryCode)

        init {
            itemView.setOnClickListener {
                val newPosition = adapterPosition
                if (newPosition != RecyclerView.NO_POSITION) {
                    onCountryClicked(countryFilterList[newPosition])

                    val oldPosition = selectedPosition // Store the old position
                    selectedPosition = newPosition   // Set the new position

                    // FIXED: Only notify the old position if it was valid
                    if (oldPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(oldPosition)
                    }
                    // Notify the new position to select it
                    notifyItemChanged(newPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countryFilterList[position]
        holder.name.text = country.nameInfo.common
        holder.code.text = country.code
        Glide.with(holder.itemView.context).load(country.flags.png).into(holder.flag)

        // Set the selection state
        holder.itemView.isSelected = (selectedPosition == position)
    }

    override fun getItemCount(): Int = countryFilterList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                countryFilterList = if (charSearch.isEmpty()) {
                    countryList
                } else {
                    val resultList = ArrayList<Country>()
                    for (row in countryList) {
                        if (row.nameInfo.common.lowercase().contains(charSearch.lowercase())) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = countryFilterList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                countryFilterList = results?.values as? List<Country> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
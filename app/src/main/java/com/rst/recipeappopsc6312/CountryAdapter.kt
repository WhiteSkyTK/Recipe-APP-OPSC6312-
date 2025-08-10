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

class CountryAdapter(private var countryList: List<Country>) :
    RecyclerView.Adapter<CountryAdapter.CountryViewHolder>(), Filterable {

    // This list will hold the filtered results
    var countryFilterList: List<Country> = countryList

    inner class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flag: ImageView = itemView.findViewById(R.id.imageViewFlag)
        val name: TextView = itemView.findViewById(R.id.textViewCountryName)
        val code: TextView = itemView.findViewById(R.id.textViewCountryCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country, parent, false)
        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        val country = countryFilterList[position]

        // Use Glide to load the image from the URL
        Glide.with(holder.itemView.context)
            .load(country.flags.png) // Load the flag URL
            .into(holder.flag)

        holder.name.text = country.nameInfo.common
        holder.code.text = country.code
    }

    override fun getItemCount(): Int {
        return countryFilterList.size
    }

    // This is where the search magic happens!
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                countryFilterList = if (charSearch.isEmpty()) {
                    countryList // If search is empty, show all countries
                } else {
                    val resultList = ArrayList<Country>()
                    for (row in countryList) {
                        // FIXED: Search the common name inside the nameInfo object
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
                notifyDataSetChanged() // Refresh the list with filtered results
            }
        }
    }
}
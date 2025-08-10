package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShoppingListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private var shoppingList = mutableListOf<ShoppingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shopping_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewShoppingList)

        // In a real app, you would fetch this list from Supabase
        prepareDummyData()

        shoppingListAdapter = ShoppingListAdapter(shoppingList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = shoppingListAdapter

        // You would add a click listener to the "Add more" button
        // to show a dialog for adding new items.

        return view
    }

    private fun prepareDummyData() {
        shoppingList.add(ShoppingItem("Brown Rice"))
        shoppingList.add(ShoppingItem("Chickpeas"))
        shoppingList.add(ShoppingItem("Sweet Potatoes", isChecked = true)) // Example of a checked item
        shoppingList.add(ShoppingItem("Apples"))
        shoppingList.add(ShoppingItem("Ground Beef"))
    }
}
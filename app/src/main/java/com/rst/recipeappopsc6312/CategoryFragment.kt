package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private var categoryList = ArrayList<Category>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewAllCategories)

        prepareCategoryData()

        categoryAdapter = CategoryAdapter(categoryList)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = categoryAdapter

        return view
    }

    private fun prepareCategoryData() {
        // Add all your categories here
        categoryList.add(Category("Breakfast", isSelected = true)) // Example of a pre-selected item
        categoryList.add(Category("Lunch"))
        categoryList.add(Category("Dinner"))
        categoryList.add(Category("Fruits"))
        categoryList.add(Category("Dairy product"))
        categoryList.add(Category("Protein"))
        categoryList.add(Category("Cereal"))
        categoryList.add(Category("Grain"))
        categoryList.add(Category("Egg"))
        categoryList.add(Category("Vegetables"))
        categoryList.add(Category("Dairy"))
        categoryList.add(Category("Sweets"))
        categoryList.add(Category("Beverages"))
        categoryList.add(Category("Spices"))
        categoryList.add(Category("Snacks"))
    }
}
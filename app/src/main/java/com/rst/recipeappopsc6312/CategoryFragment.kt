package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // Get the full category list from the central DummyData object
        val categoryList = DummyData.getAllCategories()

        categoryAdapter = CategoryAdapter(categoryList) { clickedCategory ->
            // This is where you handle the click on a category
            // For now, we'll just show a Toast message.
            Toast.makeText(context, "Selected: ${clickedCategory.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.adapter = categoryAdapter

        return view
    }

}
package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ShoppingListFragment : Fragment() {

    // --- ViewModel and Adapter ---
    // This assumes you have a ViewModelFactory set up, which I'll explain below.
    private val viewModel: ShoppingViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        // Provide the missing FirebaseStorage instance as the fourth argument
        val repository = ShoppingRepository(
            database.shoppingDao(),
            database.recipeDao(),
            database.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance() // <-- This was the missing part
        )
        ShoppingViewModelFactory(repository)
    }
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private lateinit var deleteListButton: ImageButton

    // --- Views ---
    private lateinit var shoppingListRecyclerView: RecyclerView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var fabDeleteItems: FloatingActionButton
    private lateinit var titleTextView: TextView
    private lateinit var listsSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // onCreateView should ONLY be used to inflate the view.
        return inflater.inflate(R.layout.fragment_shopping_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // All setup logic correctly happens here.
        setupViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews(view: View) {
        shoppingListRecyclerView = view.findViewById(R.id.recyclerViewShoppingList)
        fabAddItem = view.findViewById(R.id.fabAddItem)
        fabDeleteItems = view.findViewById(R.id.fabDeleteItems)
        titleTextView = view.findViewById(R.id.textViewPageTitle)
        listsSpinner = view.findViewById(R.id.spinnerShoppingLists)
        deleteListButton = view.findViewById(R.id.buttonDeleteList)
    }

    private fun setupRecyclerView() {
        shoppingListAdapter = ShoppingListAdapter(
            onItemClicked = { item -> viewModel.onItemClicked(item) },
            onItemLongClicked = { item -> viewModel.onItemLongClicked(item) },
            onCheckboxChanged = { item, isChecked -> viewModel.onItemCheckedChanged(item, isChecked) }
        )

        shoppingListRecyclerView.apply {
            adapter = shoppingListAdapter
            layoutManager = LinearLayoutManager(context)
        }

        setupItemTouchHelper()
    }

    private fun setupObservers() {
        viewModel.allShoppingLists.observe(viewLifecycleOwner) { lists ->
            // We'll add special "All" and "My List" options here
            val listNames = mutableListOf("All Items 🛒", "My List 📝")
            listNames.addAll(lists.map { "${it.emoji} ${it.name}" })

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                listNames
            )
            listsSpinner.adapter = adapter
        }

        // Observer for the recycler view items
        viewModel.currentShoppingItems.observe(viewLifecycleOwner) { items ->
            items?.let { shoppingListAdapter.submitList(it.sortedBy { item -> item.isChecked }) }
        }

        viewModel.isInSelectionMode.observe(viewLifecycleOwner) { isInSelectionMode ->
            updateUiForSelectionMode(isInSelectionMode, viewModel.selectedItemsCount.value ?: 0)
        }

        viewModel.selectedItemsCount.observe(viewLifecycleOwner) { count ->
            updateUiForSelectionMode(viewModel.isInSelectionMode.value ?: false, count)
        }
    }

    private fun setupClickListeners() {
        fabAddItem.setOnClickListener { showAddItemDialog() }
        fabDeleteItems.setOnClickListener { viewModel.deleteSelectedItems() }
        deleteListButton.setOnClickListener {
            // Show a confirmation dialog before deleting
            AlertDialog.Builder(requireContext())
                .setTitle("Delete List 🗑️") // Emoji in title
                .setMessage("Are you sure you want to delete this shopping list and all its items? 😥") // Emoji in message
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCurrentList()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        listsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.onListSelected(position)
                updateUiForSelectionMode(viewModel.isInSelectionMode.value ?: false, viewModel.selectedItemsCount.value ?: 0)

                // ++ Show/Hide the delete button based on selection ++
                deleteListButton.visibility = if (position > 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                deleteListButton.visibility = View.GONE
            }
        }
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            // CORRECTED: The full function signature is needed for the override.
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = shoppingListAdapter.currentList[position]
                viewModel.deleteItem(item) // Delegate deletion to ViewModel

                Snackbar.make(requireView(), "🗑️ ${item.name} deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        viewModel.addItem(item) // Delegate re-adding to ViewModel
                    }.show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(shoppingListRecyclerView)
    }

    private fun updateUiForSelectionMode(isInSelectionMode: Boolean, count: Int) {
        // UPDATED: Check if position is 0 ("All Items") or 1 ("My List")
        val canAddItem = (listsSpinner.selectedItemPosition <= 1)

        if (isInSelectionMode) {
            fabAddItem.hide()
            fabDeleteItems.show()
            titleTextView.text = "$count item(s) selected"
        } else {
            // UPDATED: Use the new 'canAddItem' boolean
            if (canAddItem) fabAddItem.show() else fabAddItem.hide()
            fabDeleteItems.hide()
            titleTextView.text = listsSpinner.selectedItem?.toString() ?: "Shopping List 🍽️"
        }
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_shopping_items, null)
        val editTextItems = dialogView.findViewById<EditText>(R.id.editTextItems)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Shopping Items")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val itemsText = editTextItems.text.toString()
                if (itemsText.isNotBlank()) {
                    val itemNames = itemsText.split("\n")
                        .filter { it.trim().isNotEmpty() }

                    // CORRECTED: Tell the ViewModel to add the new items.
                    viewModel.addItems(itemNames)

                    Toast.makeText(context, "${itemNames.size} items added.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }
}
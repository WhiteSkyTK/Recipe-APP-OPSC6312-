package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ScanFragment : Fragment() {

    private lateinit var ingredientInputLayout: TextInputLayout
    private lateinit var ingredientEditText: TextInputEditText
    private lateinit var ingredientChipGroup: ChipGroup

    // You will need to implement the logic for camera/voice launchers
    // private val cameraLauncher = ...
    // private val voiceLauncher = ...

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        ingredientInputLayout = view.findViewById(R.id.textInputLayoutIngredient)
        ingredientEditText = view.findViewById(R.id.editTextIngredient)
        ingredientChipGroup = view.findViewById(R.id.chipGroupIngredients)

        // --- Click Listeners ---

        // Add ingredient when the end icon is clicked
        ingredientInputLayout.setEndIconOnClickListener {
            val ingredientName = ingredientEditText.text.toString().trim()
            if (ingredientName.isNotEmpty()) {
                addIngredientChip(ingredientName)
                ingredientEditText.text?.clear()
            }
        }

        view.findViewById<View>(R.id.buttonScan).setOnClickListener {
            // --- Placeholder for Camera/ML Kit logic ---
            Toast.makeText(context, "Starting camera scanner...", Toast.LENGTH_SHORT).show()
            // cameraLauncher.launch(...)
        }

        view.findViewById<View>(R.id.buttonVoice).setOnClickListener {
            // --- Placeholder for Voice-to-Text logic ---
            Toast.makeText(context, "Listening for voice input...", Toast.LENGTH_SHORT).show()
            // voiceLauncher.launch(...)
        }

        view.findViewById<View>(R.id.buttonFindRecipes).setOnClickListener {
            val ingredients = getIngredientsFromChips()
            if (ingredients.isNotEmpty()) {
                // --- Placeholder for API call logic ---
                Toast.makeText(context, "Searching for recipes with: $ingredients", Toast.LENGTH_LONG).show()
                // Navigate to a results fragment/activity
            } else {
                Toast.makeText(context, "Please add some ingredients first", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun addIngredientChip(name: String) {
        val chip = layoutInflater.inflate(R.layout.item_ingredient_chip, ingredientChipGroup, false) as Chip
        chip.text = name
        chip.setOnCloseIconClickListener {
            ingredientChipGroup.removeView(it)
        }
        ingredientChipGroup.addView(chip)
    }

    private fun getIngredientsFromChips(): List<String> {
        return (0 until ingredientChipGroup.childCount).map { i ->
            (ingredientChipGroup.getChildAt(i) as Chip).text.toString()
        }
    }
}
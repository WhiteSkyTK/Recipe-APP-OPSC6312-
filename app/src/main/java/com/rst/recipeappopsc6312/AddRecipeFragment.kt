package com.rst.recipeappopsc6312

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class AddRecipeFragment : Fragment() {

    private lateinit var coverPhotoImageView: ImageView
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private var coverImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coverImageUri = it
            coverPhotoImageView.setImageURI(it)
            coverPhotoImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_recipe, container, false)

        coverPhotoImageView = view.findViewById(R.id.imageViewCoverPhoto)
        ingredientsContainer = view.findViewById(R.id.ingredientsContainer)
        stepsContainer = view.findViewById(R.id.stepsContainer)
        val addIngredientButton = view.findViewById<Button>(R.id.buttonAddIngredient)
        val addStepButton = view.findViewById<Button>(R.id.buttonAddStep)
        val saveButton = view.findViewById<Button>(R.id.buttonSaveRecipe)

        // --- Click Listeners ---
        coverPhotoImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        addIngredientButton.setOnClickListener {
            addIngredientView()
        }

        addStepButton.setOnClickListener {
            addStepView()
        }

        saveButton.setOnClickListener {
            // --- Placeholder for Save Logic ---
            // 1. Validate all input fields.
            // 2. Collect all data into a Recipe object.
            // 3. Save the recipe to your local SQLite database.
            // 4. Trigger a background sync to upload the new recipe to Supabase.
            Toast.makeText(context, "Recipe Saved!", Toast.LENGTH_SHORT).show()
        }

        // Add the first ingredient and step fields by default
        addIngredientView()
        addStepView()

        return view
    }

    private fun addIngredientView() {
        val inflater = LayoutInflater.from(context)
        val ingredientView = inflater.inflate(R.layout.item_ingredient_input, ingredientsContainer, false)
        ingredientsContainer.addView(ingredientView)
    }

    private fun addStepView() {
        val inflater = LayoutInflater.from(context)
        val stepView = inflater.inflate(R.layout.item_method_step_input, stepsContainer, false)
        stepsContainer.addView(stepView)
    }
}
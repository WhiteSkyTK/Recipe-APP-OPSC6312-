package com.rst.recipeappopsc6312

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AddRecipeFragment : Fragment() {
    private val TAG = "AddRecipeFragment"
    private lateinit var coverPhotoImageView: ImageView
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var stepsContainer: LinearLayout
    private lateinit var loadingOverlayContainer: FrameLayout // For the overlay
    private lateinit var deleteButton: Button

    private var coverImageUri: Uri? = null
    private var tempImageUri: Uri? = null
    private var recipeToEdit: Recipe? = null
    private var editRecipeId: String? = null

    private val viewModel: AddRecipeViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repository = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance() // Pass storage instance
        )
        ViewModelFactory(repository)
    }

    // ++ NEW LAUNCHER for taking a picture
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverImageUri = tempImageUri
            coverPhotoImageView.setImageURI(coverImageUri)
            coverPhotoImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    // Renamed for clarity
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coverImageUri = it
            coverPhotoImageView.setImageURI(it)
            coverPhotoImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    // ++ NEW LAUNCHER for camera permission
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), R.string.permission_camera_required, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if a recipe ID was passed for editing
        arguments?.let {
            editRecipeId = it.getString("EDIT_RECIPE_ID")
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
        deleteButton = view.findViewById(R.id.buttonDeleteRecipe)
        val addIngredientButton = view.findViewById<Button>(R.id.buttonAddIngredient)
        val addStepButton = view.findViewById<Button>(R.id.buttonAddStep)
        val saveButton = view.findViewById<Button>(R.id.buttonSaveRecipe)
        val categoryAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        val mealTypeAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMealType)
        loadingOverlayContainer = view.findViewById(R.id.loadingOverlayContainer) // Find the FrameLayout

        val mealTypes = resources.getStringArray(R.array.meal_types)
        val mealTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mealTypes)
        mealTypeAutoComplete.setAdapter(mealTypeAdapter)

        // --- Click Listeners ---
        coverPhotoImageView.setOnClickListener { showImagePickerDialog() }
        addIngredientButton.setOnClickListener { addIngredientView() }
        addStepButton.setOnClickListener { addStepView() }
        saveButton.setOnClickListener { saveRecipe() }
        deleteButton.setOnClickListener {
            recipeToEdit?.let { recipe ->
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_recipe_dialog_title)
                    .setMessage(R.string.delete_recipe_dialog_message)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteRecipe(recipe)
                        showLoading(true)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
        return view
    }

    private fun showImagePickerDialog() {
        val options = resources.getStringArray(R.array.dialog_cover_photo_options)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_set_photo_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> {
                        coverImageUri = null
                        coverPhotoImageView.setImageResource(R.drawable.ic_add_a_photo)
                        coverPhotoImageView.scaleType = ImageView.ScaleType.CENTER
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.permission_needed_title)
                    .setMessage(R.string.permission_camera_rationale)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val file = File(requireContext().filesDir, "temp_image.jpg")
        tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        cameraLauncher.launch(tempImageUri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ++ THIS IS THE NEW LOGIC: Observe the saveStatus LiveData from the ViewModel ++
        viewModel.saveStatus.observe(viewLifecycleOwner) { result ->
            showLoading(false)
            if (result.success) {
                val messageResId = if (result.isDelete) R.string.toast_recipe_deleted else R.string.toast_recipe_saved
                Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()

                // Trigger a notification for successful uploads
                if (!result.isDelete) {
                    val recipeName = view.findViewById<TextInputLayout>(R.id.textInputLayoutRecipeName).editText?.text.toString()
                    lifecycleScope.launch {
                        viewModel.repository.createNotification(
                            getString(R.string.notification_upload_success_title),
                            getString(R.string.notification_upload_success_body, recipeName), // Pass recipeName as placeholder
                            "ic_new_recipe"
                        )
                    }
                }

                if (result.isDelete) {
                    parentFragmentManager.popBackStack()
                } else {
                    startActivity(Intent(activity, MyRecipesActivity::class.java))
                    parentFragmentManager.popBackStack()
                }
            } else {
                Toast.makeText(context, getString(R.string.toast_operation_failed, result.error), Toast.LENGTH_LONG).show()
            }
        }

        val categoryAutoComplete = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory)
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->
            // This will run when the categories are fetched from the repository
            val categoryNames = categoryList.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            categoryAutoComplete.setAdapter(adapter)
        }

        // If we are in edit mode, fetch the recipe and populate the form
        if (editRecipeId != null) {
            view.findViewById<TextView>(R.id.textViewAddRecipeTitle).text = getString(R.string.edit_recipe_title)
            view.findViewById<Button>(R.id.buttonSaveRecipe).text = getString(R.string.save_changes)
            deleteButton.visibility = View.VISIBLE // Show delete button in edit mode
            loadRecipeForEditing(editRecipeId!!)
        } else {
            if (ingredientsContainer.childCount == 0) addIngredientView()
            if (stepsContainer.childCount == 0) addStepView()
        }
    }

    private fun loadRecipeForEditing(recipeId: String) {
        showLoading(true)
        lifecycleScope.launch {
            recipeToEdit = viewModel.repository.getRecipeById(recipeId)
            if (recipeToEdit != null) {
                populateForm(recipeToEdit!!)
            } else {
                Toast.makeText(context, R.string.toast_could_not_load_recipe, Toast.LENGTH_SHORT).show()
            }
            showLoading(false)
        }
    }

    private fun populateForm(recipe: Recipe) {
        val view = requireView()
        view.findViewById<TextInputLayout>(R.id.textInputLayoutRecipeName).editText?.setText(recipe.title)
        view.findViewById<TextInputLayout>(R.id.textInputLayoutDescription).editText?.setText(recipe.description)
        view.findViewById<TextInputLayout>(R.id.textInputLayoutTime).editText?.setText(recipe.timeInMins.toString())
        view.findViewById<TextInputLayout>(R.id.textInputLayoutServings).editText?.setText(recipe.servings.toString())
        view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).setText(recipe.category, false)
        view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMealType).setText(recipe.mealType, false)

        if (recipe.imageUrl.isNotBlank()) {
            coverImageUri = Uri.parse(recipe.imageUrl)
            Glide.with(this).load(coverImageUri).into(coverPhotoImageView)
        }

        ingredientsContainer.removeAllViews()
        recipe.ingredients.forEach { addIngredientView(it) }

        stepsContainer.removeAllViews()
        recipe.method.forEach { addStepView(it) }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingOverlayContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
        // You might also want to disable interactive elements like the save button
        view?.findViewById<Button>(R.id.buttonSaveRecipe)?.isEnabled = !isLoading
        // Disable other input fields and buttons as needed
    }

    private fun addIngredientView(ingredient: Ingredient? = null) {
        val inflater = LayoutInflater.from(context)
        val ingredientView = inflater.inflate(R.layout.item_ingredient_input, ingredientsContainer, false)

        // ++ POPULATE THE UNITS DROPDOWN (CORRECTED) ++
        val units = listOf("g", "kg", "ml", "L", "tsp", "tbsp", "cup", "pinch", "piece(s)")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)

        // Find the AutoCompleteTextView by its new ID
        val unitAutoCompleteTextView = ingredientView.findViewById<AutoCompleteTextView>(R.id.autoCompleteUnit)
        unitAutoCompleteTextView.setAdapter(unitAdapter)

        val removeButton = ingredientView.findViewById<ImageButton>(R.id.buttonRemoveIngredient)
        removeButton.setOnClickListener {
            ingredientsContainer.removeView(ingredientView)
        }
        ingredientsContainer.addView(ingredientView)

        if (ingredient != null) {
            ingredientView.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientName).editText?.setText(ingredient.name)
            ingredientView.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientQty).editText?.setText(ingredient.quantity)
            ingredientView.findViewById<AutoCompleteTextView>(R.id.autoCompleteUnit).setText(ingredient.unit, false)
        }
    }
    private fun addStepView(step: MethodStep? = null) {
        val inflater = LayoutInflater.from(context)
        val stepView = inflater.inflate(R.layout.item_method_step_input, stepsContainer, false)
        val removeButton = stepView.findViewById<ImageButton>(R.id.buttonRemoveStep)
        val stepNumber = stepView.findViewById<TextView>(R.id.textViewStepNumber)

        stepNumber.text = "${stepsContainer.childCount + 1}."

        removeButton.setOnClickListener {
            stepsContainer.removeView(stepView)
            // Update the numbers of the remaining steps
            for (i in 0 until stepsContainer.childCount) {
                val child = stepsContainer.getChildAt(i)
                child.findViewById<TextView>(R.id.textViewStepNumber).text = "${i + 1}."
            }
        }
        stepsContainer.addView(stepView)

        if (step != null) {
            stepView.findViewById<TextInputLayout>(R.id.textInputLayoutStep).editText?.setText(step.step)
        }
    }

    private fun saveRecipe() {
        if (!validateInputs(requireView())) return
        showLoading(true)

        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, R.string.toast_login_required, Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        FirebaseManager.firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val authorUsername = document.getString("username") ?: "Unknown Author"
                val recipe = collectRecipeDataFromView(requireView(), authorUsername)

                viewModel.saveRecipe(recipe, coverImageUri)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(context, R.string.toast_profile_error, Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(view: View): Boolean {
        val recipeName = view.findViewById<TextInputLayout>(R.id.textInputLayoutRecipeName).editText?.text.toString()
        val category = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).text.toString()
        val time = view.findViewById<TextInputLayout>(R.id.textInputLayoutTime).editText?.text.toString()
        val servings = view.findViewById<TextInputLayout>(R.id.textInputLayoutServings).editText?.text.toString()
        val mealType = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMealType).text.toString()

        if (recipeName.isBlank()) {
            Toast.makeText(context, R.string.validation_recipe_name_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (category.isBlank()) {
            Toast.makeText(context, R.string.validation_category_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (time.isBlank()) {
            Toast.makeText(context, R.string.validation_time_required, Toast.LENGTH_SHORT).show()
            return false
        }
        if (servings.isBlank()) {
            Toast.makeText(context, R.string.validation_servings_required, Toast.LENGTH_SHORT).show()
            return false
        }

        if (mealType.isBlank()) {
            Toast.makeText(context, R.string.validation_meal_type_required, Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate ingredients
        if (ingredientsContainer.childCount == 0) {
            Toast.makeText(context, R.string.validation_ingredient_min_required, Toast.LENGTH_SHORT).show()
            return false
        }
        for (child in ingredientsContainer.children) {
            val name = child.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientName).editText?.text.toString()
            // ++ ADD VALIDATION FOR QUANTITY ++
            val qty = child.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientQty).editText?.text.toString()

            if (name.isBlank()) {
                Toast.makeText(context, R.string.validation_ingredient_name_required, Toast.LENGTH_SHORT).show()
                return false
            }
            if (qty.isBlank()) { // ++ NEW CHECK
                Toast.makeText(context, R.string.validation_ingredient_qty_required, Toast.LENGTH_SHORT).show()
                return false
            }
        }

        // Validate steps
        if (stepsContainer.childCount == 0) {
            Toast.makeText(context, R.string.validation_step_min_required, Toast.LENGTH_SHORT).show()
            return false
        }
        for (child in stepsContainer.children) {
            val step = child.findViewById<TextInputLayout>(R.id.textInputLayoutStep).editText?.text.toString()
            if (step.isBlank()) {
                Toast.makeText(context, R.string.validation_step_desc_required, Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun collectRecipeDataFromView(view: View, authorUsername: String): Recipe {
        val userId = FirebaseManager.auth.currentUser?.uid ?: ""
        val recipeName = view.findViewById<TextInputLayout>(R.id.textInputLayoutRecipeName).editText?.text.toString()
        val description = view.findViewById<TextInputLayout>(R.id.textInputLayoutDescription).editText?.text.toString()
        val time = view.findViewById<TextInputLayout>(R.id.textInputLayoutTime).editText?.text.toString().toIntOrNull() ?: 0
        val servings = view.findViewById<TextInputLayout>(R.id.textInputLayoutServings).editText?.text.toString().toIntOrNull() ?: 0
        val category = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteCategory).text.toString()
        val mealType = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteMealType).text.toString()
        val recipeId = editRecipeId ?: UUID.randomUUID().toString()

        val ingredientsList = ingredientsContainer.children
            .mapNotNull { childView ->
                val name = childView.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientName).editText?.text.toString()
                val qty = childView.findViewById<TextInputLayout>(R.id.textInputLayoutIngredientQty).editText?.text.toString()
                // Get the text from the AutoCompleteTextView directly
                val unit = childView.findViewById<AutoCompleteTextView>(R.id.autoCompleteUnit).text.toString()

                if (name.isNotBlank() && qty.isNotBlank()) Ingredient(name, qty, unit) else null
            }.toList()

        // ++ THIS IS THE CORRECTED LOGIC for steps ++
        val stepsList = stepsContainer.children
            .mapNotNull { childView ->
                val stepLayout = childView.findViewById<TextInputLayout>(R.id.textInputLayoutStep)
                val stepDesc = stepLayout.editText?.text.toString()
                if (stepDesc.isNotBlank()) MethodStep(step = stepDesc) else null
            }.toList()

        val nutritionList = mutableListOf<NutritionFact>()
        val calories = view.findViewById<TextInputLayout>(R.id.textInputLayoutCalories).editText?.text.toString()
        val fat = view.findViewById<TextInputLayout>(R.id.textInputLayoutFat).editText?.text.toString()
        val carbs = view.findViewById<TextInputLayout>(R.id.textInputLayoutCarbs).editText?.text.toString()
        val salts = view.findViewById<TextInputLayout>(R.id.textInputLayoutSalts).editText?.text.toString()

        // Use string resources for nutrition fact names and units
        if (calories.isNotBlank()) nutritionList.add(NutritionFact(getString(R.string.calories), calories))
        if (fat.isNotBlank()) nutritionList.add(NutritionFact(getString(R.string.nutrition_fat), getString(R.string.unit_grams, fat)))
        if (carbs.isNotBlank()) nutritionList.add(NutritionFact(getString(R.string.nutrition_carbs), getString(R.string.unit_grams, carbs)))
        if (salts.isNotBlank()) nutritionList.add(NutritionFact(getString(R.string.nutrition_salts), getString(R.string.unit_grams, salts)))

        return Recipe(
            id = recipeId,
            userId = userId,
            title = recipeName,
            author = authorUsername,
            description = description,
            timeInMins = time,
            servings = servings,
            isPublic = false,
            ingredients = ingredientsList,
            method = stepsList,
            category = category,
            mealType = mealType,
            nutrition = nutritionList,
            imageUrl = "" // Will be replaced after upload
        )
    }

}
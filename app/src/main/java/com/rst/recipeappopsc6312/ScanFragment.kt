package com.rst.recipeappopsc6312

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
class ScanFragment : Fragment() {

    private lateinit var ingredientInputLayout: TextInputLayout
    private lateinit var ingredientEditText: TextInputEditText
    private lateinit var ingredientChipGroup: ChipGroup
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ScanHistoryAdapter
    private lateinit var recipeMatchAdapter: RecipeMatchAdapter
    private val TAG = "ScanFragment"

    private val viewModel: ScanViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        // ++ UPDATE this to match the new constructor
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(), // Pass the new DAO
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ScanViewModelFactory(repo)
    }


    // ++ LAUNCHER FOR CAMERA (to get a photo) ++
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizeTextFromImage(image)
        } else {
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    // ++ LAUNCHER FOR VOICE INPUT ++
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: ArrayList<String>? = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                // The API might return multiple interpretations, we take the first and split it by spaces
                val words = spokenText[0].split(" ")
                words.forEach { word ->
                    if (word.isNotBlank()) addIngredientChip(word.trim().capitalize(Locale.ROOT))
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action
                cameraLauncher.launch(null)
            } else {
                // Explain to the user that the feature is unavailable
                Toast.makeText(context, "Camera permission is needed to scan ingredients.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        ingredientInputLayout = view.findViewById(R.id.textInputLayoutIngredient)
        ingredientEditText = view.findViewById(R.id.editTextIngredient)
        ingredientChipGroup = view.findViewById(R.id.chipGroupIngredients)
        historyRecyclerView = view.findViewById(R.id.recyclerViewHistory)

        setupHistoryList()
        observeViewModel()

        // ++ IMPROVED TYPING: Add ingredient on keyboard "Enter" or "Done" button press ++
        ingredientEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                addTypedIngredient()
                return@setOnEditorActionListener true
            }
            false
        }

        // Add ingredient when the end icon is clicked (optional, if you have one)
        ingredientEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                addTypedIngredient()
                return@setOnEditorActionListener true
            }
            false
        }

        view.findViewById<View>(R.id.buttonScan).setOnClickListener {
            when {
                // 1. Check if we already have permission
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted, launch the camera
                    cameraLauncher.launch(null)
                }

                // 2. (RECOMMENDED) Explain why you need the permission if they denied it before
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Permission Needed")
                        .setMessage("This feature requires camera access to scan ingredients. Please grant the permission to continue.")
                        .setPositiveButton("OK") { _, _ ->
                            // After they tap OK, request the permission again
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                // 3. If it's the first time, just ask for permission
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        view.findViewById<View>(R.id.buttonVoice).setOnClickListener {
            // Launch the voice recognizer
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your ingredients...")
            }
            try {
                voiceLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.buttonFindRecipes).setOnClickListener {
            val ingredients = getIngredientsFromChips()
            if (ingredients.isNotEmpty()) {
                // First, find recipes to get an image for the history item
                viewModel.findRecipes(ingredients)

                // Launch the results activity
                val intent = Intent(activity, ScanResultsActivity::class.java)
                intent.putStringArrayListExtra("INGREDIENTS", ArrayList(ingredients))
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please add some ingredients first", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun addTypedIngredient() {
        val ingredientName = ingredientEditText.text.toString().trim()
        if (ingredientName.isNotEmpty()) {
            addIngredientChip(ingredientName)
            ingredientEditText.text?.clear()
        }
    }

    private fun recognizeTextFromImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                // We get blocks of text, then lines, then words.
                // Let's add each recognized line as a separate ingredient chip.
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val lineText = line.text.trim()
                        if (lineText.isNotBlank()) {
                            addIngredientChip(lineText.capitalize(Locale.ROOT))
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Toast.makeText(context, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Text recognition failed", e)
            }
    }
    private fun observeViewModel() {
        // Observe the scan history from the ViewModel
        viewModel.scanHistory.observe(viewLifecycleOwner) { history ->
            historyAdapter.updateHistory(history)
        }

        // This observer saves the scan to history once the recipe results are found
        viewModel.recipeMatches.observe(viewLifecycleOwner) { matches ->
            val ingredients = getIngredientsFromChips()
            if (ingredients.isNotEmpty()) {
                val topImage = matches.firstOrNull()?.recipe?.imageUrl
                viewModel.saveScanToHistory(ingredients, topImage)
            }
        }
    }

    private fun setupHistoryList() {
        historyAdapter = ScanHistoryAdapter(emptyList()) { clickedItem ->
            val intent = Intent(activity, ScanResultsActivity::class.java)
            intent.putStringArrayListExtra("INGREDIENTS", ArrayList(clickedItem.ingredients))
            startActivity(intent)
        }
        historyRecyclerView.layoutManager = LinearLayoutManager(context)
        historyRecyclerView.adapter = historyAdapter
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
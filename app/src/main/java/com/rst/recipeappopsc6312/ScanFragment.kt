package com.rst.recipeappopsc6312

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment() {

    private lateinit var ingredientInputLayout: TextInputLayout
    private lateinit var ingredientChipGroup: ChipGroup
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ScanHistoryAdapter
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private val TAG = "ScanFragment"

    private val viewModel: ScanViewModel by viewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(), // Pass the new DAO
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
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

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is needed to scan ingredients.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan, container, false)

        ingredientInputLayout = view.findViewById(R.id.textInputLayoutIngredient)
        val ingredientEditText = view.findViewById<TextInputEditText>(R.id.editTextIngredient)
        ingredientChipGroup = view.findViewById(R.id.chipGroupIngredients)
        historyRecyclerView = view.findViewById(R.id.recyclerViewHistory)
        cameraPreviewView = view.findViewById(R.id.cameraPreviewView)
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupHistoryList()
        observeViewModel()

        // Check for permissions and start the camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        view.findViewById<View>(R.id.buttonScan).setOnClickListener {
            analyzeCurrentImage()
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

        ingredientEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTypedIngredient(ingredientEditText)
                return@setOnEditorActionListener true
            }
            false
        }

        return view
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            // We create the analyzer here but don't set a listener yet
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun analyzeCurrentImage() {
        lottieAnimationView.visibility = View.VISIBLE // Show animation

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Split the recognized text by lines and add each as a chip
                        activity?.runOnUiThread {
                            visionText.text.split("\n").forEach { line ->
                                if (line.isNotBlank()) {
                                    addIngredientChip(line.trim().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Text recognition failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                        activity?.runOnUiThread {
                            lottieAnimationView.visibility = View.GONE // Hide animation
                        }
                        // Stop analyzing after one frame to prevent continuous scanning
                        imageAnalyzer?.clearAnalyzer()
                    }
            }
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

    private fun addTypedIngredient(editText: TextInputEditText) {
        val ingredientName = editText.text.toString().trim()
        if (ingredientName.isNotEmpty()) {
            addIngredientChip(ingredientName)
            editText.text?.clear()
        }
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

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
package com.rst.recipeappopsc6312

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class ScanManualFragment : Fragment() {

    private val viewModel: ScanViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(), com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.storage.FirebaseStorage.getInstance())
        ViewModelFactory(repo)
    }

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: ArrayList<String>? = result.data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS)
            if (!spokenText.isNullOrEmpty()) {
                // The API might return multiple interpretations; we take the first and split it by spaces.
                // This allows the user to say "flour sugar eggs" and add three separate chips.
                val words = spokenText[0].split(" ")
                words.forEach { word ->
                    if (word.isNotBlank()) {
                        // We use the ViewModel to add the ingredient, ensuring consistent logic.
                        viewModel.addIngredient(word.trim().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_manual, container, false)
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.textInputLayoutIngredient)
        val editText = textInputLayout.editText
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupIngredients)
        val findButton = view.findViewById<MaterialButton>(R.id.buttonFindRecipes)
        val voiceButton = view.findViewById<ImageButton>(R.id.buttonVoice)

        textInputLayout.setEndIconOnClickListener {
            viewModel.addIngredient(editText?.text.toString())
            editText?.text?.clear()
        }

        viewModel.ingredientsList.observe(viewLifecycleOwner) { ingredients ->
            chipGroup.removeAllViews()
            ingredients.forEach { ingredient ->
                val chip = Chip(requireContext()).apply {
                    text = ingredient
                    isCloseIconVisible = true
                    setOnCloseIconClickListener { viewModel.removeIngredient(ingredient) }
                }
                chipGroup.addView(chip)
            }
        }

        findButton.setOnClickListener {
            val ingredients = viewModel.ingredientsList.value
            if (!ingredients.isNullOrEmpty()) {
                val intent = Intent(activity, ScanResultsActivity::class.java)
                intent.putStringArrayListExtra("INGREDIENTS", ArrayList(ingredients))
                startActivity(intent)
            } else {
                Toast.makeText(context, getString(R.string.scan_manual_add_ingredients_first), Toast.LENGTH_SHORT).show()
            }
        }

        voiceButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.scan_manual_voice_prompt))
                // We can set the language to English, but it will often default to the user's system language
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            }
            try {
                voiceLauncher.launch(intent)
            } catch (e: Exception) {
                // This can happen if the device doesn't have a voice recognition service
                Toast.makeText(context, getString(R.string.scan_manual_voice_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}


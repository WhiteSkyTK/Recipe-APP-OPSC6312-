package com.rst.recipeappopsc6312

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class PreferencesActivity : AppCompatActivity() {
    private val viewModel: ShoppingViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }
    private val TAG = "PreferencesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences) // Use the new layout file
        enableEdgeToEdge()
        val preferencesLayout = findViewById<View>(R.id.preferences_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(preferencesLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val themeRadioGroup = findViewById<RadioGroup>(R.id.radioGroupTheme)
        val unitRadioGroup = findViewById<RadioGroup>(R.id.radioGroupUnits)
        val allCapsSwitch = findViewById<MaterialSwitch>(R.id.switchAllCaps)
        val editCountry = findViewById<TextView>(R.id.textViewEditCountry)
        val editCuisines = findViewById<TextView>(R.id.textViewEditCuisines)
        val editDiets = findViewById<TextView>(R.id.textViewEditDiets)


        loadCurrentSettings(themeRadioGroup, allCapsSwitch, unitRadioGroup)

        // --- Click Listeners ---
        backButton.setOnClickListener {
            finish() // Simply close this activity to go back
        }

        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                R.id.radioSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_NO // Default to Light
            }
            AppCompatDelegate.setDefaultNightMode(selectedMode)
            saveThemeSetting(selectedMode)
        }

        allCapsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveCapitalizationSetting(isChecked)
        }

        unitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedSystem = when (checkedId) {
                R.id.radioImperial -> UnitConverter.IMPERIAL
                else -> UnitConverter.METRIC
            }
            saveUnitSetting(selectedSystem)
        }

        // --- Navigation to Edit Preferences ---
        editCountry.setOnClickListener {
            val intent = Intent(this, CountrySelectionActivity::class.java)
            // Add a flag to tell the next screen it's in "edit mode"
            intent.putExtra("IS_EDIT_MODE", true)
            startActivity(intent)
        }

        editCuisines.setOnClickListener {
            val intent = Intent(this, CuisineSelectionActivity::class.java)
            intent.putExtra("IS_EDIT_MODE", true)
            startActivity(intent)
        }

        editDiets.setOnClickListener {
            val intent = Intent(this, DietarySelectionActivity::class.java)
            intent.putExtra("IS_EDIT_MODE", true)
            startActivity(intent)
        }
    }

    private fun loadCurrentSettings(radioGroup: RadioGroup, allCapsSwitch: MaterialSwitch, unitGroup: RadioGroup) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        // Default to Light Mode if no setting is saved
        val currentTheme = prefs.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_NO)
        val useAllCaps = prefs.getBoolean("UseAllCaps", true)

        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroup.check(R.id.radioDark)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> radioGroup.check(R.id.radioSystem)
            else -> radioGroup.check(R.id.radioLight)
        }
        allCapsSwitch.isChecked = useAllCaps

        val currentUnitSystem = prefs.getString("UnitSystem", UnitConverter.METRIC)
        when (currentUnitSystem) {
            UnitConverter.IMPERIAL -> unitGroup.check(R.id.radioImperial)
            else -> unitGroup.check(R.id.radioMetric)
        }
    }

    private fun saveThemeSetting(mode: Int) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putInt("ThemeMode", mode).apply()
        Log.d(TAG, "Theme setting saved: $mode")
    }

    private fun saveCapitalizationSetting(useAllCaps: Boolean) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("UseAllCaps", useAllCaps).apply()
        Log.d(TAG, "Capitalization setting saved: $useAllCaps")
    }

    private fun saveUnitSetting(system: String) {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putString("UnitSystem", system).apply()
    }
}
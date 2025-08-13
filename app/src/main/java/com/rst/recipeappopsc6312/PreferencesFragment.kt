package com.rst.recipeappopsc6312

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch

class PreferencesActivity : AppCompatActivity() {

    private val TAG = "PreferencesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences) // Use the new layout file

        val preferencesLayout = findViewById<View>(R.id.preferences_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(preferencesLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val themeRadioGroup = findViewById<RadioGroup>(R.id.radioGroupTheme)
        val allCapsSwitch = findViewById<MaterialSwitch>(R.id.switchAllCaps)
        val editCountry = findViewById<TextView>(R.id.textViewEditCountry)
        val editCuisines = findViewById<TextView>(R.id.textViewEditCuisines)
        val editDiets = findViewById<TextView>(R.id.textViewEditDiets)

        loadCurrentSettings(themeRadioGroup, allCapsSwitch)

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

    private fun loadCurrentSettings(radioGroup: RadioGroup, allCapsSwitch: MaterialSwitch) {
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
}
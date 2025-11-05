package com.rst.recipeappopsc6312

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutUsActivity : AppCompatActivity() {

    private val TAG = "AboutUsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        enableEdgeToEdge()
        val aboutUsLayout = findViewById<View>(R.id.about_us_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(aboutUsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        val versionTextView = findViewById<TextView>(R.id.textViewVersion)

        backButton.setOnClickListener {
            finish() // Closes the activity and returns to the previous screen
        }

        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            // Use the string resource and pass the version name into the placeholder
            versionTextView.text = getString(R.string.profile_version, versionName)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't get package info", e)
            // Use the same string resource with a fallback version number
            versionTextView.text = getString(R.string.profile_version, "1.0")
        }
    }
}
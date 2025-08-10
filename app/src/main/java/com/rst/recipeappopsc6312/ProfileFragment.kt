package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import de.hdodenhof.circleimageview.BuildConfig

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Find all the views
        val editProfileButton = view.findViewById<Button>(R.id.buttonEditProfile)
        val preferencesTextView = view.findViewById<TextView>(R.id.textViewPreferences)
        val languageTextView = view.findViewById<TextView>(R.id.textViewLanguage)
        val notificationsSwitch = view.findViewById<MaterialSwitch>(R.id.switchNotifications)
        val aboutUsTextView = view.findViewById<TextView>(R.id.textViewAboutUs)
        val reportIssueTextView = view.findViewById<TextView>(R.id.textViewReportIssue)
        val versionTextView = view.findViewById<TextView>(R.id.textViewVersion)

        // --- Load User Data ---
        // In a real app, you would fetch the user's data from Supabase and populate the views
        // For now, we'll use placeholders.
        view.findViewById<TextView>(R.id.textViewUserName).text = "Tokollo (TK)"
        view.findViewById<TextView>(R.id.textViewUserEmail).text = "tk@rstinnovations.com"

        // Set the app version text
        versionTextView.text = "Version ${BuildConfig.VERSION_NAME}"

        // --- Set Click Listeners ---

        editProfileButton.setOnClickListener {
            // Navigate to an "Edit Profile" activity where the user can change their name, pic, etc.
            Toast.makeText(context, "Edit Profile Clicked", Toast.LENGTH_SHORT).show()
        }

        preferencesTextView.setOnClickListener {
            // Navigate to a screen where the user can re-do the cuisine/diet selection
            Toast.makeText(context, "Preferences Clicked", Toast.LENGTH_SHORT).show()
        }

        languageTextView.setOnClickListener {
            // Show a dialog to let the user select a language
            Toast.makeText(context, "Language Clicked", Toast.LENGTH_SHORT).show()
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the user's notification preference to your backend
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(context, "Notifications $status", Toast.LENGTH_SHORT).show()
        }

        aboutUsTextView.setOnClickListener {
            // Navigate to an "About Us" screen
            Toast.makeText(context, "About Us Clicked", Toast.LENGTH_SHORT).show()
        }

        reportIssueTextView.setOnClickListener {
            // Open an email client or a form to report an issue
            Toast.makeText(context, "Report Issue Clicked", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.materialswitch.MaterialSwitch
import de.hdodenhof.circleimageview.BuildConfig
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Find all the views
        val profileImageView = view.findViewById<CircleImageView>(R.id.profile_image)
        val userNameTextView = view.findViewById<TextView>(R.id.textViewUserName)
        val userEmailTextView = view.findViewById<TextView>(R.id.textViewUserEmail)
        val editProfileButton = view.findViewById<Button>(R.id.buttonEditProfile)
        val preferencesTextView = view.findViewById<TextView>(R.id.textViewPreferences)
        val languageTextView = view.findViewById<TextView>(R.id.textViewLanguage)
        val notificationsSwitch = view.findViewById<MaterialSwitch>(R.id.switchNotifications)
        val aboutUsTextView = view.findViewById<TextView>(R.id.textViewAboutUs)
        val reportIssueTextView = view.findViewById<TextView>(R.id.textViewReportIssue)
        val versionTextView = view.findViewById<TextView>(R.id.textViewVersion)

        // --- Load User Data from Firebase ---
        loadUserProfile(profileImageView, userNameTextView, userEmailTextView)

        // Set the app version text
        try {
            val versionName = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
            versionTextView.text = "Version $versionName"
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't get package info", e)
            versionTextView.text = "Version 1.0"
        }

        // --- Set Click Listeners ---

        editProfileButton.setOnClickListener {
            // FIXED: Launch the new EditProfileActivity
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        preferencesTextView.setOnClickListener {
            // FIXED: Launch the new PreferencesActivity
            val intent = Intent(activity, PreferencesActivity::class.java)
            startActivity(intent)
        }

        aboutUsTextView.setOnClickListener {
            // FIXED: Launch the new AboutUsActivity
            val intent = Intent(activity, AboutUsActivity::class.java)
            startActivity(intent)
        }

        // --- Disabled Features ---
        languageTextView.alpha = 0.5f // Make it look disabled
        reportIssueTextView.alpha = 0.5f
        notificationsSwitch.isEnabled = false

        languageTextView.setOnClickListener {
            //TODO
            Toast.makeText(context, "Language selection is coming soon!", Toast.LENGTH_SHORT).show()
        }

        reportIssueTextView.setOnClickListener {
            //TODO
            Toast.makeText(context, "Issue reporting is coming soon!", Toast.LENGTH_SHORT).show()
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save the user's notification preference to your backend or local storage
            Toast.makeText(context, "Notification settings are coming soon!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadUserProfile(imageView: CircleImageView, nameView: TextView, emailView: TextView) {
        val user = FirebaseManager.auth.currentUser
        if (user != null) {
            emailView.text = user.email // Email is available directly from Auth

            // Fetch the rest of the profile from Firestore
            FirebaseManager.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d(TAG, "User profile data found in Firestore.")
                        val fullName = document.getString("full_name")
                        val profileImageUrl = document.getString("profileImageUrl")

                        nameView.text = fullName ?: "Your Name"

                        // Use Glide to load the profile picture
                        if (profileImageUrl != null) {
                            Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(imageView)
                        }
                    } else {
                        Log.d(TAG, "No profile document found in Firestore.")
                        nameView.text = "Your Name"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting user profile from Firestore.", exception)
                    nameView.text = "Your Name"
                }
        }
    }
}
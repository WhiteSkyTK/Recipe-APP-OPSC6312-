package com.rst.recipeappopsc6312

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.materialswitch.MaterialSwitch
import de.hdodenhof.circleimageview.BuildConfig
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"
    private val mainViewModel: MainViewModel by activityViewModels() // Use the shared MainViewModel

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
        val myRecipesButton = view.findViewById<Button>(R.id.buttonMyRecipes)
        val logOutButton = view.findViewById<Button>(R.id.buttonLogOut)
        val badgesTextView = view.findViewById<TextView>(R.id.textViewBadges)

        // --- Load User Data from Firebase ---
        loadUserProfile(profileImageView, userNameTextView, userEmailTextView)

        logOutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.profile_logout_title))
                .setMessage(getString(R.string.profile_logout_message))
                .setPositiveButton(getString(R.string.profile_logout_button)) { _, _ ->
                    logoutUser()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Set the app version text
        try {
            val versionName = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
            versionTextView.text = getString(R.string.profile_version, versionName)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't get package info", e)
            versionTextView.text = getString(R.string.profile_version, "1.0")
        }

        // --- Set Click Listeners ---
        myRecipesButton.setOnClickListener {
            startActivity(Intent(activity, MyRecipesActivity::class.java))
        }
        editProfileButton.setOnClickListener {
            startActivity(Intent(activity, EditProfileActivity::class.java))
        }
        preferencesTextView.setOnClickListener {
            startActivity(Intent(activity, PreferencesActivity::class.java))
        }
        aboutUsTextView.setOnClickListener {
            startActivity(Intent(activity, AboutUsActivity::class.java))
        }
        badgesTextView.setOnClickListener {
            startActivity(Intent(activity, BadgesActivity::class.java))
        }
        languageTextView.setOnClickListener {
            showLanguageSelectionDialog()
        }
        reportIssueTextView.setOnClickListener {
            sendReportEmail()
        }

        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        notificationsSwitch.isChecked = prefs.getBoolean("NotificationsEnabled", true)
        notificationsSwitch.isEnabled = true

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("NotificationsEnabled", isChecked).apply()
            mainViewModel.updateNotificationSubscription(isChecked)
        }

        return view
    }

    private fun sendReportEmail() {
        val recipient = getString(R.string.report_issue_email_address)
        val subject = getString(R.string.report_issue_subject)

        // Gather useful debug info
        val appVersion = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) { "N/A" }
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val userId = FirebaseManager.auth.currentUser?.uid ?: "Not Logged In"

        val body = getString(R.string.report_issue_body, appVersion, deviceModel, androidVersion, userId)

        // Create the email intent
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.report_issue_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("🇬🇧 English", "🇿🇦 Sepedi", "🇿🇦 Tsonga", "🇿🇦 Venda")
        val languageCodes = arrayOf("en", "nso", "ts", "ve")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                setAppLocale(selectedLanguageCode)
                dialog.dismiss()
            }
            .show()
    }

    private fun setAppLocale(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
        // The activity needs to be recreated for the language change to take full effect.
        activity?.recreate()
    }

    private fun logoutUser() {
        Log.d(TAG, "Logging out user and clearing all data.")

        // 1. Sign out from Firebase
        FirebaseManager.auth.signOut()

        // 2. Clear all SharedPreferences
        val appSettingsPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val appCachePrefs = requireActivity().getSharedPreferences("AppCache", Context.MODE_PRIVATE)
        val favoritesPrefs = requireActivity().getSharedPreferences("FavoritePrefs", Context.MODE_PRIVATE)
        appSettingsPrefs.edit().clear().apply()
        appCachePrefs.edit().clear().apply()
        favoritesPrefs.edit().clear().apply()
        Log.d(TAG, "All SharedPreferences cleared.")

        // 3. Clear the Room database in a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.getDatabase(requireContext()).clearAllTables()
                Log.d(TAG, "Room database cleared successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Room database.", e)
            }
        }

        // 4. Navigate to the Login screen and clear the back stack
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish() // Close MainActivity
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

                        nameView.text = fullName ?: getString(R.string.profile_default_name)

                        // Use Glide to load the profile picture
                        if (profileImageUrl != null) {
                            Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(imageView)
                        }
                    } else {
                        Log.d(TAG, "No profile document found in Firestore.")
                        nameView.text = getString(R.string.profile_default_name)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting user profile from Firestore.", exception)
                    nameView.text = getString(R.string.profile_default_name)
                }
        }
    }
}
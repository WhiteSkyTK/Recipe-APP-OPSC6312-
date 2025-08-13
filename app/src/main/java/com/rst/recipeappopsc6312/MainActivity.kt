package com.rst.recipeappopsc6312

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private var currentFragmentId = R.id.nav_home
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        greetingTextView = findViewById(R.id.textViewGreeting)

        enableEdgeToEdge()
        val mainLayout = findViewById<View>(R.id.main_content_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        // Find all the views
        val greetingTextView = findViewById<TextView>(R.id.textViewGreeting)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val favoritesButton = findViewById<ImageView>(R.id.buttonFavorites)
        val notificationButton = findViewById<ImageView>(R.id.buttonNotifications)
        val addRecipeButton = findViewById<ImageView>(R.id.buttonAddRecipe)

        // Set the dynamic greeting
        setGreeting()

        // Load the HomeFragment by default when the app starts
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.nav_home)
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentFragmentId) return@setOnItemSelectedListener false

            val newFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_discover -> DiscoverFragment()
                R.id.nav_scan -> ScanFragment() // Scan is now a regular fragment
                R.id.nav_cart -> ShoppingListFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            if (newFragment != null) {
                loadFragment(newFragment, item.itemId)
            }
            true
        }

        addRecipeButton.setOnClickListener {
            Log.d(TAG, "Add Recipe button clicked.")
            loadFragment(AddRecipeFragment(), -2) // Use a unique ID for top nav
        }

        favoritesButton.setOnClickListener {
            Log.d(TAG, "Favorites button clicked.")
            handleFavoritesClick()
        }

        notificationButton.setOnClickListener {
            Log.d(TAG, "Notifications button clicked.")
            showNotificationsPopup()
        }
    }

    override fun onResume() {
        super.onResume()
        // This function is called every time the user returns to this screen
        setGreeting()
    }

    private fun setGreeting() {
        val user = FirebaseManager.auth.currentUser
        if (user != null) {
            FirebaseManager.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    var fullName = document.getString("full_name") ?: "USER"

                    val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                    val useAllCaps = prefs.getBoolean("UseAllCaps", true)

                    fullName = if (useAllCaps) {
                        fullName.uppercase()
                    } else {
                        fullName.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar(Char::titlecase) }
                    }

                    val calendar = Calendar.getInstance()
                    val greetingText = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                        in 0..11 -> "☀️ Good Morning"
                        in 12..17 -> "🌤️ Good Afternoon"
                        else -> "🌙 Good Evening"
                    }
                    greetingTextView.text = "$greetingText\n$fullName"
                }
                .addOnFailureListener {
                    greetingTextView.text = "☀️ Good Morning\nUSER"
                }
        }
    }

    internal fun loadFragment(fragment: Fragment, newFragmentId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        val currentPosition = getFragmentPosition(currentFragmentId)
        val newPosition = getFragmentPosition(newFragmentId)

        if (newFragmentId < 0) { // Coming from top nav
            transaction.setCustomAnimations(R.anim.slide_in_top, 0, 0, R.anim.slide_out_top)
            transaction.addToBackStack(null) // Add to back stack for top nav
        } else if (newPosition > currentPosition) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
        currentFragmentId = newFragmentId
    }

    private fun handleFavoritesClick() {
        // In a real app, you would fetch this from Firestore
        val favoriteRecipes = DummyData.getRecommendedRecipes().filter { it.isFavorite }

        if (favoriteRecipes.isEmpty()) {
            // If no favorites, show the popup
            val favoritesButton = findViewById<ImageView>(R.id.buttonFavorites)
            showEmptyPopup(favoritesButton, "You have no favorites yet!")
        } else {
            // If there are favorites, go directly to the fragment
            loadFragment(FavoritesFragment(), -1)
        }
    }
    private fun showNotificationsPopup() {
        val recentNotifications = DummyData.getNotifications() // Get fake data
        val notificationButton = findViewById<ImageView>(R.id.buttonNotifications)

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_notifications, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.animationStyle = R.style.PopupAnimation

        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recyclerViewPopupNotifications)
        val viewAll = popupView.findViewById<TextView>(R.id.textViewViewAll)
        val noItemsTextView = popupView.findViewById<TextView>(R.id.textViewNoItems)

        if (recentNotifications.isEmpty()) {
            noItemsTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            viewAll.visibility = View.GONE
            noItemsTextView.text = "No new notifications"
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            // recyclerView.adapter = NotificationAdapter(recentNotifications.take(2))
        }

        viewAll.setOnClickListener {
            loadFragment(NotificationsFragment(), -1)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(notificationButton)
    }

    private fun showEmptyPopup(anchor: View, message: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_notifications, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.animationStyle = R.style.PopupAnimation

        popupView.findViewById<RecyclerView>(R.id.recyclerViewPopupNotifications).visibility = View.GONE
        popupView.findViewById<TextView>(R.id.textViewViewAll).visibility = View.GONE
        val noItemsTextView = popupView.findViewById<TextView>(R.id.textViewNoItems)
        noItemsTextView.visibility = View.VISIBLE
        noItemsTextView.text = message

        popupWindow.showAsDropDown(anchor)
    }

    private fun getFragmentPosition(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_home -> 0
            R.id.nav_discover -> 1
            R.id.nav_scan -> 2 // For the FAB/Scan
            R.id.nav_cart -> 3
            R.id.nav_profile -> 4
            else -> -1
        }
    }
}
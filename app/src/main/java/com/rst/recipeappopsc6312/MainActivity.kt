package com.rst.recipeappopsc6312

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
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

    private var currentFragmentId = R.id.nav_home
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        setGreeting(greetingTextView)

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
            loadFragment(AddRecipeFragment(), -2) // Use a unique ID for top nav
        }

        favoritesButton.setOnClickListener {
            showFavoritesPopup()
        }

        notificationButton.setOnClickListener {
            showNotificationsPopup()
        }
    }

    private fun setGreeting(greetingTextView: TextView) {
        val user = FirebaseManager.auth.currentUser
        if (user != null) {
            // Fetch user's profile from Firestore
            FirebaseManager.firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val fullName = document.getString("full_name")?.uppercase() ?: "USER"
                    val calendar = Calendar.getInstance()
                    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
                        in 0..11 -> "☀️ Good Morning"
                        in 12..17 -> "🌤️ Good Afternoon"
                        else -> "🌙 Good Evening"
                    }
                    greetingTextView.text = "$greeting\n$fullName"
                }
                .addOnFailureListener {
                    // Fallback if Firestore fails
                    greetingTextView.text = "☀️ Good Morning\nUSER"
                }
        }
    }

    internal fun loadFragment(fragment: Fragment, newFragmentId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        val currentPosition = getFragmentPosition(currentFragmentId)
        val newPosition = getFragmentPosition(newFragmentId)

        // Determine animation based on source and destination
        if (newFragmentId < 0) { // Coming from top nav
            transaction.setCustomAnimations(R.anim.slide_in_top, 0)
        } else if (newPosition > currentPosition) { // Moving right on bottom nav
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else { // Moving left on bottom nav
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
        currentFragmentId = newFragmentId
    }

    private fun showNotificationsPopup() {
        val recentNotifications = DummyData.getNotifications() // Get fake data
        val notificationButton = findViewById<ImageView>(R.id.buttonNotifications)
        showPopup(notificationButton, recentNotifications, NotificationsFragment())
    }
    private fun showFavoritesPopup() {
        // In a real app, you would fetch only favorited recipes
        val favoriteRecipes = DummyData.getRecommendedRecipes().filter { it.isFavorite }
        val favoritesButton = findViewById<ImageView>(R.id.buttonFavorites)
        showPopup(favoritesButton, favoriteRecipes, FavoritesFragment())
    }

    private fun <T> showPopup(anchor: View, items: List<T>, targetFragment: Fragment) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_notifications, null)

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.animationStyle = R.style.PopupAnimation // Apply bubble animation

        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recyclerViewPopupNotifications)
        val viewAll = popupView.findViewById<TextView>(R.id.textViewViewAll)
        val noItemsTextView = popupView.findViewById<TextView>(R.id.textViewNoNotifications)

        if (items.isEmpty()) {
            noItemsTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            viewAll.visibility = View.GONE
            noItemsTextView.text = if (targetFragment is FavoritesFragment) "No favorites yet" else "No new notifications"
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            // Here you would have a generic adapter or separate ones
            // For now, we'll just log it.
            Log.d(TAG, "Showing ${items.size} items in popup.")
        }

        viewAll.setOnClickListener {
            loadFragment(targetFragment, -1)
            popupWindow.dismiss()
        }

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
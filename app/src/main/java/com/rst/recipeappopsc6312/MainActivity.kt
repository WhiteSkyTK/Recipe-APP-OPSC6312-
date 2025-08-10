package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find all the views
        val greetingTextView = findViewById<TextView>(R.id.textViewGreeting)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val favoritesButton = findViewById<ImageView>(R.id.buttonFavorites)
        val notificationButton = findViewById<ImageView>(R.id.buttonNotifications)

        // Set the dynamic greeting
        greetingTextView.text = getGreeting()

        // Load the HomeFragment by default when the app starts
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.nav_home
        }

        // --- Click Listeners ---

        // Handle clicks on the bottom navigation items
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_discover -> loadFragment(DiscoverFragment())
                R.id.nav_cart -> loadFragment(ShoppingListFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            // Prevent the placeholder from being visually selected
            item.itemId != R.id.nav_placeholder
        }

        // Handle the click on the central FloatingActionButton (FAB)
        fab.setOnClickListener {
            loadFragment(ScanFragment())
            // Deselect any item in the bottom nav to show the FAB is the active screen
            bottomNav.selectedItemId = R.id.nav_placeholder
        }

        // Handle the click on the top-right Favorites button
        favoritesButton.setOnClickListener {
            // Here you would navigate to a new Activity or Fragment that shows a list of favorite recipes
            Toast.makeText(this, "Favorites clicked!", Toast.LENGTH_SHORT).show()
        }

        notificationButton.setOnClickListener {
            // This is where you would call the function to show your notification popup
            showNotificationsPopup()
            Toast.makeText(this, "Notifications clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        // In a real app, you would get the user's actual name from your database
        val userName = "EBMTС"
        val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "☀️ Good Morning"
            in 12..17 -> "🌤️ Good Afternoon"
            else -> "🌙 Good Evening"
        }
        return "$greeting\n$userName"
    }

    internal fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showNotificationsPopup() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_notifications, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // Makes it dismissable
        )

        // Setup the RecyclerView inside the popup
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.recyclerViewPopupNotifications)
        // ... setup adapter with only the 2 most recent notifications ...

        // Handle "View All" click
        popupView.findViewById<TextView>(R.id.textViewViewAll).setOnClickListener {
            loadFragment(NotificationsFragment())
            popupWindow.dismiss()
        }

        // Show the popup anchored to the notification bell
        val notificationButton = findViewById<ImageView>(R.id.buttonNotifications)
        popupWindow.showAsDropDown(notificationButton)
    }
}
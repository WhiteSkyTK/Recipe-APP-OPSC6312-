package com.rst.recipeappopsc6312

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import android.os.Handler
import com.github.amlcurran.showcaseview.OnShowcaseEventListener


class MainActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var favoritesButton: ImageView
    private lateinit var notificationButton: ImageView
    private lateinit var addRecipeButton: ImageView
    private var currentFragmentId = R.id.nav_home
    private var hasFavorites = false
    private var lastBottomNavFragmentId = R.id.nav_home
    private val TAG = "MainActivity"
    private val CURRENT_FRAGMENT_KEY = "CURRENT_FRAGMENT_KEY"
    private val LAST_BOTTOM_NAV_FRAGMENT_KEY = "LAST_BOTTOM_NAV_FRAGMENT_KEY"

    private val mainViewModel: MainViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(),
            db.recipeDao(),
            db.scanHistoryDao(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance())
        MainViewModelFactory(repo)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
        val mainLayout = findViewById<View>(R.id.main_content_layout) // Add this ID to your root layout in XML

        // This is the correct way to handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // We handle bottom padding with the nav bar
            insets
        }

        // Find all the views
        greetingTextView = findViewById(R.id.textViewGreeting)
        favoritesButton = findViewById(R.id.buttonFavorites)
        notificationButton = findViewById(R.id.buttonNotifications)
        addRecipeButton = findViewById(R.id.buttonAddRecipe)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Set the dynamic greeting
        setGreeting()

        // Handle the initial intent when the activity is first created
        handleIncomingIntent(intent)

        // Load the HomeFragment by default when the app starts
        if (savedInstanceState != null) {
            // If the activity is being recreated, restore the saved fragment ID
            currentFragmentId = savedInstanceState.getInt(CURRENT_FRAGMENT_KEY, R.id.nav_home)
        } else {
            // If it's a fresh start, load the HomeFragment
            loadFragment(HomeFragment(), R.id.nav_home)
        }

        // Check if the currentFragmentId is a valid bottom nav item before setting
        if (getFragmentPosition(currentFragmentId) != -1) {
            bottomNav.selectedItemId = currentFragmentId
        }

        if (isFirstLaunch()) {
            // Add a half-second delay before showing the tour
            Handler(Looper.getMainLooper()).postDelayed({
                showOnboardingTour()
            }, 500) // 500 milliseconds
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
                // ++ UPDATE THE LAST BOTTOM NAV ID when a bottom nav item is clicked
                lastBottomNavFragmentId = item.itemId
            }
            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            // When the back stack changes, find the currently visible fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            currentFragment?.let {
                // Update the icon colors based on which fragment is now on top
                updateTopNavIcons(it)

                // Also update the currentFragmentId to keep the slide animation logic correct
                // Find the ID associated with this fragment instance
                val newId = getFragmentId(it)
                if (newId != -99) { // Use a magic number to indicate not found
                    currentFragmentId = newId
                    if (getFragmentPosition(newId) != -1) {
                        lastBottomNavFragmentId = newId
                    }
                }
            }
        }

        addRecipeButton.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            // If the AddRecipeFragment is already showing, go back. Otherwise, load it.
            if (currentFragment is AddRecipeFragment) {
                supportFragmentManager.popBackStack()
            } else {
                loadFragment(AddRecipeFragment(), -2)
            }
        }

        // In MainActivity.kt -> onCreate()
        mainViewModel.allFavorites.observe(this) { favorites ->
            hasFavorites = !favorites.isNullOrEmpty()
        }
        // favoritesButton OnClickListener
        favoritesButton.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is FavoritesFragment) {
                supportFragmentManager.popBackStack()
            } else {
                // Now, it just checks the boolean variable instantly.
                if (hasFavorites) {
                    loadFragment(FavoritesFragment(), -1)
                } else {
                    showEmptyPopup(favoritesButton, "You have no favorites yet!")
                }
            }
        }

        notificationButton.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            // If the NotificationsFragment is already showing, go back.
            if (currentFragment is NotificationsFragment) {
                supportFragmentManager.popBackStack()
            } else {
                // Otherwise, just show the popup as before.
                // The popup's "View All" button will handle opening the full fragment.
                showNotificationsPopup()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_FRAGMENT_KEY, currentFragmentId)
    }

    override fun onResume() {
        super.onResume()
        setGreeting()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
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
                        in 2..11 -> "☀️ Good Morning"
                        in 12..17 -> "🌤️ Good Afternoon"
                        else -> "🌙 Good Evening"
                    }
                    val fullGreeting = "$greetingText\n$fullName"

                    // ++ Animate the text change ++
                    if (greetingTextView.text != fullGreeting) {
                        greetingTextView.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                greetingTextView.text = fullGreeting
                                greetingTextView.animate().alpha(1f).duration = 200
                            }
                    }
                }
                .addOnFailureListener {
                    greetingTextView.text = "☀️ Good Morning\nUSER"
                }
        }
    }

    internal fun loadFragment(fragment: Fragment, newFragmentId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        val newPosition = getFragmentPosition(newFragmentId)

        if (newPosition < 0) { // It's a top-nav fragment
            transaction.setCustomAnimations(R.anim.slide_in_top, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_top)
            transaction.addToBackStack(null)
        } else { // It's a bottom-nav fragment, calculate slide based on LAST bottom nav position
            val lastBottomNavPosition = getFragmentPosition(lastBottomNavFragmentId)

            if (newPosition > lastBottomNavPosition) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            } else if (newPosition < lastBottomNavPosition) {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            // No animation if it's the same position, which is already handled by the listener
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()

        // Update the current and last positions
        currentFragmentId = newFragmentId
        if (newPosition >= 0) { // Only update lastBottomNavFragmentId if it's a bottom nav item
            lastBottomNavFragmentId = newFragmentId
        }
        updateTopNavIcons(fragment)
    }

    private fun updateTopNavIcons(activeFragment: Fragment) {
        val activeColor = ContextCompat.getColor(this, R.color.primary_blue)
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColor, typedValue, true)
        val inactiveColorFromTheme = typedValue.data // This will be dark in light theme, light in dark theme

        // Set the favorites icon color
        favoritesButton.imageTintList = if (activeFragment is FavoritesFragment) {
            ColorStateList.valueOf(activeColor)
        } else {
            ColorStateList.valueOf(inactiveColorFromTheme)
        }

        // Set the add recipe icon color
        addRecipeButton.imageTintList = if (activeFragment is AddRecipeFragment) {
            ColorStateList.valueOf(activeColor)
        } else {
            ColorStateList.valueOf(inactiveColorFromTheme)
        }

        // Set the notifications icon color
        notificationButton.imageTintList = if (activeFragment is NotificationsFragment) {
            ColorStateList.valueOf(activeColor)
        } else {
            ColorStateList.valueOf(inactiveColorFromTheme)
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
            recyclerView.adapter = NotificationAdapter(recentNotifications.take(2))
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

    private fun getFragmentId(fragment: Fragment): Int {
        return when(fragment) {
            is HomeFragment -> R.id.nav_home
            is DiscoverFragment -> R.id.nav_discover
            is ScanFragment -> R.id.nav_scan
            is ShoppingListFragment -> R.id.nav_cart
            is ProfileFragment -> R.id.nav_profile
            is FavoritesFragment -> -1 // Top nav items
            is AddRecipeFragment -> -2
            is NotificationsFragment -> -3
            else -> -99 // Not a recognized fragment
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean("isFirstLaunch", true)
        if (isFirst) {
            // If it is the first launch, update the flag to false for next time
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
        }
        return isFirst
    }

    private fun showOnboardingTour() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 1. Define your tour steps as a list of data: Target View, Title, Text
        // We use a list of functions that return a ShowcaseView.Builder if the target view is found.
        val tourSteps = mutableListOf<() -> ShowcaseView.Builder?>()

        // Helper to add steps, ensuring view is not null
        fun addTourStep(viewId: Int, title: String, text: String, isBottomNavItem: Boolean = true) {
            val targetView: View? = if (isBottomNavItem) {
                bottomNav.findViewById(viewId)
            } else {
                findViewById(viewId)
            }

            if (targetView != null) {
                tourSteps.add {
                    ShowcaseView.Builder(this@MainActivity)
                        .setTarget(ViewTarget(targetView))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(R.style.CustomShowcaseTheme) // Optional: Your custom theme
                }
            } else {
                Log.w(TAG, "Showcase target view not found for ID: $viewId")
            }
        }

        // 2. Define the steps using the helper
        addTourStep(
            R.id.nav_home, // This is a menu item ID within your bottom_nav_menu.xml
            "Welcome to Hamory Kitchen!",
            "This is your Home screen, where you'll find daily inspiration."
        )
        addTourStep(
            R.id.nav_discover,
            "Discover New Recipes",
            "Explore a world of public recipes shared by the community."
        )
        addTourStep(
            R.id.buttonAddRecipe, // This is an ImageView ID in your activity_main.xml
            "Add Your Creations",
            "Tap the '+' button anytime to add your own recipes.",
            isBottomNavItem = false // Explicitly state it's not a bottom nav item
        )
        addTourStep(
            R.id.nav_scan,
            "Scan Ingredients",
            "Use your camera to find recipes based on what's in your kitchen."
        )
        addTourStep(
            R.id.nav_cart,
            "Shopping Lists",
            "Manage your grocery lists for different recipes here."
        )
        addTourStep(
            R.id.nav_profile,
            "Your Profile",
            "View your own creations, favorites, and manage app settings."
        )

        // 3. Start the sequence if there are any valid steps
        if (tourSteps.isNotEmpty()) {
            showShowcaseAtIndex(tourSteps, 0)
        }
    }

    private fun showShowcaseAtIndex(
        tourSteps: List<() -> ShowcaseView.Builder?>,
        currentIndex: Int
    ) {
        if (currentIndex >= tourSteps.size) {
            // Tour finished
            return
        }

        // Get the builder function for the current step and execute it
        val builder = tourSteps[currentIndex]()

        // If the builder is null (meaning the target view wasn't found), skip to the next
        if (builder == null) {
            showShowcaseAtIndex(tourSteps, currentIndex + 1)
            return
        }

        builder.setShowcaseEventListener(object : OnShowcaseEventListener {
            override fun onShowcaseViewHide(showcaseView: ShowcaseView) {
                // Placeholder, not typically used for sequencing
            }

            override fun onShowcaseViewDidHide(showcaseView: ShowcaseView) {
                // When the current showcase is hidden, show the next one
                showShowcaseAtIndex(tourSteps, currentIndex + 1)
            }

            override fun onShowcaseViewShow(showcaseView: ShowcaseView) {
                // Placeholder
            }

            override fun onShowcaseViewTouchBlocked(motionEvent: android.view.MotionEvent) {
                // Placeholder
            }
        })
            .build() // This builds and shows the ShowcaseView
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "ADD_RECIPE_FRAGMENT") {
            val recipeId = intent.getStringExtra("EDIT_RECIPE_ID")
            val editFragment = AddRecipeFragment().apply {
                arguments = Bundle().apply {
                    putString("EDIT_RECIPE_ID", recipeId)
                }
            }
            loadFragment(editFragment, -2) // Use the AddRecipe ID
        }
    }
}
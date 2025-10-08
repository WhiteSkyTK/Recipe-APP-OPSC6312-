package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class BadgesActivity : AppCompatActivity() {

    private val viewModel: BadgesViewModel by viewModels {
        val db = AppDatabase.getDatabase(application)
        val repo = ShoppingRepository(
            db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(),
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance()
        )
        ViewModelFactory(repo)
    }

    private lateinit var badgesAdapter: BadgesAdapter
    private lateinit var loadingIndicator: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_badges)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.badges_activity_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<ImageView>(R.id.imageViewBack)
        backButton.setOnClickListener {
            finish() // Simply close the activity
        }

        loadingIndicator = findViewById(R.id.loadingIndicator)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewBadges)

        badgesAdapter = BadgesAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = badgesAdapter

        observeViewModel()
        viewModel.loadBadges()
    }

    private fun observeViewModel() {
        viewModel.badgeList.observe(this) { badges ->
            badgesAdapter.updateData(badges)
        }
        viewModel.isLoading.observe(this) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}

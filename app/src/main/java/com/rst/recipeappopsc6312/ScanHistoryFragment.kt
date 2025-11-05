package com.rst.recipeappopsc6312

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScanHistoryFragment : Fragment() {

    private val viewModel: ScanViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val repo = ShoppingRepository(db.shoppingDao(), db.recipeDao(), db.scanHistoryDao(), com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.storage.FirebaseStorage.getInstance())
        ViewModelFactory(repo)
    }
    private lateinit var historyAdapter: ScanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_history, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewHistory)

        historyAdapter = ScanHistoryAdapter(emptyList()) { clickedItem ->
            val intent = Intent(activity, ScanResultsActivity::class.java)
            intent.putStringArrayListExtra("INGREDIENTS", ArrayList(clickedItem.ingredients))
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = historyAdapter

        viewModel.scanHistory.observe(viewLifecycleOwner) { history ->
            historyAdapter.updateData(history)
        }

        return view
    }
}


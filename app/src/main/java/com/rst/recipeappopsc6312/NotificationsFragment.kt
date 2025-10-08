package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var noNotificationsTextView: TextView

    // ++ Use activityViewModels to get the SHARED MainViewModel from MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewAllNotifications)
        noNotificationsTextView = view.findViewById(R.id.textViewNoNotifications) // Add this ID to your fragment_notifications.xml

        // Set up the adapter with an empty list initially
        notificationAdapter = NotificationAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = notificationAdapter

        observeViewModel()

        // Fetch the notifications when the fragment is created
        mainViewModel.fetchNotifications()

        return view
    }

    private fun observeViewModel() {
        mainViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                noNotificationsTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                noNotificationsTextView.visibility = View.GONE
                notificationAdapter.updateData(notifications) // Create this helper function in your adapter
            }
        }
    }
}

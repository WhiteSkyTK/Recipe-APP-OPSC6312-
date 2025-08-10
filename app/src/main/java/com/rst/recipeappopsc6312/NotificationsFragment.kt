package com.rst.recipeappopsc6312

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private var notificationList = ArrayList<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewAllNotifications)

        // In a real app, you would fetch this data from Supabase or another backend.
        prepareDummyNotifications()

        // Set up the adapter and the RecyclerView
        notificationAdapter = NotificationAdapter(notificationList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = notificationAdapter

        return view
    }

    private fun prepareDummyNotifications() {
        // You will need to add these icons (e.g., ic_alert, ic_new_recipe) to your drawable folder.
        notificationList.add(Notification("New Update Available", "Today / 10:52 PM", R.drawable.ic_alert))
        notificationList.add(Notification("New Recipes Added", "Yesterday / 04:01 PM", R.drawable.ic_new_recipe))
        notificationList.add(Notification("Your weekly report is ready", "2 days ago", R.drawable.ic_report))
        notificationList.add(Notification("A friend liked your recipe!", "3 days ago", R.drawable.ic_heart_filled))
    }
}
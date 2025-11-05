package com.rst.recipeappopsc6312

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_Service"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            // 1. Show the system tray notification to the user
            sendNotification(notification.title ?: "New Recipe Alert", notification.body ?: "Check it out!")

            // --- ** NOTIFICATION CHANGE ** ---
            // 2. Also save a copy of this notification to the user's private collection in Firestore.
            // This ensures it appears in their in-app notification history.
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                val notificationData = mapOf(
                    "title" to (notification.title ?: "New Notification"),
                    "message" to (notification.body ?: "Check it out!"),
                    "iconName" to "ic_notification", // A default icon for push notifications
                    "isRead" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                Firebase.firestore.collection("users").document(userId).collection("notifications")
                    .add(notificationData)
                    .addOnSuccessListener { Log.d(TAG, "Notification saved to user's private collection.") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error saving notification to user's collection.", e) }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            val tokenData = hashMapOf("fcmToken" to token)
            Firebase.firestore.collection("users").document(userId)
                .update(tokenData as Map<String, Any>)
                .addOnSuccessListener { Log.d(TAG, "FCM token successfully updated for user: $userId") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = "recipe_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Recipe Updates",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}


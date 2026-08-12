package com.example.dingtoneclone.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.dingtoneclone.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DingtoneFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "New SMS"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "You have a new message"
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // TODO: Send token to backend to enable push notifications
        super.onNewToken(token)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "dingtone_sms"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "SMS Notifications", NotificationManager.IMPORTANCE_HIGH)
        nm.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}

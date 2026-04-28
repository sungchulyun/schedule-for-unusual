package com.example.scheduleapp.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.scheduleapp.MainActivity
import com.example.scheduleapp.R
import com.example.scheduleapp.data.AuthSessionManager
import com.example.scheduleapp.data.CalendarRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScheduleFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AuthSessionManager.initialize(applicationContext)
        ScheduleNotificationChannels.ensureCreated(this)
    }

    override fun onNewToken(token: String) {
        if (AuthSessionManager.getSession() == null) {
            return
        }

        serviceScope.launch {
            runCatching {
                CalendarRepository().registerFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!canPostNotifications()) {
            return
        }

        val title = message.notification?.title
            ?: message.data["title"]
            ?: defaultTitle(message.data["type"])
        val body = message.notification?.body
            ?: message.data["body"]
            ?: defaultBody(message.data)

        showNotification(title, body)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ScheduleNotificationChannels.ScheduleUpdates)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun defaultTitle(type: String?): String {
        return when (type) {
            "DAILY_SCHEDULE_SUMMARY" -> "오늘 일정"
            else -> "일정 알림"
        }
    }

    private fun defaultBody(data: Map<String, String>): String {
        return when (data["type"]) {
            "SCHEDULE_CHANGED" -> "일정이 변경되었습니다."
            "DAILY_SCHEDULE_SUMMARY" -> "오늘 일정을 확인해 주세요."
            else -> "LinkTogether 알림이 도착했습니다."
        }
    }
}

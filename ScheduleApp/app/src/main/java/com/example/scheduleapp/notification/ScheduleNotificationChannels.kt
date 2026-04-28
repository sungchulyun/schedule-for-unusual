package com.example.scheduleapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.scheduleapp.R

object ScheduleNotificationChannels {
    const val ScheduleUpdates = "schedule_updates"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ScheduleUpdates,
            context.getString(R.string.notification_channel_schedule_updates),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_schedule_updates_description)
        }
        manager.createNotificationChannel(channel)
    }
}

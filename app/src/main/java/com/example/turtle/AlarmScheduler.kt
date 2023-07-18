package com.example.turtle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent


class AlarmScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule() {
        val intent = Intent(context, AlarmReceiver::class.java)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            5000,
            60000,
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    fun cancel() {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
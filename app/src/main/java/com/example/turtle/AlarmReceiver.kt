package com.example.turtle

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            handleAlarm(context, it)
        } ?: return
    }

    private fun handleAlarm(context: Context?, intent: Intent) = CoroutineScope(SupervisorJob()).launch {
        val billRepository = (context?.applicationContext as TurtleApplication).billRepository
        val userId = (context.applicationContext as TurtleApplication).userId
        val userDebt = billRepository.getUserDebt(userId!!)
        userDebt?.run {
            createNotification(context, userDebt)
        }
    }

    private fun createNotification(context: Context, userDebt: Pair<String, Int>) {
        val title = "Go settle your debts"
        val description = "You have a debt of ${userDebt.first} " +
                "from ${userDebt.second} ${if(userDebt.second > 1) "bills" else "bill"}"

        val intent = Intent(context, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "debts")
            .setSmallIcon(R.drawable.turtle)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0, notification)
    }
}
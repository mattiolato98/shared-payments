package com.example.turtle

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.turtle.data.AuthRepository
import com.example.turtle.data.BillRepository

class TurtleApplication: Application() {
    lateinit var billRepository: BillRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    var userId: String? = null
        private set
    var userEmail: String? = null
        private set

    var alarmScheduler: AlarmScheduler? = null
        private set

    override fun onCreate() {
        super.onCreate()
        billRepository = BillRepository()
        authRepository = AuthRepository()
        createNotificationChannel()
    }

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun setUserEmail(email: String?) {
        this.userEmail = email
    }

    fun setAlarmScheduler(alarmScheduler: AlarmScheduler) {
        this.alarmScheduler = alarmScheduler
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Debts reminder"
            val descriptionText = "This notification channel reminds you your pending debts, once per day"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("debts", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
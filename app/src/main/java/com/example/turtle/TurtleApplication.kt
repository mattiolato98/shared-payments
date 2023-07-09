package com.example.turtle

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        billRepository = BillRepository()
        authRepository = AuthRepository()
    }

    fun setUserId(userId: String) {
        this.userId = userId
    }

    fun setUserEmail(email: String) {
        this.userEmail = email
    }
}
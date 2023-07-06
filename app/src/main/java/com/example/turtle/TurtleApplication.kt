package com.example.turtle

import android.app.Application
import com.example.turtle.data.BillRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first

class TurtleApplication: Application() {
    lateinit var billRepository: BillRepository
        private set

    var userId: String? = null
        private set

    override fun onCreate() {
        super.onCreate()
        billRepository = BillRepository()
    }

    fun setUserId(uid: String) {
        userId = uid
    }
}
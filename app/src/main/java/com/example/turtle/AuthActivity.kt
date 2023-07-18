package com.example.turtle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.turtle.data.BillRepository
import com.example.turtle.databinding.ActivityAuthBinding
import com.example.turtle.ui.auth.AuthViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class AuthActivity: AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.isUserLoggedIn()) {
            auth.currentUser?.run {
                lifecycleScope.launch {
                    val app = application as TurtleApplication
                    app.setUserId(uid)
                    app.setUserEmail(email!!)
                    schedulePeriodicNotification()
                    startActivityMain()
                }
            } ?:run {
                initAuthActivity()
            }
        } else {
            initAuthActivity()
        }
    }

    private fun initAuthActivity() {
        setTheme(R.style.Theme_Turtle)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun startActivityMain() {
        Intent(this, MainActivity::class.java).also {
            startActivity(it)
        }
        finish()
    }

    private fun schedulePeriodicNotification() {
        val scheduler = AlarmScheduler(this)
        (application as TurtleApplication).setAlarmScheduler(scheduler)
        scheduler.schedule()
    }
}
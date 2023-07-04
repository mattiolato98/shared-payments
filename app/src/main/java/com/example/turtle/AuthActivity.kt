package com.example.turtle

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.turtle.databinding.ActivityAuthBinding
import com.example.turtle.ui.auth.AuthViewModel
import kotlinx.coroutines.launch


class AuthActivity: AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    private val settingPreferences = SettingsPreferences(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val userProfile = viewModel.getSignedInUserProfile()
            if (viewModel.isUserLoggedIn() && userProfile != null) {
                settingPreferences.setUserInfo(userProfile)
                startActivityMain()
            } else {
                initAuthActivity()
            }
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
}
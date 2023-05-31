package com.example.turtle

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.turtle.databinding.ActivitySigningBinding
import com.example.turtle.ui.auth.AuthViewModel


class AuthActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySigningBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.getSignedInUser() != null) {
            startActivityMain()
        } else {
            setTheme(R.style.Theme_Turtle)

            binding = ActivitySigningBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
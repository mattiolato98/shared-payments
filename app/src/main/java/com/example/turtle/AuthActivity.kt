package com.example.turtle

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.turtle.databinding.ActivityAuthBinding
import com.example.turtle.ui.auth.AuthViewModel
import com.example.turtle.ui.auth.UserData


class AuthActivity: AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.getSignedInUser() != null) {
            startActivityMain()
        } else {
            setTheme(R.style.Theme_Turtle)

            binding = ActivityAuthBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun startActivityMain() {
        Intent(this, MainActivity::class.java).also {
            it.putExtra("user", viewModel.getSignedInUser())
            startActivity(it)
        }
        finish()
    }
}
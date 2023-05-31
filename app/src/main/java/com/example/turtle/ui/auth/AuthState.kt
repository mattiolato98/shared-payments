package com.example.turtle.ui.auth

data class AuthState(
    val isSignInSuccessful: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val genericFormError: String? = null,
)
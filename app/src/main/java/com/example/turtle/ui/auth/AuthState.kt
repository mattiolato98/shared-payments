package com.example.turtle.ui.auth

data class AuthState(
    val isUserLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val genericFormError: String? = null,
)
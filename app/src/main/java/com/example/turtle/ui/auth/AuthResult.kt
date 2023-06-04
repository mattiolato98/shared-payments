package com.example.turtle.ui.auth

import com.example.turtle.data.Profile


data class AuthResult(
    val isSuccessful: Boolean,
    val emailErrorMessage: String?,
    val passwordErrorMessage: String?,
    val genericFormErrorMessage: String?,
)
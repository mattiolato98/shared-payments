package com.example.turtle.ui.auth


data class AuthResult(
    val isSuccessful: Boolean,
    val emailErrorMessage: String?,
    val passwordErrorMessage: String?,
    val genericFormErrorMessage: String?,
)
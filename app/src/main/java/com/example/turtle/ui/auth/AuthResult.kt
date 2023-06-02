package com.example.turtle.ui.auth


data class AuthResult(
    val data: UserData?,
    val emailErrorMessage: String?,
    val passwordErrorMessage: String?,
    val genericFormErrorMessage: String?,
)


data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?
)
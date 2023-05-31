package com.example.turtle.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.data.AuthRepository
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class AuthViewModel: ViewModel() {
    private val authRepository = AuthRepository()

    private val _state = MutableStateFlow((AuthState()))
    val state = _state.asStateFlow()

    fun signInWithGoogle(googleCredentials: AuthCredential) = viewModelScope.launch {
        val result = authRepository.signInWithGoogle(googleCredentials)
        onAuthResult(result)
    }

    fun signInWithEmailAndPassword(email: String, password: String) = viewModelScope.launch {
        val result = authRepository.signInWithEmailAndPassword(email, password)
        onAuthResult(result)
    }

    fun signUpWithEmailAndPassword(email: String, password: String) = viewModelScope.launch {
        val result = authRepository.signUpWithEmailAndPassword(email, password)
        onAuthResult(result)
    }

    private fun onAuthResult(result: AuthResult) {
        with(result) {
            _state.update { it.copy(
                isSignInSuccessful = data != null,
                emailError = emailErrorMessage,
                passwordError = passwordErrorMessage,
                genericFormError = genericFormErrorMessage
            )}}
    }

    fun resetState() {
        _state.update { AuthState() }
    }
}
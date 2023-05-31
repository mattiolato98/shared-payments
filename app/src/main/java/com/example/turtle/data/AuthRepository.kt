package com.example.turtle.data

import android.util.Log
import com.example.turtle.ui.auth.AuthResult
import com.example.turtle.ui.auth.UserData
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

const val TAG = "AUTH"


class AuthRepository {
    private val auth = Firebase.auth

    suspend fun signInWithGoogle(googleCredentials: AuthCredential): AuthResult {
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            buildAuthResult(user, null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            buildAuthResult(null, e.message)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult {
        return try {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
            buildAuthResult(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> buildAuthResult(emailErrorMsg = e.message)
                else -> buildAuthResult(formErrorMsg = "Invalid credentials.")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            buildAuthResult(formErrorMsg = "Invalid credentials.")
        }
    }

    suspend fun signUpWithEmailAndPassword(email: String, password: String): AuthResult {
        return try {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
            buildAuthResult(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            buildAuthResult(passwordErrorMsg = e.message)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> buildAuthResult(emailErrorMsg = e.message)
                "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> buildAuthResult(
                    formErrorMsg = "The email address is already in use by another account."
                )
                "ERROR_EMAIL_ALREADY_IN_USE" -> buildAuthResult(
                    emailErrorMsg = "The email address is already in use by another account."
                )
                else -> buildAuthResult(formErrorMsg = "Invalid credentials.")
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            buildAuthResult(formErrorMsg = e.message)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            buildAuthResult(formErrorMsg = "Invalid credentials.")
        }
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            username = displayName,
            profilePictureUrl = photoUrl?.toString()
        )
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }
    }

    private fun buildAuthResult(
        user: FirebaseUser? = null,
        emailErrorMsg: String? = null,
        passwordErrorMsg: String? = null,
        formErrorMsg: String? = null
    ): AuthResult {
        return AuthResult(
            data = user?.run {
                UserData(
                    userId = uid,
                    username = displayName,
                    profilePictureUrl = photoUrl?.toString()
                )
            },
            emailErrorMessage = emailErrorMsg,
            passwordErrorMessage = passwordErrorMsg,
            genericFormErrorMessage = formErrorMsg
        )
    }
}
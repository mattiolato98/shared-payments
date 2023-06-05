package com.example.turtle.data

import android.util.Log
import android.widget.Toast
import com.example.turtle.ui.auth.AuthResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

const val TAG = "AUTH"


class AuthRepository {
    private val auth = Firebase.auth
    private val profileCollectionRef = Firebase.firestore.collection("profiles")


    suspend fun signInWithGoogle(googleCredentials: AuthCredential): AuthResult {
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            if (!userAlreadyExists(user))
                createProfile(user)
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
            createProfile(user)
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

    suspend fun getSignedInUser(): Profile? = auth.currentUser?.run {
        val doc = profileCollectionRef.document(uid).get().await()
        return doc.toObject(Profile::class.java)
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
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
            isSuccessful = user != null,
            emailErrorMessage = emailErrorMsg,
            passwordErrorMessage = passwordErrorMsg,
            genericFormErrorMessage = formErrorMsg
        )
    }

    private suspend fun createProfile(user: FirebaseUser?) {
        user?.run {
            val profile = Profile(
                userId = uid,
                username = displayName,
                email = email,
                profilePictureUrl = photoUrl?.toString()
            )
            profileCollectionRef.add(profile).await()
        }
    }

    private suspend fun userAlreadyExists(user: FirebaseUser?): Boolean {
        user?.run {
            return !profileCollectionRef.whereEqualTo("userId", user.uid).get().await().isEmpty
        }
        return false
    }
}
package com.example.turtle.data

import android.util.Log
import com.example.turtle.Resource
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


class AuthRepository: BaseRepository() {
    val tag = "AUTH"

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
        return@run getSignedInUser(uid)
    }

    private suspend fun getSignedInUser(userId: String): Profile? {
        return try {
            val doc = profileCollectionRef.document(userId).get().await()
            doc.toObject(Profile::class.java)
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            if (e is CancellationException) throw e
            null
        }
    }

    suspend fun getProfileByUserId(userId: String) = getProfile("userId", userId)
    suspend fun getProfileByEmail(email: String) = getProfile("email", email)

    private suspend fun getProfile(fieldName: String, fieldValue: Any): Resource<Profile> {
        return try {
            val profileDocs = profileCollectionRef
                .whereEqualTo(fieldName, fieldValue).get().await()
            if (profileDocs.isEmpty)
                return Resource.Error("No user found with this $fieldName")
            Resource.Success(profileDocs.first().toObject(Profile::class.java))
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            if (e is CancellationException) throw e
            Resource.Error("An error occurred while fetching the data. Try again later")
        }
    }

    fun getSignedInUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(tag, e.message.toString())
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
                displayName = displayName,
                email = email,
                username = email!!.split("@")[0],
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
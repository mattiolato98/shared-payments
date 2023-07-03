package com.example.turtle.ui.auth

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.turtle.MainActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

const val TAG = "AUTH"


abstract class BaseAuthFragment: Fragment() {

    abstract val fieldEmail: TextInputEditText
    abstract val fieldEmailLayout: TextInputLayout
    abstract val fieldPassword: TextInputEditText
    abstract val fieldPasswordLayout: TextInputLayout
    abstract val formErrorMessage: TextView
    abstract val progressBar: ProgressBar
    abstract val googleButton: Button

    val viewModel: AuthViewModel by activityViewModels()
    private lateinit var oneTapSignInLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var oneTapClient: SignInClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        oneTapClient = Identity.getSignInClient(requireActivity())

        oneTapSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = result.data
                val idToken = oneTapClient.getSignInCredentialFromIntent(intent).googleIdToken
                val googleCredentials = GoogleAuthProvider.getCredential(idToken, null)
                viewModel.signInWithGoogle(googleCredentials)
            }
        }

        collectLifecycleFlow(viewModel.state) { state ->
            checkState(state)
        }
    }

    private fun checkState(state: AuthState) {
        if (state.isLoading) showProgressBar() else hideProgressBar()

        if (state.isUserLoggedIn) {
            startActivityMain()
        } else {
            fieldEmailLayout.error = state.emailError
            fieldPasswordLayout.error = state.passwordError

            if (state.genericFormError != null) {
                formErrorMessage.visibility = View.VISIBLE
                formErrorMessage.text = state.genericFormError
            } else {
                formErrorMessage.visibility = View.GONE
                formErrorMessage.text = null
            }
        }
    }

    private fun startActivityMain() {
        Intent(context, MainActivity::class.java).also {
            startActivity(it)
        }
        requireActivity().finish()
    }

    private fun launchGoogleSignIn() {
        showProgressBar()
        viewLifecycleOwner.lifecycleScope.launch {
            val signInIntentSender = initGoogleSignIn()
            oneTapSignInLauncher.launch(
                IntentSenderRequest.Builder(signInIntentSender?: return@launch).build()
            )
            hideProgressBar()
        }
    }

    open fun initUiListeners() {
        googleButton.setOnClickListener{ launchGoogleSignIn() }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private suspend fun initGoogleSignIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(requireContext().getString(
                        com.firebase.ui.auth.R.string.default_web_client_id)
                    )
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }
}

fun <T> BaseAuthFragment.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
}
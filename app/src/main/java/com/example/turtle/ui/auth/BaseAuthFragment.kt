package com.example.turtle.ui.auth

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.turtle.MainActivity
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseAuthFragment: Fragment() {

    abstract var fieldEmail: TextInputEditText
    abstract var fieldEmailLayout: TextInputLayout
    abstract var fieldPassword: TextInputEditText
    abstract var fieldPasswordLayout: TextInputLayout
    abstract var formErrorMessage: TextView
    abstract var progressBar: ProgressBar
    abstract var googleButton: Button


    lateinit var auth: FirebaseAuth
    val viewModel: AuthViewModel by viewModels()
    private lateinit var oneTapSignUpLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var oneTapClient: SignInClient
    private lateinit var firebaseAuthUiClient: FirebaseAuthUiClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        oneTapClient = Identity.getSignInClient(requireActivity())

        auth = Firebase.auth
        firebaseAuthUiClient = FirebaseAuthUiClient(
            context = requireContext(),
            oneTapClient = Identity.getSignInClient(requireContext())
        )

        oneTapSignUpLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = result.data
                val idToken = oneTapClient.getSignInCredentialFromIntent(intent).googleIdToken
                val googleCredentials = GoogleAuthProvider.getCredential(idToken, null)
                viewModel.signInWithGoogle(googleCredentials)
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun checkState(state: AuthState) {
        if (state.isSignInSuccessful) {
            startMain()
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

    fun initUiListeners() {
        googleButton.setOnClickListener{ launchGoogleSignIn() }
    }

    fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun startMain() {
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun launchGoogleSignIn() {
        showProgressBar()
        viewLifecycleOwner.lifecycleScope.launch {
            val signInIntentSender = firebaseAuthUiClient.initGoogleSignIn()
            oneTapSignUpLauncher.launch(
                IntentSenderRequest.Builder(signInIntentSender?: return@launch).build()
            )
            hideProgressBar()
        }
    }
}

fun <T> BaseAuthFragment.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
}
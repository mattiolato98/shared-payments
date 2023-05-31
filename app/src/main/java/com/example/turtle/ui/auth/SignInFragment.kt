package com.example.turtle.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.turtle.databinding.FragmentSignInBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class SignInFragment: BaseAuthFragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override lateinit var fieldEmail: TextInputEditText
    override lateinit var fieldEmailLayout: TextInputLayout
    override lateinit var fieldPassword: TextInputEditText
    override lateinit var fieldPasswordLayout: TextInputLayout
    override lateinit var formErrorMessage: TextView
    override lateinit var progressBar: ProgressBar
    override lateinit var googleButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUiComponents()
        initUiListeners()

        with(binding) {
            signInButton.setOnClickListener { signInWithEmailAndPassword() }

            toSignUp.setOnClickListener { navigateToSignUp() }
        }

        collectLifecycleFlow(viewModel.state) { state ->
            checkState(state)
        }
    }

    private fun signInWithEmailAndPassword() {
        showProgressBar()

        val email = fieldEmail.text.toString()
        val password = fieldPassword.text.toString()

        viewModel.signInWithEmailAndPassword(email, password)
        Thread.sleep(5000L)
    }

    private fun navigateToSignUp() {
        val action = SignInFragmentDirections.navigateToSignUp()
        findNavController().navigate(action)
    }

    private fun initUiComponents() {
        fieldEmail = binding.fieldEmail
        fieldEmailLayout = binding.fieldEmailLayout
        fieldPassword = binding.fieldPassword
        fieldPasswordLayout = binding.fieldPasswordLayout
        formErrorMessage = binding.formErrorMessage
        progressBar = binding.progressBar
        googleButton = binding.googleSignInButton
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.turtle.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.turtle.databinding.FragmentSignUpBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


class SignUpFragment: BaseAuthFragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override val fieldEmail get() = binding.fieldEmail
    override val fieldEmailLayout get() = binding.fieldEmailLayout
    override val fieldPassword get() = binding.fieldPassword
    override val fieldPasswordLayout get() = binding.fieldPasswordLayout
    override val formErrorMessage get() = binding.formErrorMessage
    override val progressBar get() = binding.progressBar
    override val googleButton get() = binding.googleSignUpButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUiListeners()
    }

    override fun initUiListeners() {
        super.initUiListeners()
        binding.signUpButton.setOnClickListener { signUpWithEmailAndPassword() }
        binding.toSignIn.setOnClickListener { navigateToSignIn() }
    }

    private fun signUpWithEmailAndPassword() {
        val email = fieldEmail.text.toString()
        val password = fieldPassword.text.toString()
        viewModel.signUpWithEmailAndPassword(email, password)
    }

    private fun navigateToSignIn() {
        val action = SignUpFragmentDirections.navigateToSignIn()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
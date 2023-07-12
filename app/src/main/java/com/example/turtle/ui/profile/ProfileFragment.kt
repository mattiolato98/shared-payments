package com.example.turtle.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.turtle.AuthActivity
import com.example.turtle.R
import com.example.turtle.SettingsPreferences
import com.example.turtle.TurtleApplication
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth
    private lateinit var oneTapClient: SignInClient
    private lateinit var settingsPreferences: SettingsPreferences

    private val viewModel: ProfileViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        settingsPreferences = SettingsPreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        oneTapClient = Identity.getSignInClient(requireActivity())
        initProfileFragment()
        collectProfile()
    }

    private fun initProfileFragment() {
        binding.signOutButton.setOnClickListener { signOut() }

        viewModel.getProfile()
    }

    private fun collectProfile() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.profile.collect { profile ->
                profile?.run { fillProfileData(profile) }
            }
        }
    }

    private fun fillProfileData(profile: Profile) {
        if (profile.profilePictureUrl != null) {
            Picasso.get()
                .load(profile.profilePictureUrl!!.replace("96", "400").toUri())
                .into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(R.drawable.profile_material)
        }

        binding.displayName.text = profile.displayName?: profile.username
        binding.email.text = profile.email
    }

    private fun signOut() = viewLifecycleOwner.lifecycleScope.launch {
        oneTapClient.signOut()
        auth.signOut()

        settingsPreferences.clearPreferences()
        (requireActivity().application as TurtleApplication).setUserId(null)
        (requireActivity().application as TurtleApplication).setUserEmail(null)

        startActivityAuth()
    }

    private fun startActivityAuth() {
        Intent(requireContext(), AuthActivity::class.java).also {
            startActivity(it)
        }
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
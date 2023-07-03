package com.example.turtle.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.turtle.AuthActivity
import com.example.turtle.R
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentProfileBinding
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth
    private lateinit var oneTapClient: SignInClient

    private val profileCollectionRef = Firebase.firestore.collection("profiles")
    private lateinit var profile: Profile

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        oneTapClient = Identity.getSignInClient(requireActivity())

        setUpProfile()

        binding.signOutButton.setOnClickListener { signOut() }
    }

    private fun setUpProfile() = viewLifecycleOwner.lifecycleScope.launch {
        val profileDoc = profileCollectionRef.whereEqualTo("userId", auth.currentUser?.uid).get().await().first()
        profile = profileDoc.toObject(Profile::class.java)

        if (profile.profilePictureUrl != null) {
            Picasso.get()
                .load(profile.profilePictureUrl!!.replace("96", "400").toUri())
                .into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(R.drawable.profile_material)
        }

        binding.displayName.text = profile.displayName
        binding.email.text = profile.email
    }

    private fun signOut() = viewLifecycleOwner.lifecycleScope.launch {
        oneTapClient.signOut()
        auth.signOut()

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
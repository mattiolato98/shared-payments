package com.example.turtle.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.turtle.AuthActivity
import com.example.turtle.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment(), OnClickListener {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var signOutButton: Button
    private lateinit var context: Context
    private var user: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val profileViewModel: ProfileViewModel by viewModels()

        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        signOutButton = binding.signOutButton
        signOutButton.setOnClickListener(this)

        context = container!!.context

        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        profileViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        user = Firebase.auth.currentUser

        return root
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.signOutButton.id -> onSignOutClicked()
        }
    }

    private fun onSignOutClicked() {
        AuthUI.getInstance()
            .signOut(context)
            .addOnCompleteListener {
                val i = Intent(requireContext(), AuthActivity::class.java)
                startActivity(i)
                requireActivity().finish()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
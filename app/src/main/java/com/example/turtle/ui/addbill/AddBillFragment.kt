package com.example.turtle.ui.addbill

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentAddBillBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val TAG = "ADD_BILL"


class AddBillFragment: Fragment() {

    private var _binding: FragmentAddBillBinding? = null
    private val binding get() = _binding!!

    private var friends = mutableMapOf<String, Profile>()

    private val billCollectionRef = Firebase.firestore.collection("bills")
    private val profileCollectionRef = Firebase.firestore.collection("profiles")

    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            saveBillButton.setOnClickListener { saveBill() }
            fieldAddFriend.doOnTextChanged { text, _, _, _ -> fillEmailText(text) }
            buttonAddFriend.setOnClickListener { addFriend() }
        }
    }

    private fun saveBill() = viewLifecycleOwner.lifecycleScope.launch {
        val profileDocs = profileCollectionRef.whereEqualTo("userId", auth.currentUser?.uid).get().await()
        val currentUserProfile = profileDocs.first().toObject(Profile::class.java)

        val title = binding.fieldTitle.text.toString()
        val description = binding.fieldDescription.text.toString().let { it.ifEmpty { null } }
        val users = friends.values + currentUserProfile

        if (title.isEmpty()) {
            Snackbar.make(requireView(), "Bill title cannot be empty", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        val bill = createBill(title, description, users)

        try {
            billCollectionRef.add(bill).await()
            Snackbar.make(requireView(), "Bill saved", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }

        findNavController().navigateUp()
    }

    private fun createBill(title: String, description: String?, users: List<Profile>): Bill = Bill(
        userOwnerId = auth.currentUser?.uid!!,
        users = users,
        title = title,
        description = description,
    )

    private fun addFriend() = viewLifecycleOwner.lifecycleScope.launch {
        val friendEmail = binding.fieldAddFriend.text.toString().trim()
//        val fieldName = if (EMAIL_ADDRESS.matcher(text).matches()) "email" else "username"
        val profileDocs = profileCollectionRef.whereEqualTo("email", friendEmail).get().await()

        if (profileDocs.isEmpty) {
            Snackbar.make(
                requireView(),
                "No user matches this email.",
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            if (friendEmail == auth.currentUser?.email) {
                Snackbar.make(requireView(), "You cannot add yourself.", Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            if (friendAlreadyAdded(friendEmail)) {
                Snackbar.make(requireView(), "Friend already added.", Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            val profile = profileDocs.first().toObject(Profile::class.java)
            addFriendToLayout(friendEmail)
            friends[friendEmail] = profile

            clearAddFriend()
        }
    }

    private fun addFriendToLayout(text: String) {
        showFriendsTitle()

        (LayoutInflater.from(requireContext()).inflate(
            R.layout.button_borderless_template,
            null,
            false
        ) as Button).also { btn ->
            btn.text = text
            btn.setOnClickListener { removeFriend(it) }
            binding.friendsLinearLayout.addView(btn)
        }
    }

    private fun friendAlreadyAdded(text: String): Boolean {
        return friends.containsKey(text)
    }

    private fun clearAddFriend() {
        binding.fieldAddFriend.text = null
        binding.buttonAddFriend.text = null
    }

    private fun removeFriend(v: View) {
        val text = (v as Button).text

        friends.remove(text)
        binding.friendsLinearLayout.removeView(v)

        if (friends.isEmpty())
            hideFriendsTitle()
    }

    private fun fillEmailText(text: CharSequence?) {
        if (text.toString().isNotEmpty()) {
            binding.buttonAddFriend.visibility = View.VISIBLE
            binding.buttonAddFriend.text = "${text.toString()} +"
        } else {
            binding.buttonAddFriend.visibility = View.GONE
        }

    }

    private fun showFriendsTitle() {
        binding.friendsTitle.visibility = View.VISIBLE
    }

    private fun hideFriendsTitle() {
        binding.friendsTitle.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
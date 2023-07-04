package com.example.turtle.ui.addeditbill

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
import androidx.navigation.fragment.navArgs
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentAddEditBillBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val TAG = "ADD_BILL"


class AddEditBillFragment: Fragment() {

    private var _binding: FragmentAddEditBillBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditBillFragmentArgs by navArgs()

    private var friends = mutableMapOf<String, Profile>()

    private val billCollectionRef = Firebase.firestore.collection("bills")
    private val profileCollectionRef = Firebase.firestore.collection("profiles")

    private var bill: Bill? = null

    private var isNewBill: Boolean = true

    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUp(args.billId)

        with(binding) {
            saveBillButton.setOnClickListener { saveBill() }
            fieldAddFriend.doOnTextChanged { text, _, _, _ -> fillEmailText(text) }
            buttonAddFriend.setOnClickListener { addFriend() }
        }
    }

    private fun setUp(billId: String?) = viewLifecycleOwner.lifecycleScope.launch {
        billId?.also {
            bill = try {
                val doc = billCollectionRef.document(billId).get().await()
                doc.toObject(Bill::class.java)!!
            } catch (e: Exception) {
                Log.e(com.example.turtle.ui.billdetail.TAG, e.message.toString())
                if (e is CancellationException) throw e
                return@launch
            }

            isNewBill = false
            fillForm(bill!!)

            for (user in bill!!.users!!.filter { it.userId != auth.currentUser?.uid }) {
                val userEmail = user.email!!
                val profileDoc = profileCollectionRef.whereEqualTo("email", userEmail).get().await().first()
                val profile = profileDoc.toObject(Profile::class.java)
                addFriendToLayout(userEmail)
                friends[userEmail] = profile
            }

            val expensesCollection = billCollectionRef.document(billId).collection("expenses").get().await()
            val expensesList = expensesCollection.documents.map { doc ->
                doc.toObject(Expense::class.java)!!
            }
            bill!!.expenses = expensesList
        }
    }

    private fun fillForm(bill: Bill) {
        with(binding) {
            fieldTitle.setText(bill.title)
            fieldDescription.setText(bill.description)
        }
    }

    private fun saveBill() = viewLifecycleOwner.lifecycleScope.launch {
        val profileDoc = profileCollectionRef.whereEqualTo("userId", auth.currentUser?.uid).get().await().first()
        val currentUserProfile = profileDoc.toObject(Profile::class.java)

        val title = binding.fieldTitle.text.toString()
        val description = binding.fieldDescription.text.toString().let { it.ifEmpty { null } }
        val users = friends.values + currentUserProfile

        if (title.isEmpty()) {
            Snackbar.make(requireView(), "Bill title cannot be empty", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        val billObject = billObject(title, description, users)

        try {
            if (isNewBill)
                createNewBill(billObject)
            else
                updateBill(bill!!.documentId!!, billObject)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }

        findNavController().navigateUp()
    }

    private fun createNewBill(bill: Bill) {
        billCollectionRef.add(bill)
        Snackbar.make(requireView(), "Bill saved", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateBill(billId: String, newBill: Bill) {
        billCollectionRef.document(billId).set(
            newBill,
            SetOptions.merge()
        )
        Snackbar.make(requireView(), "Bill information updated", Snackbar.LENGTH_SHORT).show()
    }

    private fun billObject(title: String, description: String?, users: List<Profile>) = Bill(
        userOwnerId = auth.currentUser?.uid!!,
        usersId = users.map { it.userId!! },
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

    private fun removeFriend(v: View) = viewLifecycleOwner.lifecycleScope.launch {
        val text = (v as Button).text

        if (!isNewBill && isUserInvolvedInExpenses(text.toString())) {
            Snackbar.make(
                requireView(),
                "This user is involved in at least one expense, so it is not possible to remove it",
                Snackbar.LENGTH_SHORT
            ).show()

            return@launch
        }

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

    private suspend fun isUserInvolvedInExpenses(userEmail: String): Boolean {
        val profileDoc = profileCollectionRef.whereEqualTo("email", userEmail).get().await().first()
        val profile = profileDoc.toObject(Profile::class.java)

        return bill!!.expenses
            ?.flatMap { it.usersPaidFor?.keys!! + it.userPayingId }
            ?.contains(profile.userId)
            ?: false
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
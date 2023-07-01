package com.example.turtle.ui.bills

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.turtle.R
import com.example.turtle.TAG
import com.example.turtle.data.Bill
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentBillsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var billsAdapter: BillsAdapter
    private val viewModel: BillsViewModel by viewModels()

    private val auth = Firebase.auth

    private val billCollectionRef = Firebase.firestore.collection("bills")
    private val profileCollectionRef = Firebase.firestore.collection("profiles")
    private lateinit var currentUserProfile: Profile

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)
        setupMenuProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        billsAdapter = BillsAdapter { item -> navigateToBillDetail(item)}

        with(binding) {
            newBillButton.setOnClickListener { navigateToAddBill() }
            billsList.adapter = billsAdapter
        }

        subscribeToRealtimeUpdates()
    }

    private fun longClick(bill: Bill): Boolean {
//        Snackbar.make(requireView(), "Long clicked ${bill.title}", Snackbar.LENGTH_SHORT).show()
        return true
    }

    private fun navigateToAddBill() {
        val action = BillsFragmentDirections.navigateToAddBill(
            title = resources.getString(R.string.new_bill)
        )
        findNavController().navigate(action)
    }

    private fun navigateToBillDetail(bill: Bill) {
        val action = BillsFragmentDirections.navigateToBillDetail(
            bill.documentId!!,
            bill.title
        )
        findNavController().navigate(action)
    }

    private fun subscribeToRealtimeUpdates() = viewLifecycleOwner.lifecycleScope.launch {
        val currentUserProfileDoc = profileCollectionRef.whereEqualTo("userId", auth.currentUser?.uid).get().await().first()
        currentUserProfile = currentUserProfileDoc.toObject(Profile::class.java)

        val billQuery =  billCollectionRef
            .whereArrayContains("users", currentUserProfile)
            .orderBy("createDateTime", Query.Direction.DESCENDING)

        billQuery.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Log.e(TAG, it.message.toString())
            }
            querySnapshot?.let {
                val billsList = querySnapshot.documents.map { doc ->
                    doc.toObject(Bill::class.java)
                }
                billsAdapter.differ.submitList(billsList)
            }
        }
    }

    private fun navigateToProfile() {
        val action = BillsFragmentDirections.navigateToProfile(currentUserProfile.username!!)
        findNavController().navigate(action)
    }

    private fun setupMenuProvider() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.home_options, menu)
                    (menu as MenuBuilder).setOptionalIconsVisible(true)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.open_profile -> navigateToProfile()
                        else -> return false
                    }

                    return true
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
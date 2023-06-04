package com.example.turtle.ui.bills

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.turtle.TAG
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentBillsBinding
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var billsAdapter: BillsAdapter
    private val viewModel: BillsViewModel by viewModels()
    private val billCollectionRef = Firebase.firestore.collection("bills").orderBy("createDateTime", Query.Direction.DESCENDING)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)
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

    private fun navigateToAddBill() {
        val action = BillsFragmentDirections.navigateToAddBill()
        findNavController().navigate(action)
    }

//    private fun newBill(bill: Bill) = viewLifecycleOwner.lifecycleScope.launch {
//        try {
//            billCollectionRef.add(bill).await()
//            Toast.makeText(requireContext(), "Bill saved.", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Log.e(TAG, e.message.toString())
//        }
//    }

    private fun navigateToBillDetail(bill: Bill) {
        val action = BillsFragmentDirections.navigateToBillDetail(
            bill.documentId!!,
            bill.title
        )
        findNavController().navigate(action)
    }

    private fun subscribeToRealtimeUpdates() = viewLifecycleOwner.lifecycleScope.launch {
        billCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.turtle.ui.billdetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentBillDetailBinding
import com.example.turtle.ui.bills.BillsFragmentDirections
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val TAG = "BILL_DETAIL"


class BillDetailFragment: Fragment() {

    private var _binding: FragmentBillDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BillDetailFragmentArgs by navArgs()
    private val billCollectionRef = Firebase.firestore.collection("bills")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpBill(args.billId)
        binding.newExpenseButton.setOnClickListener { navigateToAddExpense() }
    }

    private fun setUpBill(billId: String) = viewLifecycleOwner.lifecycleScope.launch {
        val bill = try {
            val doc = billCollectionRef.document(billId).get().await()
            doc.toObject(Bill::class.java)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            null
        }

        bill?.let {

        } ?:let {
            Toast.makeText(requireContext(), "Unable to retrieve bill. Try again later.", Toast.LENGTH_SHORT).show()
            val action = BillDetailFragmentDirections.navigateToBills()
            findNavController().navigate(action)
        }
    }

    private fun navigateToAddExpense() {
        val action = BillDetailFragmentDirections.navigateToAddExpense()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
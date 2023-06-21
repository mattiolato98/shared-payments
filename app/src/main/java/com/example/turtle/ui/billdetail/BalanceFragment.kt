package com.example.turtle.ui.billdetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentBalanceBinding
import com.example.turtle.ui.expensedetail.BalanceAdapter
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class BalanceFragment: Fragment() {
    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var billId: String
    private lateinit var bill: Bill
    private val billCollectionRef = Firebase.firestore.collection("bills")

    private lateinit var balanceAdapter: BalanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        billId = this.requireArguments().getString("billId")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getBill(billId) }

    private fun getBill(billId: String) = viewLifecycleOwner.lifecycleScope.launch {
        bill = try {
            val doc = billCollectionRef.document(billId).get().await()
            doc.toObject(Bill::class.java)!!
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return@launch
        }

        val expensesCollection = billCollectionRef.document(billId).collection("expenses").get().await()
        val expensesList = expensesCollection.documents.map { doc ->
            doc.toObject(Expense::class.java)!!
        }
        bill.expenses = expensesList

        balanceAdapter = BalanceAdapter(bill.balance())
        binding.usersBalance.adapter = balanceAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
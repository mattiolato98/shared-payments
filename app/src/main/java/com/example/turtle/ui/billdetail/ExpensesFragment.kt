package com.example.turtle.ui.billdetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentExpensesBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.RoundingMode

const val TAG = "BILL_DETAIL"


class ExpensesFragment: Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth

    private lateinit var expensesAdapter: ExpensesAdapter
    private lateinit var billId: String

    private lateinit var bill: Bill
    private val billCollectionRef = Firebase.firestore.collection("bills")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        billId = this.requireArguments().getString("billId")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        getBill(billId)
        binding.newExpenseButton.setOnClickListener { navigateToAddExpense() }
    }

    private fun getBill(billId: String) = viewLifecycleOwner.lifecycleScope.launch {
        bill = try {
            val doc = billCollectionRef.document(billId).get().await()
            doc.toObject(Bill::class.java)!!
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return@launch
        }

        expensesAdapter = ExpensesAdapter(
            bill,
            setTotals = { setTotals() },
            onClickListener = { item -> navigateToExpenseDetail(item) }
        )
        binding.expensesList.adapter = expensesAdapter

        subscribeToRealtimeUpdates()
    }

    private fun subscribeToRealtimeUpdates() = viewLifecycleOwner.lifecycleScope.launch {
        val expenseQuery = billCollectionRef.document(billId)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)

        expenseQuery.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Log.e(TAG, it.message.toString())
            }
            querySnapshot?.let {
                val expensesList = querySnapshot.documents.map { doc ->
                    doc.toObject(Expense::class.java)!!
                }
                bill.expenses = expensesList
                expensesAdapter.differ.submitList(expensesList)
            }
        }
    }

    private fun setTotals() {
        binding.userTotal.text = bill.userTotal(auth.currentUser!!.uid).setScale(2, RoundingMode.HALF_UP).toString()
        binding.groupTotal.text = bill.groupTotal().setScale(2, RoundingMode.HALF_UP).toString()
    }

    private fun navigateToExpenseDetail(expense: Expense) {
        val action = BillDetailFragmentDirections.navigateToExpenseDetail(
            bill.documentId!!,
            expense.documentId!!,
            expense.title
        )
        navigateToDirection(action)
    }

    private fun navigateToAddExpense() {
        val action = BillDetailFragmentDirections.navigateToAddExpense(bill.documentId!!)
        navigateToDirection(action)
    }

    private fun navigateToDirection(action: NavDirections) {
        parentFragmentManager.beginTransaction().detach(this).commit()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
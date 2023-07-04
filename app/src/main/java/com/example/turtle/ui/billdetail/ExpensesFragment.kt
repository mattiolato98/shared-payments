package com.example.turtle.ui.billdetail

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.example.turtle.R
import com.example.turtle.SettingsPreferences
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentExpensesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val TAG = "BILL_DETAIL"


class ExpensesFragment: Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expensesAdapter: ExpensesAdapter

    private lateinit var billId: String
    private lateinit var bill: Bill
    private val billCollectionRef = Firebase.firestore.collection("bills")

    private lateinit var settingsPreferences: SettingsPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        billId = this.requireArguments().getString("billId")!!
        settingsPreferences = SettingsPreferences(requireContext())
        setupMenuProvider()
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

    private fun setTotals() = viewLifecycleOwner.lifecycleScope.launch {
        binding.userTotal.text = bill.userTotal(settingsPreferences.getUserId.first())
        binding.groupTotal.text = bill.groupTotal()
    }

    private fun setupMenuProvider() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.bill_options, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.edit_bill -> navigateToEditBill()
                        R.id.delete_bill -> showDeleteDialog()
                        else -> return false
                    }

                    return true
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    private fun deleteBill() = viewLifecycleOwner.lifecycleScope.launch {
        val msg = try {
            billCollectionRef.document(billId).delete().await()
            "Bill successfully deleted"
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
            if (e is CancellationException) throw e
            "An unexpected error occurred while deleting the item. Retry later"
        }

        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete the bill? The action is not reversible!")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteBill()
                dialog.cancel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToEditBill() {
        val action = BillDetailFragmentDirections.navigateToEditBill(
            billId,
            resources.getString(R.string.edit_bill)
        )
        findNavController().navigate(action)
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
        val action = BillDetailFragmentDirections.navigateToAddExpense(
            billId = bill.documentId!!,
            title = resources.getString(R.string.new_expense)
        )
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
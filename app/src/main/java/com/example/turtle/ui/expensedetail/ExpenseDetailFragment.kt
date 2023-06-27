package com.example.turtle.ui.expensedetail

import android.app.AlertDialog
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentExpenseDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

const val TAG = "EXPENSE_DETAIL"


class ExpenseDetailFragment: Fragment() {
    private var _binding: FragmentExpenseDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ExpenseDetailFragmentArgs by navArgs()

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    private lateinit var bill: Bill
    private lateinit var expense: Expense

    private lateinit var balanceAdapter: BalanceAdapter

    private val billCollectionRef = Firebase.firestore.collection("bills")
    private lateinit var expenseCollectionRef: CollectionReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseDetailBinding.inflate(inflater, container, false)
        setupMenuProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        expenseCollectionRef = billCollectionRef.document(args.billId).collection("expenses")
        setUp(args.expenseId)
    }

    private fun setUp(expenseId: String) = viewLifecycleOwner.lifecycleScope.launch {
        expense = try {
            val doc = expenseCollectionRef.document(expenseId).get().await()
            doc.toObject(Expense::class.java)!!
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return@launch
        }

        bill = try {
            val doc = billCollectionRef.document(args.billId).get().await()
            doc.toObject(Bill::class.java)!!
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return@launch
        }

        val paidByUser = bill.users!!.first { it.userId == expense.userPayingId }.email!!.split("@")[0]

        binding.expenseAmount.text = expense.amount
        binding.paidBy.text = Html.fromHtml("Paid by <b>${paidByUser}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.expenseDate.text = dateFormat.format(expense.date!!)

        binding.usersPaidForTitle.text = Html.fromHtml(
            "Paid for <b>${expense.usersPaidFor!!.count()} people</b>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        val usersPaidFor =
            bill.users!!.filter { it.userId in expense.usersPaidFor!!.keys }.associateWith {
                expense.usersPaidFor!![it.userId].toString()
            }.mapKeys {
                it.key.email!!.split("@")[0]
            }

        balanceAdapter = BalanceAdapter(usersPaidFor)
        binding.usersPaidForList.adapter = balanceAdapter
    }

    private fun navigateToEditExpense() {
        val action = ExpenseDetailFragmentDirections.navigateToEditExpense(
            expense.documentId!!,
            bill.documentId!!,
            expense.title,
        )
        findNavController().navigate(action)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete ${args.title}? The action is not reversible!")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.cancel()
                expenseCollectionRef.document(args.expenseId).delete()
                    .addOnSuccessListener {
                        Snackbar.make(
                            requireView(),
                            "Expense successfully deleted",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        Snackbar.make(
                            requireView(),
                            "An unexpected error occurred while deleting the item. Retry later",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMenuProvider() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.expense_options, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.edit_expense -> navigateToEditExpense()
                        R.id.delete_expense -> showDeleteDialog()
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
package com.example.turtle.ui.billdetail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.turtle.R
import com.example.turtle.SettingsPreferences
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentExpensesBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val TAG = "BILL_DETAIL"


class ExpensesFragment: Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expensesAdapter: ExpensesAdapter

    private lateinit var billId: String

    private val viewModel: BillDetailViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application,
            billId
        )
    }

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
        initExpensesFragment()
        collectBill()
        collectSnackbar()
        collectDeleted()
    }

    private fun initExpensesFragment() {
        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        expensesAdapter = ExpensesAdapter(onClickListener = { item -> navigateToExpenseDetail(item) })
        binding.expensesList.adapter = expensesAdapter

        binding.newExpenseButton.setOnClickListener { navigateToAddExpense() }
    }

    private fun collectBill() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.bill.collect { bill ->
                setTotals(bill)
                expensesAdapter.differ.submitList(bill.expenses)
                (activity as AppCompatActivity).supportActionBar?.title = bill.title
            }
        }
    }

    private fun collectSnackbar() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.snackbarText.collect { msg ->
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun collectDeleted() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.isDeleted.collect { isDeleted ->
                if (isDeleted) {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun deleteBill() = viewModel.deleteBill()


    private fun setTotals(bill: Bill) = viewLifecycleOwner.lifecycleScope.launch {
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
            billId,
            expense.documentId!!,
            expense.title
        )
        findNavController().navigate(action)
    }

    private fun navigateToAddExpense() {
        val action = BillDetailFragmentDirections.navigateToAddExpense(
            billId = billId,
            title = resources.getString(R.string.new_expense)
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
package com.example.turtle.ui.expensedetail

import android.app.AlertDialog
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.R
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Expense
import com.example.turtle.databinding.FragmentExpenseDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

const val TAG = "EXPENSE_DETAIL"


class ExpenseDetailFragment: Fragment() {
    private var _binding: FragmentExpenseDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ExpenseDetailFragmentArgs by navArgs()
    private lateinit var expenseTitle: String
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private lateinit var balanceAdapter: BalanceAdapter

    private val viewModel: ExpenseDetailViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application,
            args.billId,
            args.expenseId,
        )
    }

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
        initExpenseDetailFragment()
        collectExpense()
        collectSnackbar()
        collectDeleted()
    }

    private fun initExpenseDetailFragment() {
        (activity as AppCompatActivity).supportActionBar?.elevation = 0f
        balanceAdapter = BalanceAdapter()
        binding.usersPaidForList.adapter = balanceAdapter
        expenseTitle = args.title
    }

    private fun collectExpense() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.expense.collect { expense ->
                (activity as AppCompatActivity).supportActionBar?.title = expense.title
                expenseTitle = expense.title
                fillExpenseData(expense)
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

    private fun deleteExpense() = viewModel.deleteExpense()

    private fun fillExpenseData(expense: Expense) {
        binding.expenseAmount.text = expense.amount
        binding.expenseDate.text = dateFormat.format(expense.date!!)

        binding.paidBy.text = Html.fromHtml(
            "Paid by <b>${expense.userPayingUsername}</b>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.usersPaidForTitle.text = Html.fromHtml(
            "Paid for <b>${expense.usersPaidForId!!.count()} people</b>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        balanceAdapter.setData(expense.usersPaidForUsername!!)
    }

    private fun navigateToEditExpense() {
        val action = ExpenseDetailFragmentDirections.navigateToEditExpense(
            args.expenseId,
            args.billId,
            expenseTitle,
        )
        findNavController().navigate(action)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete ${expenseTitle}? The action is not reversible!")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteExpense()
                dialog.cancel()
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
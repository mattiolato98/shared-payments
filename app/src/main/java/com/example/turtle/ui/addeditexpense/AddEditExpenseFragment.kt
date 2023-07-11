package com.example.turtle.ui.addeditexpense

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.util.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentAddEditExpenseBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AddEditExpenseFragment: Fragment() {

    private var _binding: FragmentAddEditExpenseBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditExpenseFragmentArgs by navArgs()

    private lateinit var userPayingDataAdapter: ArrayAdapter<Profile>
    private lateinit var usersPaidForDataAdapter: ArrayAdapter<Profile>

    private val viewModel: AddEditExpenseViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application,
            args.billId,
            args.expenseId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAddEditExpenseFragment()
        collectBill()
        collectExpense()
        collectBillAndExpense()
        collectBillAndUserPayingId()
        collectDate()
        collectState()
        collectSnackbar()
    }

    private fun initAddEditExpenseFragment() {
        binding.saveExpenseButton.setOnClickListener { saveExpense() }

        viewModel.getBill()
        viewModel.getExpense()

        setUpDatePickerDialog()
        setUpUserPayingSpinner()
        setUpUsersPaidForListView()
    }

    private fun collectBill() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.bill.collect { bill ->
                bill?.run { fillBillData(bill) }
            }
        }
    }

    private fun collectExpense() =
        collectLifecycleFlow(viewModel.expense) { expense ->
            expense?.run { fillExpenseData(expense) }
        }

    private fun collectDate() =
        collectLifecycleFlow(viewModel.calendar) { calendar ->
            updateDateInputField(calendar.time)
        }

    private fun collectState() =
        collectLifecycleFlow(viewModel.isDone) { isDone ->
            if (isDone) findNavController().navigateUp()
        }

    private fun collectSnackbar() =
        collectLifecycleFlow(viewModel.snackbarText) { message ->
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        }

    private fun fillBillData(bill: Bill) {
        userPayingDataAdapter.addAll(bill.users!!)
        usersPaidForDataAdapter.addAll(bill.users!!)
    }

    private fun fillExpenseData(expense: Expense) {
        with(binding) {
            fieldTitle.setText(expense.title)
            fieldAmount.setText(expense.amount)
        }
    }

    private fun collectBillAndExpense() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.bill.combine(viewModel.expense) { bill, expense ->
                Pair(bill, expense)
            }.collect { (bill, expense) ->
                bill?.run {
                    setUsersPaidFor(bill, expense)
                }
            }
        }
    }

    private fun collectBillAndUserPayingId() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.bill.combine(viewModel.userPayingId) { bill, userPayingId ->
                Pair(bill, userPayingId)
            }.collect { (bill, userPayingId) ->
                bill?.run {
                    setUserPaying(bill, userPayingId)
                }
            }
        }
    }

    private fun setUserPaying(bill: Bill, userPayingId: String) {
        val userPosition = userPayingDataAdapter.getPosition(
            bill.users!!.first { it.userId == userPayingId }
        )
        binding.userPayingSpinner.setSelection(userPosition)
    }

    private fun setUsersPaidFor(bill: Bill, expense: Expense?) {
        expense?.run {
            bill.users!!.filter { user ->
                user.userId in expense.usersPaidForId!!.keys
            }.forEach {
                val position = usersPaidForDataAdapter.getPosition(it)
                binding.usersPaidForList.setItemChecked(position, true)
            }
        } ?:run {
            bill.users!!.indices.forEach {
                binding.usersPaidForList.setItemChecked(it, true)
            }
        }

        setUpUsersPaidForHeight(usersPaidForDataAdapter)
    }

    private fun saveExpense() = viewLifecycleOwner.lifecycleScope.launch {
        val title = binding.fieldTitle.text.toString()
        val amountString = binding.fieldAmount.text.toString()
        val dateString = binding.fieldDate.text.toString()
        val userPaying = binding.userPayingSpinner.selectedItem as Profile

        val usersPaidFor = mutableListOf<Profile>()
        binding.usersPaidForList.checkedItemPositions.forEach { key, value ->
            if (value)
                usersPaidFor.add(binding.usersPaidForList.getItemAtPosition(key) as Profile)
        }

        viewModel.saveExpense(title, amountString, dateString, userPaying, usersPaidFor)
    }

    private fun setUpDatePickerDialog() {
        val datePickerDialog = OnDateSetListener { _, year, month, dayOfMonth ->
            viewModel.setDate(year, month, dayOfMonth)
        }

        binding.fieldDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                datePickerDialog,
                viewModel.getYear(),
                viewModel.getMonth(),
                viewModel.getDayOfMonth(),
            ).show()
        }
    }

    private fun setUpUserPayingSpinner() {
        userPayingDataAdapter = ArrayAdapter<Profile>(
            requireContext(),
            android.R.layout.simple_spinner_item,
        )
        userPayingDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.userPayingSpinner.adapter = userPayingDataAdapter
    }

    private fun setUpUsersPaidForListView() {
        usersPaidForDataAdapter = ArrayAdapter<Profile>(
            requireContext(),
            android.R.layout.simple_list_item_multiple_choice
        )
        binding.usersPaidForList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        binding.usersPaidForList.adapter = usersPaidForDataAdapter
    }

    private fun setUpUsersPaidForHeight(dataAdapter: ArrayAdapter<Profile>) {
        var total = 0
        for (i in 0 until dataAdapter.count) {
            val listItem = dataAdapter.getView(i, null, binding.usersPaidForList)
            listItem.measure(0, 0)
            total += listItem.measuredHeight + 50
        }

        val params = binding.usersPaidForList.layoutParams
        params.height = total + (binding.usersPaidForList.dividerHeight * (dataAdapter.count))
        binding.usersPaidForList.layoutParams = params
    }

    private fun updateDateInputField(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        binding.fieldDate.setText(dateFormat.format(date))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun <T> AddEditExpenseFragment.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
}
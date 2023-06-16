package com.example.turtle.ui.addexpense

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.util.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentAddExpenseBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val TAG = "ADD_EXPENSE"


class AddExpenseFragment: Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth

    private val calendar: Calendar = Calendar.getInstance()
    private val args: AddExpenseFragmentArgs by navArgs()

    private lateinit var bill: Bill
    private lateinit var expenseCollectionRef: CollectionReference
    private val billCollectionRef = Firebase.firestore.collection("bills")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUp(args.billId)

        binding.saveExpenseButton.setOnClickListener { saveExpense() }
    }

    private fun saveExpense() = viewLifecycleOwner.lifecycleScope.launch {
        val title = binding.fieldTitle.text.toString()

        if (title.isEmpty()) {
            Snackbar.make(requireView(), "Title cannot be empty.", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        val amountString = binding.fieldAmount.text.toString()
        val amount = if (amountString.isNotEmpty()) BigDecimal(amountString) else BigDecimal.ZERO

        if (amount <= BigDecimal.ZERO) {
            Snackbar.make(requireView(), "Please insert a positive amount.", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.US)
        val date = dateFormat.parse(
            "${binding.fieldDate.text.toString()} " +
                    "${calendar.get(Calendar.HOUR_OF_DAY)}:" +
                    "${calendar.get(Calendar.MINUTE)}:" +
                    "${calendar.get(Calendar.SECOND)}"
        )!!

        val userPayingId = (binding.userPayingSpinner.selectedItem as Profile).userId!!

        val usersPaidFor = mutableMapOf<String, String>()

        val selectedUsersCount = binding.usersPaidForList.checkedItemCount

        if (selectedUsersCount == 0) {
            Snackbar.make(requireView(), "You must pay for at least one participant.", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        binding.usersPaidForList.checkedItemPositions.forEach { key, value ->
            if (value) {
                val userId = (binding.usersPaidForList.getItemAtPosition(key) as Profile).userId!!
                val userAmount = (amount / BigDecimal(selectedUsersCount)).toString()
                usersPaidFor[userId] = userAmount
            }
        }

        val expense = createExpense(title, amount, date, userPayingId, usersPaidFor)

        try {
            expenseCollectionRef.add(expense).await()
            Snackbar.make(requireView(), "Expense saved", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }

        findNavController().navigateUp()
    }

    private fun createExpense(
        title: String,
        amount: BigDecimal,
        date: Date,
        userPayingId: String,
        usersPaidFor: Map<String, String>
    ): Expense = Expense (
        title = title,
        bigDecimalAmount = amount,
        date = date,
        userPayingId = userPayingId,
        usersPaidFor = usersPaidFor
    )

    private fun setUpDatePickerDialog() {
        updateDateInputField()

        val datePickerDialog = OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInputField()
        }

        binding.fieldDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                datePickerDialog,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
    }

    private fun updateDateInputField() {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        binding.fieldDate.setText(dateFormat.format(calendar.time))
    }

    private fun setUpUserPayingSpinner(users: List<Profile>) {
        val dataAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            users
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.userPayingSpinner.adapter = dataAdapter


        val currentUserPosition = dataAdapter.getPosition(users.first { it.userId == auth.currentUser!!.uid })
        binding.userPayingSpinner.setSelection(currentUserPosition)
    }

    private fun setUpUsersPaidForListView(users: List<Profile>) {
        val dataAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, users)
        binding.usersPaidForList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        binding.usersPaidForList.adapter = dataAdapter

        for (i in users.indices) {
            binding.usersPaidForList.setItemChecked(i, true)
        }
    }

    private fun setUp(billId: String) {
        try {
            setUpDatePickerDialog()

            expenseCollectionRef = billCollectionRef.document(billId).collection("expenses")

            billCollectionRef.document(billId).get().addOnSuccessListener {
                bill = it.toObject(Bill::class.java)!!
                val users = getBillUsers(bill)

                setUpUserPayingSpinner(users)
                setUpUsersPaidForListView(users)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }
    }

    private fun getBillUsers(bill: Bill): List<Profile> = bill.users!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

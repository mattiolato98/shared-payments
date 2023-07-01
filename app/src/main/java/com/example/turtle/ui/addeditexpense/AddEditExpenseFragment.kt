package com.example.turtle.ui.addeditexpense

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
import com.example.turtle.databinding.FragmentAddEditExpenseBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.exp

const val TAG = "ADD_EXPENSE"


class AddEditExpenseFragment: Fragment() {

    private var _binding: FragmentAddEditExpenseBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth

    private val calendar: Calendar = Calendar.getInstance()
    private val args: AddEditExpenseFragmentArgs by navArgs()

    private lateinit var bill: Bill
    private lateinit var expenseCollectionRef: CollectionReference
    private val billCollectionRef = Firebase.firestore.collection("bills")

    private var expense: Expense? = null

    private var isNewExpense: Boolean = true

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
        setUp(args.billId, args.expenseId)

        binding.saveExpenseButton.setOnClickListener { saveExpense() }
    }

    private fun setUp(billId: String, expenseId: String?) = viewLifecycleOwner.lifecycleScope.launch {
        bill = try {
            expenseCollectionRef = billCollectionRef.document(billId).collection("expenses")

            val billDoc = billCollectionRef.document(billId).get().await()
            billDoc.toObject(Bill::class.java)!!
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return@launch
        }

        val users = getBillUsers(bill)

        if (expenseId == null) {
            setUpDatePickerDialog()
            setUpUserPayingSpinner(users)
            setUpUsersPaidForListView(users)
        } else {
            expense = setUpExpense(expenseId)

            setUpDatePickerDialog(expense!!.date)
            setUpUserPayingSpinner(users, expense!!.userPayingId)
            setUpUsersPaidForListView(users, expense!!.usersPaidFor)
        }
    }

    private suspend fun setUpExpense(expenseId: String): Expense? {
        val expense = try {
            val doc = expenseCollectionRef.document(expenseId).get().await()
            doc.toObject(Expense::class.java)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
            return null
        }

        isNewExpense = false

        with(binding) {
            fieldTitle.setText(expense!!.title)
            fieldAmount.setText(expense.amount)
        }

        return expense
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
                val userAmount = (amount.divide(BigDecimal(selectedUsersCount), 2, RoundingMode.HALF_UP)).toString()
                usersPaidFor[userId] = userAmount
            }
        }

        val expenseObject = expenseObject(title, amount, date, userPayingId, usersPaidFor)

        try {
            if (isNewExpense)
                createNewExpense(expenseObject)
            else
                updateExpense(expense!!.documentId!!, expenseObject)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }

        findNavController().navigateUp()
    }

    private fun createNewExpense(expense: Expense) {
        expenseCollectionRef.add(expense)
        Snackbar.make(requireView(), "Expense saved", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateExpense(expenseId: String, newExpense: Expense) {
        expenseCollectionRef.document(expenseId).set(
            newExpense.copy(usersPaidFor = mutableMapOf()),
            SetOptions.merge()
        )

        // usersPaidFor field updated separately, since it needs to be overwritten rather than merged
        expenseCollectionRef.document(expenseId).update("usersPaidFor", newExpense.usersPaidFor)

        Snackbar.make(requireView(), "Expense information updated", Snackbar.LENGTH_SHORT).show()
    }

    private fun expenseObject(
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

    private fun setUpDatePickerDialog(expenseDate: Date? = null) {
        expenseDate?.also {
            calendar.time = expenseDate
        }

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

    private fun setUpUserPayingSpinner(users: List<Profile>, userPayingId: String? = null) {
        val dataAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            users
        )
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.userPayingSpinner.adapter = dataAdapter

        val userPosition = if (userPayingId == null) {
            dataAdapter.getPosition(users.first { it.userId == auth.currentUser!!.uid })

        } else {
            dataAdapter.getPosition(users.first { it.userId == userPayingId })
        }

        binding.userPayingSpinner.setSelection(userPosition)
    }

    private fun setUpUsersPaidForListView(users: List<Profile>, usersPaidFor: Map<String, String>? = null) {
        val dataAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, users)
        binding.usersPaidForList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        binding.usersPaidForList.adapter = dataAdapter

        if (usersPaidFor == null) {
            users.indices.forEach {
                binding.usersPaidForList.setItemChecked(it, true)
            }
        } else {
            users.filter { user ->
                user.userId in usersPaidFor.keys
            }.forEach {
                val position = dataAdapter.getPosition(it)
                binding.usersPaidForList.setItemChecked(position, true)
            }
        }

        setUpUsersPaidForHeight(dataAdapter)
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

    private fun getBillUsers(bill: Bill): List<Profile> = bill.users!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

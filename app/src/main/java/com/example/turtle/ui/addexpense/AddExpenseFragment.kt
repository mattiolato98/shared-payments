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
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentAddExpenseBinding
import com.example.turtle.ui.billdetail.TAG
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment: Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val calendar: Calendar = Calendar.getInstance()
    private val args: AddExpenseFragmentArgs by navArgs()
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
    }

    private fun setUpUserPayingSpinner(users: List<String>) {
        val dataAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, users)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.userPayingSpinner.adapter = dataAdapter
    }

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

    private fun setUpUsersPaidForListView(users: List<String>) {
        val dataAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, users)
        binding.usersPaidForList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        binding.usersPaidForList.adapter = dataAdapter

        for (i in users.indices) {
            binding.usersPaidForList.setItemChecked(i, true)
        }
    }

    private fun getBillUsers(bill: Bill): MutableList<String> {
        val users = mutableListOf<String>()

        for (user in bill.users!!) {
            users.add(user.email.toString())
        }

        return users
    }

    private fun setUp(billId: String) {
        try {
            billCollectionRef.document(billId).get().addOnSuccessListener {
                val bill = it.toObject(Bill::class.java)!!

                val users = getBillUsers(bill)

                setUpDatePickerDialog()
                setUpUserPayingSpinner(users)
                setUpUsersPaidForListView(users)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

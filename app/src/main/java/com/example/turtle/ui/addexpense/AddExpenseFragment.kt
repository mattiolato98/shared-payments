package com.example.turtle.ui.addexpense

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.turtle.databinding.FragmentAddExpenseBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment: Fragment() {
    val calendar = Calendar.getInstance()

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

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

        setUpUserPayingSpinner()
        setUpDatePickerDialog()
        setUpUsersPaidForListView()
    }

    private fun setUpUserPayingSpinner() {
        val users = listOf(
            "Gabriele Mattioli",
            "Lorenzo Baraldi",
            "Fabrizio Garuti"
        )
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

    private fun setUpUsersPaidForListView() {
        val list = listOf(
            "Gabriele Mattioli",
            "Lorenzo Baraldi",
            "Fabrizio Garuti"
        )
        val dataAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, list)
        binding.usersPaidForList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        binding.usersPaidForList.adapter = dataAdapter
        binding.usersPaidForList.setItemChecked(0, true)
        binding.usersPaidForList.setItemChecked(1, true)
        binding.usersPaidForList.setItemChecked(2, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

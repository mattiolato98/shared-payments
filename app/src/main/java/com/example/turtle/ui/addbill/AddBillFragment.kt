package com.example.turtle.ui.addbill

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentAddBillBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val TAG = "ADD_BILL"


class AddBillFragment: Fragment() {

    private var _binding: FragmentAddBillBinding? = null
    private val binding get() = _binding!!

    private val billCollectionRef = Firebase.firestore.collection("bills")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveBillButton.setOnClickListener{ saveBill() }
    }

    private fun saveBill() = viewLifecycleOwner.lifecycleScope.launch {
        val title = binding.fieldTitle.text.toString()
        val description = binding.fieldDescription.text.toString().let {
            it.ifEmpty { null }
        }

        if (title.isEmpty()) {
            Snackbar.make(requireView(), "Bill title cannot be empty", Snackbar.LENGTH_SHORT).show()
            return@launch
        }

        val bill = Bill(
            title = title,
            description = description,
        )
        createBill(bill)
    }

    private suspend fun createBill(bill: Bill) {
        try {
            billCollectionRef.add(bill).await()
            Snackbar.make(requireView(), "Bill saved", Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
            if (e is CancellationException) throw e
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.turtle.ui.expensedetail

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import com.example.turtle.data.Profile
import com.example.turtle.databinding.FragmentExpenseDetailBinding
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

    private lateinit var usersPaidForAdapter: UsersPaidForAdapter

    private val billCollectionRef = Firebase.firestore.collection("bills")
    private lateinit var expenseCollectionRef: CollectionReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseDetailBinding.inflate(inflater, container, false)
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
            }

        usersPaidForAdapter = UsersPaidForAdapter(usersPaidFor)
        binding.usersPaidForList.adapter = usersPaidForAdapter
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
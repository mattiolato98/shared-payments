package com.example.turtle

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.turtle.ui.billdetail.BillDetailViewModel
import com.example.turtle.ui.bills.BillsViewModel
import com.example.turtle.ui.expensedetail.ExpenseDetailViewModel

class ViewModelFactory(
    private val application: Application? = null,
    private val billId: String? = null,
    private val expenseId: String? = null,
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val billRepository = (application as TurtleApplication).billRepository

        return with(modelClass) {
            when {
                isAssignableFrom(BillDetailViewModel::class.java) -> {
                    BillDetailViewModel(billRepository, billId!!) as T
                }
                isAssignableFrom(BillsViewModel::class.java) -> {
                    val userId = application.userId!!
                    BillsViewModel(billRepository, userId) as T
                }
                isAssignableFrom(ExpenseDetailViewModel::class.java) -> {
                    ExpenseDetailViewModel(billRepository, billId!!, expenseId!!) as T
                }
                else -> {
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
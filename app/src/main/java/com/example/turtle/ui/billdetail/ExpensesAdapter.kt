package com.example.turtle.ui.billdetail

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.example.turtle.data.Expense
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp

class ExpensesAdapter(
    private val bill: Bill
): RecyclerView.Adapter<ExpensesAdapter.ExpensesViewHolder>() {

    inner class ExpensesViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val expenseTitle: TextView
        val paidBy: TextView
        val expenseAmount: TextView
        val expenseDate: TextView

        init {
            expenseTitle = itemView.findViewById(R.id.expense_title)
            paidBy = itemView.findViewById(R.id.paid_by)
            expenseAmount = itemView.findViewById(R.id.expense_amount)
            expenseDate = itemView.findViewById(R.id.expense_date)
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExpensesAdapter.ExpensesViewHolder {
        return ExpensesViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.expense_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ExpensesAdapter.ExpensesViewHolder, position: Int) {
        val expense = differ.currentList[position]

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

        with(holder) {
            expenseTitle.text = expense.title
            val paidByUser = bill.users!!.filter { it.userId == expense.userPayingId }.first().email!!.split("@")[0]
            paidBy.text = Html.fromHtml("Paid by <b>${paidByUser}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            expenseAmount.text = expense.bigDecimalAmount.setScale(2, RoundingMode.HALF_UP).toString()
            expenseDate.text = dateFormat.format(expense.date!!)
        }

    }

    override fun getItemCount(): Int = differ.currentList.size
}
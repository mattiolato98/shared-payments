package com.example.turtle.ui.billdetail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.turtle.R

class RefundsAdapter(
    private val data: MutableList<Triple<String, String, String>> = mutableListOf(),
): RecyclerView.Adapter<RefundsAdapter.RefundsViewHolder>() {

    inner class RefundsViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val userDebtor: TextView
        val userCreditor: TextView
        val transactionAmount: TextView

        init {
            userDebtor = itemView.findViewById(R.id.user_debtor)
            userCreditor = itemView.findViewById(R.id.user_creditor)
            transactionAmount = itemView.findViewById(R.id.transaction_amount)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Triple<String, String, String>>) {
        this.data.removeAll { true }
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RefundsViewHolder {
        return RefundsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.refund_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RefundsViewHolder, position: Int) {
        val (debtor, amount, creditor) = data[position]

        with(holder) {
            userDebtor.text = debtor
            userCreditor.text = creditor
            transactionAmount.text = amount
        }
    }

    override fun getItemCount() = data.count()
}
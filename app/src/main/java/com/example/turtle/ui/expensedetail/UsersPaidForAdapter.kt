package com.example.turtle.ui.expensedetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.example.turtle.data.Profile
import java.math.BigDecimal
import java.math.RoundingMode

class UsersPaidForAdapter(
    private val data: Map<String, String>,
): RecyclerView.Adapter<UsersPaidForAdapter.UsersPaidForViewHolder>() {

    private val keys: List<String> = data.keys.toList()

    inner class UsersPaidForViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val user: TextView
        val userPositiveAmount: TextView
        val userNegativeAmount: TextView

        init {
            user = itemView.findViewById(R.id.user)
            userPositiveAmount = itemView.findViewById(R.id.user_positive_amount)
            userNegativeAmount = itemView.findViewById(R.id.user_negative_amount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersPaidForViewHolder {
        return UsersPaidForViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.user_paid_for_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UsersPaidForViewHolder, position: Int) {
        val username = keys[position]
        val amount = BigDecimal(data[keys[position]]).setScale(2, RoundingMode.HALF_UP)

        with(holder) {
            user.text = username

            if (amount >= BigDecimal.ZERO)
                userPositiveAmount.text = amount.toString()
            else
                userNegativeAmount.text = amount.toString()
        }
    }

    override fun getItemCount() = data.count()
}
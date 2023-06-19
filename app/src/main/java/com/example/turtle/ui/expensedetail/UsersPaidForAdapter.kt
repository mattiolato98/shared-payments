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
    private val data: Map<Profile, String>,
): RecyclerView.Adapter<UsersPaidForAdapter.UsersPaidForViewHolder>() {

    private val keys: List<Profile> = data.keys.toList()

    inner class UsersPaidForViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val user: TextView
        val userAmount: TextView

        init {
            user = itemView.findViewById(R.id.user)
            userAmount = itemView.findViewById(R.id.user_amount)
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
        val username = keys[position].email!!.split("@")[0]
        val amount = BigDecimal(data[keys[position]]).setScale(2, RoundingMode.HALF_UP).toString()

        with(holder) {
            user.text = username
            userAmount.text = amount
        }
    }

    override fun getItemCount() = data.count()
}
package com.example.turtle.ui.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.turtle.R
import com.example.turtle.data.Bill

class BillsAdapter(
    private val listener: (Bill) -> Unit
): RecyclerView.Adapter<BillsAdapter.BillsViewHolder>() {

    inner class BillsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val billTitle: TextView
        val billDescription: TextView

        init {
            billTitle = itemView.findViewById(R.id.bill_title)
            billDescription = itemView.findViewById(R.id.bill_description)
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: Bill, newItem: Bill): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillsViewHolder {
        return BillsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.bill_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BillsViewHolder, position: Int) {
        val bill = differ.currentList[position]
        with(holder) {
            billTitle.text = bill.title
            if (bill.description.isNullOrEmpty())
                billDescription.visibility = View.GONE
            else {
                billDescription.text = bill.description
            }
            itemView.setOnClickListener { listener(bill) }
        }
    }

    override fun getItemCount() = differ.currentList.size
}
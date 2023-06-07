package com.example.turtle.ui.bills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.turtle.R
import com.example.turtle.data.Bill
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso


class BillsAdapter(
    private val onClickListener: (Bill) -> Unit,
    private val onLongClickListener: (Bill) -> Boolean
): RecyclerView.Adapter<BillsAdapter.BillsViewHolder>() {

    inner class BillsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView
        val billTitle: TextView
        val billDescription: TextView
        val profileImages: List<ImageView>
        val extraUsersText: TextView

        init {
            cardView = itemView.findViewById(R.id.card_view)
            billTitle = itemView.findViewById(R.id.bill_title)
            billDescription = itemView.findViewById(R.id.bill_description)
            profileImages = listOf(
                itemView.findViewById(R.id.profile_image_01),
                itemView.findViewById(R.id.profile_image_02),
                itemView.findViewById(R.id.profile_image_03),
                itemView.findViewById(R.id.profile_image_04),
                itemView.findViewById(R.id.profile_image_05),
            )
            extraUsersText = itemView.findViewById(R.id.extra_users_text)
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

            cardView.setOnClickListener { onClickListener(bill) }
            cardView.setOnLongClickListener { onLongClickListener(bill) }
        }

        bill.users?.let { users ->
            bill.users!!.forEachIndexed { index, profile ->
                (holder.profileImages[index].parent as CardView).visibility = View.VISIBLE
                if (profile.profilePictureUrl != null) {
                    Picasso.get().load(profile.profilePictureUrl)
                        .resize(60, 60)
                        .centerCrop()
                        .into(holder.profileImages[index])
                } else {
                    holder.profileImages[index].setImageResource(R.drawable.profile_material)
                }

                if (index == 4)
                    return@forEachIndexed
            }

            (users.size - 5).coerceAtLeast(0).also { extra_users ->
                if (extra_users > 0) {
                    holder.extraUsersText.visibility = View.VISIBLE
                    holder.extraUsersText.text = "+$extra_users"
                }
            }
        }
    }

    override fun getItemCount() = differ.currentList.size
}
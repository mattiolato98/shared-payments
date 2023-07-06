package com.example.turtle.ui.billdetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.turtle.ViewModelFactory
import com.example.turtle.databinding.FragmentBalanceBinding
import com.example.turtle.ui.expensedetail.BalanceAdapter
import kotlinx.coroutines.launch


class BalanceFragment: Fragment() {
    private var _binding: FragmentBalanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var balanceAdapter: BalanceAdapter
    private lateinit var refundsAdapter: RefundsAdapter

    private lateinit var billId: String

    private val viewModel: BillDetailViewModel by activityViewModels {
        ViewModelFactory(
            requireActivity().application,
            billId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBalanceBinding.inflate(inflater, container, false)
        billId = this.requireArguments().getString("billId")!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBalanceFragment()
        collectBill()
    }

    private fun initBalanceFragment() {
        balanceAdapter = BalanceAdapter()
        binding.usersBalance.adapter = balanceAdapter

        refundsAdapter = RefundsAdapter()
        binding.refunds.adapter = refundsAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectBill() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.bill.collect { bill ->
                balanceAdapter.setData(bill.balance())
                balanceAdapter.notifyDataSetChanged()

                bill.refunds()?.also {
                    refundsAdapter.setData(it)
                    refundsAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.turtle.ui.billdetail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.example.turtle.R
import com.example.turtle.databinding.FragmentBillDetailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class BillDetailFragment: Fragment() {
    private var _binding: FragmentBillDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BillDetailFragmentArgs by navArgs()

    private lateinit var billDetailPagerAdapter: BillDetailPagerAdapter
    private lateinit var viewPager: ViewPager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTabLayout()
    }

    private fun setUpTabLayout() {
        billDetailPagerAdapter = BillDetailPagerAdapter(childFragmentManager, args.billId)
        viewPager = binding.pager
        viewPager.adapter = billDetailPagerAdapter
        binding.tabLayout.setupWithViewPager(viewPager)

        binding.tabLayout.getTabAt(0)?.setIcon(R.drawable.receipt_long_material)
        binding.tabLayout.getTabAt(1)?.setIcon(R.drawable.account_balance_material)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

class BillDetailPagerAdapter(fm: FragmentManager, private val billId: String): FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val expensesFragment = ExpensesFragment()
    private val balanceFragment = BalanceFragment()
    private val fragments: List<Fragment> get() = listOf(expensesFragment, balanceFragment)

    override fun getCount(): Int = 2

    override fun getItem(position: Int): Fragment {
        val fragment = fragments[position]
        val bundle = bundleOf(Pair("billId", billId))
        fragment.arguments = bundle

        return fragment
    }

    override fun getPageTitle(position: Int): CharSequence {
        return if (fragments[position] == expensesFragment)
            "Expenses"
        else
            "Balance"
    }
}
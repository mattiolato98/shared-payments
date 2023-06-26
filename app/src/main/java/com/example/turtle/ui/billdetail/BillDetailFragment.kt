package com.example.turtle.ui.billdetail

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.R
import com.example.turtle.databinding.FragmentBillDetailBinding
import com.google.android.material.snackbar.Snackbar


open class BillDetailFragment: Fragment(), MenuProvider {
    private var _binding: FragmentBillDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BillDetailFragmentArgs by navArgs()

    private lateinit var expensesFragment: ExpensesFragment
    private lateinit var balanceFragment: BalanceFragment

    private val tabs: List<LinearLayout> get() = binding.run {
        listOf(binding.expensesNavigation, binding.balanceNavigation)
    }
    private val fragments: List<Fragment> get() = listOf(expensesFragment, balanceFragment)

    private var selectedIndex = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailBinding.inflate(inflater, container, false)

        if (savedInstanceState == null) {
            expensesFragment = ExpensesFragment()
            balanceFragment = BalanceFragment()

            val bundle = bundleOf(Pair("billId", args.billId))
            expensesFragment.arguments = bundle
            balanceFragment.arguments = bundle

            childFragmentManager.beginTransaction()
                .add(R.id.container, expensesFragment, "expensesFragment")
                .add(R.id.container, balanceFragment, "balanceFragment")
                .selectFragment(selectedIndex)
                .commit()

        } else {
            selectedIndex = savedInstanceState.getInt("selectedIndex", 0)

            expensesFragment = childFragmentManager.findFragmentByTag("expensesFragment") as ExpensesFragment
            balanceFragment = childFragmentManager.findFragmentByTag("balanceFragment") as BalanceFragment
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupTabSelectedState(selectedIndex)
        tabs.forEachIndexed { index, textView ->
            textView.setOnClickListener { selectFragment(index) }
        }
    }

    private fun FragmentTransaction.selectFragment(selectedIndex: Int?): FragmentTransaction {
        fragments.forEachIndexed { index, fragment ->
            if (index == selectedIndex) attach(fragment) else detach(fragment)
        }
        return this
    }

    private fun selectFragment(index: Int) {
        selectedIndex = index
        setupTabSelectedState(index)
        childFragmentManager.beginTransaction().selectFragment(index).commit()
    }

    private fun setupTabSelectedState(selectedIndex: Int) {
        val theme = requireContext().theme
        val primaryColor = TypedValue()
        val defaultTextColor = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, primaryColor, true)
        theme.resolveAttribute(android.R.attr.textColorPrimary, defaultTextColor, true)

        tabs.forEachIndexed { index, layout ->
            val icon = (layout.getChildAt(0) as ImageView)
            val textView = (layout.getChildAt(1) as TextView)

            when (index) {
                selectedIndex -> {
                    textView.typeface = Typeface.DEFAULT_BOLD
                    textView.setTextColor(primaryColor.data)
                    icon.setColorFilter(primaryColor.data)
                }
                else -> {
                    textView.typeface = Typeface.DEFAULT
                    textView.setTextColor(defaultTextColor.data)
                    icon.setColorFilter(defaultTextColor.data)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedIndex", selectedIndex)
    }

    private fun navigateToEditBill() {
        val action = BillDetailFragmentDirections.navigateToEditBill(
            args.billId,
            resources.getString(R.string.edit_bill)
        )
        navigateToDirection(action)
    }

    private fun navigateToDirection(action: NavDirections) {
        childFragmentManager.beginTransaction().selectFragment(null).commit()
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.bill_options, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.edit_bill -> navigateToEditBill()
            R.id.delete_bill -> Snackbar.make(requireView(), "Delete", Snackbar.LENGTH_SHORT).show()
        }

        return true
    }
}
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

    private val billCollectionRef = Firebase.firestore.collection("bills")

    private lateinit var billDetailPagerAdapter: BillDetailPagerAdapter
    private lateinit var viewPager: ViewPager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillDetailBinding.inflate(inflater, container, false)
        setupMenuProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpTabLayout()
    }

    private fun navigateToEditBill() {
        val action = BillDetailFragmentDirections.navigateToEditBill(
            args.billId,
            resources.getString(R.string.edit_bill)
        )
        findNavController().navigate(action)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete the bill? The action is not reversible!")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.cancel()
                billCollectionRef.document(args.billId).delete()
                    .addOnSuccessListener {
                        Snackbar.make(
                            requireView(),
                            "Bill successfully deleted",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        Snackbar.make(
                            requireView(),
                            "An unexpected error occurred while deleting the item. Retry later",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMenuProvider() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.bill_options, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.edit_bill -> navigateToEditBill()
                        R.id.delete_bill -> showDeleteDialog()
                        else -> return false
                    }

                    return true
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
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
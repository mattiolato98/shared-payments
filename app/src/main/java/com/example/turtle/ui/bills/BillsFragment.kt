package com.example.turtle.ui.bills

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.turtle.R
import com.example.turtle.SettingsPreferences
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentBillsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class BillsFragment : Fragment() {

    private var _binding: FragmentBillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var billsAdapter: BillsAdapter
    private val viewModel: BillsViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application
        )
    }

    private lateinit var settingPreferences: SettingsPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillsBinding.inflate(inflater, container, false)

        settingPreferences = SettingsPreferences(requireContext())
        setupMenuProvider()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBillsFragment()

        collectBills()
        collectSnackbar()
    }

    private fun initBillsFragment() {
        billsAdapter = BillsAdapter { item -> navigateToBillDetail(item)}
        binding.billsList.adapter = billsAdapter

        binding.newBillButton.setOnClickListener { navigateToAddBill() }
    }

    private fun collectBills() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.bills.collect { bills ->
                bills?.let {
                    billsAdapter.differ.submitList(bills)
                }
            }
        }
    }

    private fun collectSnackbar() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.snackbarText.collect { msg ->
                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToAddBill() {
        val action = BillsFragmentDirections.navigateToAddBill(
            title = resources.getString(R.string.new_bill)
        )
        findNavController().navigate(action)
    }

    private fun navigateToBillDetail(bill: Bill) {
        val action = BillsFragmentDirections.navigateToBillDetail(
            bill.documentId!!,
            bill.title
        )
        findNavController().navigate(action)
    }

    private fun navigateToProfile() = viewLifecycleOwner.lifecycleScope.launch {
        val username = settingPreferences.getUsername.first()
        val action = BillsFragmentDirections.navigateToProfile(username)
        findNavController().navigate(action)
    }

    @SuppressLint("RestrictedApi")
    private fun setupMenuProvider() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.home_options, menu)
                    (menu as MenuBuilder).setOptionalIconsVisible(true)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.open_profile -> navigateToProfile()
                        else -> return false
                    }

                    return true
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
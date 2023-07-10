package com.example.turtle.ui.addeditbill

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.turtle.R
import com.example.turtle.ViewModelFactory
import com.example.turtle.data.Bill
import com.example.turtle.databinding.FragmentAddEditBillBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

const val TAG = "ADD_BILL"


class AddEditBillFragment: Fragment() {

    private var _binding: FragmentAddEditBillBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditBillFragmentArgs by navArgs()

    private val viewModel: AddEditBillViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application,
            args.billId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAddEditBillFragment()
        collectBill()
        collectAddFriend()
        collectShowFriendsTitle()
        collectState()
        collectSnackbar()
    }

    private fun initAddEditBillFragment() {
        with(binding) {
            saveBillButton.setOnClickListener { saveBill() }
            fieldAddFriend.doOnTextChanged { text, _, _, _ -> fillEmailText(text) }
            buttonAddFriend.setOnClickListener { addFriend() }
        }

        viewModel.getBill()
    }

    private fun collectBill() =
        collectLifecycleFlow(viewModel.bill) { bill ->
            if (!viewModel.isNewBill)
                bill.run { fillBillData(bill!!) }
    }

    private fun collectAddFriend() =
        collectLifecycleFlow(viewModel.friendsProfiles) { profiles ->
            binding.friendsLinearLayout.removeAllViews()
            profiles.forEach {
                addFriendToLayout(it.key)
                if (binding.fieldAddFriend.text.toString() == it.key)
                    clearAddFriend()
            }
    }

    private fun collectShowFriendsTitle() =
        collectLifecycleFlow(viewModel.showFriendsTitle) { show ->
            if (show) showFriendsTitle() else hideFriendsTitle()
    }


    private fun collectState() =
        collectLifecycleFlow(viewModel.isDone) { isDone ->
            if (isDone) findNavController().navigateUp()
    }

    private fun collectSnackbar() =
        collectLifecycleFlow(viewModel.snackbarText) { message ->
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun fillBillData(bill: Bill) {
        binding.fieldTitle.setText(bill.title)
        binding.fieldDescription.setText(bill.description)
    }

    private fun saveBill() {
        viewModel.title.value = binding.fieldTitle.text.toString()
        viewModel.description.value = binding.fieldDescription.text.toString()
            .let { it.ifEmpty { null } }

        viewModel.saveBill()
    }

    private fun addFriend() = viewModel.addFriend(binding.fieldAddFriend.text.toString().trim())

    private fun addFriendToLayout(text: String) {
        (LayoutInflater.from(requireContext()).inflate(
            R.layout.button_borderless_template,
            null,
            false
        ) as Button).also { btn ->
            btn.text = text
            btn.setOnClickListener { removeFriend(it) }
            binding.friendsLinearLayout.addView(btn)
        }
    }

    private fun clearAddFriend() {
        binding.fieldAddFriend.text = null
        binding.buttonAddFriend.text = null
    }

    private fun removeFriend(v: View) = viewModel.removeFriend((v as Button).text.toString())

    private fun fillEmailText(text: CharSequence?) {
        if (text.toString().isNotEmpty()) {
            binding.buttonAddFriend.visibility = View.VISIBLE
            binding.buttonAddFriend.text = "${text.toString()} +"
        } else {
            binding.buttonAddFriend.visibility = View.GONE
        }
    }

    private fun showFriendsTitle() {
        binding.friendsTitle.visibility = View.VISIBLE
    }

    private fun hideFriendsTitle() {
        binding.friendsTitle.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun <T> AddEditBillFragment.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
}
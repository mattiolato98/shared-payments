package com.example.turtle

import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ViewModelExample: ViewModel() {
    private val toSignUpEventChannel = Channel<Unit>()
    val toSignUpEventFlow = toSignUpEventChannel.receiveAsFlow()

    fun toSignUp() = viewModelScope.launch {
        toSignUpEventChannel.send(Unit)
    }
}

// Consumo lato view
//viewLifecycleOwner.lifecycleScope.launch {
//    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//        viewModel.toSignUpEventFlow.collectLatest {
////                    navigateToSignUp()
//        }
//    }
//}
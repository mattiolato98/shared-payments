package com.example.turtle.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.Bill
import com.example.turtle.data.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

class BillsViewModel @Inject constructor(
    private val repository: BillRepository,
    private val userId: String,
): ViewModel() {

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    val bills: Flow<List<Bill>?> = collectBills()

    private fun collectBills() = flow {
        repository.getBills(userId).collect {
            when(it) {
                is Resource.Success -> emit(it.data)
                is Resource.Error -> _snackbarText.emit(it.message!!)
            }
        }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
}
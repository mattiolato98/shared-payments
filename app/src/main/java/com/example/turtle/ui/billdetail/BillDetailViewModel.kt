package com.example.turtle.ui.billdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.Bill
import com.example.turtle.data.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class BillDetailViewModel @Inject constructor(
    private val repository: BillRepository,
    private val billId: String
): ViewModel() {

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    private val _isDeleted = MutableStateFlow<Boolean>(false)
    val isDeleted = _isDeleted.asStateFlow()

    val bill: Flow<Bill> = collectBillAndExpenses()

    private fun collectBillAndExpenses() = flow {
        repository.getBillAndExpenses(billId).collect {
            when(it) {
                is Resource.Success -> emit(it.data!!)
                is Resource.Error -> _snackbarText.emit(it.message!!)
            }
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    fun deleteBill() = viewModelScope.launch {
        when(val result = repository.deleteBill(billId)) {
            is Resource.Success -> {
                _snackbarText.emit(result.message!!)
                _isDeleted.value = true
            }
            is Resource.Error -> _snackbarText.emit(result.message!!)
        }
    }
}
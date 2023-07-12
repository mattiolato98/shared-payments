package com.example.turtle.ui.addeditexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.Bill
import com.example.turtle.data.BillRepository
import com.example.turtle.data.Expense
import com.example.turtle.data.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject


class AddEditExpenseViewModel @Inject constructor(
    private val repository: BillRepository,
    private val userId: String,
    private val billId: String,
    private val expenseId: String?,
): ViewModel() {

    private val _calendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    val calendar = _calendar.asStateFlow()

    private var isNewExpense: Boolean = true

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    private val _isDone = MutableStateFlow<Boolean>(false)
    val isDone = _isDone.asStateFlow()

    private val _expense = MutableStateFlow<Expense?>(null)
    val expense = _expense.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val _bill = MutableStateFlow<Bill?>(null)
    val bill = _bill.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val _userPayingId = MutableStateFlow(userId)
    val userPayingId = _userPayingId.shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    fun getExpense() = viewModelScope.launch {
        if (isNewExpense && expenseId != null) {
            repository.getExpense(billId, expenseId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _expense.value = result.data!!
                        _userPayingId.value = result.data.userPayingId!!

                        val expenseDateCalendar = Calendar.getInstance().also {
                            it.time = result.data.date!!
                        }
                        _calendar.value = expenseDateCalendar

                        isNewExpense = false
                    }
                    is Resource.Error -> _snackbarText.emit(result.message!!)
                }
            }
        }
    }

    fun getBill() = viewModelScope.launch {
        repository.getBillAndExpenses(billId).collect { result ->
            when (result) {
                is Resource.Success -> {
                    _bill.value = result.data!!
                }

                is Resource.Error -> _snackbarText.emit(result.message!!)
            }
        }
    }

    fun saveExpense(
        title: String,
        amountString: String,
        dateString: String,
        userPaying: Profile,
        usersPaidFor: List<Profile>,
    ) = viewModelScope.launch {
        if (title.isEmpty()) {
            _snackbarText.emit("Title cannot be empty")
            return@launch
        }

        val amount = if (amountString.isNotEmpty()) BigDecimal(amountString) else BigDecimal.ZERO
        if (amount <= BigDecimal.ZERO) {
            _snackbarText.emit("Please insert a positive amount")
            return@launch
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.US)
        val date = dateFormat.parse(
            dateString +
                    " ${_calendar.value.get(Calendar.HOUR_OF_DAY)}:" +
                    "${_calendar.value.get(Calendar.MINUTE)}:" +
                    "${_calendar.value.get(Calendar.SECOND)}"
        )!!

        val selectedUsersCount = usersPaidFor.size

        if (selectedUsersCount == 0) {
            _snackbarText.emit("You must pay for at least one participant")
            return@launch
        }

        val userPayingId = userPaying.userId!!
        val userPayingUsername = userPaying.username!!
        val usersPaidForId = mutableMapOf<String, String>()
        val usersPaidForUsername = mutableMapOf<String, String>()

        usersPaidFor.forEach { profile ->
            val userId = profile.userId!!
            val username = profile.username!!
            val userAmount = (amount.divide(BigDecimal(selectedUsersCount), 2, RoundingMode.HALF_UP)).toString()

            usersPaidForId[userId] = userAmount
            usersPaidForUsername[username] = userAmount
        }

        val expenseObject = _expense.value?.copy(
            title = title,
            bigDecimalAmount = amount,
            date = date,
            userPayingId = userPayingId,
            userPayingUsername = userPayingUsername,
            usersPaidForId = usersPaidForId,
            usersPaidForUsername = usersPaidForUsername
        ) ?: Expense(
            title = title,
            bigDecimalAmount = amount,
            date = date,
            userPayingId = userPayingId,
            userPayingUsername = userPayingUsername,
            usersPaidForId = usersPaidForId,
            usersPaidForUsername = usersPaidForUsername
        )

        val result = if (isNewExpense) {
            repository.createExpense(billId, expenseObject)
        } else {
            repository.updateExpense(billId, expenseId!!, expenseObject)
        }

        _snackbarText.emit(result.message!!)

        if (result is Resource.Success)
            _isDone.emit(true)
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val newDateCalendar = Calendar.getInstance()
        newDateCalendar.set(Calendar.YEAR, year)
        newDateCalendar.set(Calendar.MONTH, month)
        newDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        _calendar.value = newDateCalendar
    }

    fun getYear() = _calendar.value.get(Calendar.YEAR)
    fun getMonth() = _calendar.value.get(Calendar.MONTH)
    fun getDayOfMonth() = _calendar.value.get(Calendar.DAY_OF_MONTH)

    fun isEditExpense(): Boolean {
        return expenseId ?.let { true } ?:let { false }
    }
}
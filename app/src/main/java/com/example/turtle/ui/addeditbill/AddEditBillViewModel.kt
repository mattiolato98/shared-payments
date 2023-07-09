package com.example.turtle.ui.addeditbill

import android.util.Log
import android.widget.Button
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turtle.Resource
import com.example.turtle.data.AuthRepository
import com.example.turtle.data.Bill
import com.example.turtle.data.BillRepository
import com.example.turtle.data.Profile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddEditBillViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: BillRepository,
    private val userId: String,
    private val billId: String?,
    private val userEmail: String,
): ViewModel() {

    val title = MutableStateFlow<String>("")
    val description = MutableStateFlow<String?>(null)

    private val _friendsProfiles = MutableStateFlow<Map<String, Profile>>(mapOf())
    val friendsProfiles = _friendsProfiles.asStateFlow()

    private var isNewBill: Boolean = true

    private val _snackbarText = MutableSharedFlow<String>()
    val snackbarText = _snackbarText.asSharedFlow()

    private val _bill = MutableStateFlow(Bill())
    val bill = _bill.asStateFlow()

    private val _showFriendsTitle = MutableStateFlow<Boolean>(false)
    val showFriendsTitle = _showFriendsTitle.asStateFlow()

    private val _isDone = MutableStateFlow<Boolean>(false)
    val isDone = _isDone.asStateFlow()

    fun getBill() = viewModelScope.launch {
        isNewBill = false

        repository.getBillAndExpenses(billId!!).collect { result ->
            when(result) {
                is Resource.Success -> {
                    _bill.value = result.data!!

                    result.data.users!!.filter { profile ->
                        profile.userId != userId
                    }.forEach { addFriend(it) }
                }
                is Resource.Error -> _snackbarText.emit(result.message!!)
            }
        }
    }

    fun saveBill() = viewModelScope.launch {
        if (title.value.isEmpty()) {
            _snackbarText.emit("Bill title cannot be empty")
            return@launch
        }

        if (isNewBill) createBill() else updateBill()
    }

    private suspend fun createBill() {
        val currentUserProfile = when(val result = authRepository.getProfileByUserId(userId)) {
            is Resource.Success -> result.data!!
            is Resource.Error -> {
                _snackbarText.emit("An error occurred. Try again later.")
                return
            }
        }

        Log.d("TAG", "${title.value}")

        repository.createBill(
            userId,
            currentUserProfile,
            title.value,
            description.value,
            _friendsProfiles.value.values.toList(),
        )

        _isDone.emit(true)
        _snackbarText.emit("Bill saved")
    }

    private suspend fun updateBill() {
        val currentUserProfile = when(val result = authRepository.getProfileByUserId(userId)) {
            is Resource.Success -> result.data!!
            is Resource.Error -> {
                _snackbarText.emit("An error occurred. Try again later.")
                return
            }
        }

        repository.updateBill(
            billId!!,
            userId,
            currentUserProfile,
            title.value,
            description.value,
            _friendsProfiles.value.values.toList()
        )

        _snackbarText.emit("Bill information updated")
    }

    fun addFriend(email: String) = viewModelScope.launch {
        if (email == userEmail) {
            _snackbarText.emit("You cannot add yourself")
            return@launch
        }
        if (friendAlreadyAdded(email)) {
            _snackbarText.emit("Friend already added")
            return@launch
        }

        when(val result = authRepository.getProfileByEmail(email)) {
            is Resource.Success -> {
                val profile = result.data!!
                _friendsProfiles.add(email, profile)
            }
            is Resource.Error -> {
                _snackbarText.emit(result.message!!)
                return@launch
            }
        }

        if (_friendsProfiles.value.isNotEmpty())
            _showFriendsTitle.emit(true)
    }

    private fun addFriend(profile: Profile) = viewModelScope.launch {
        val email = profile.email!!
        _friendsProfiles.add(email, profile)
    }

    fun removeFriend(buttonClicked: Button, email: String) = viewModelScope.launch {
        if (!isNewBill && isUserInvolvedInExpenses(email)) {
            _snackbarText.emit(
                "This user is involved in at least one expense, so it is not possible to remove it"
            )
            return@launch
        }

        _friendsProfiles.remove(email)

        if (_friendsProfiles.value.isEmpty())
            _showFriendsTitle.emit(false)
    }

    private fun isUserInvolvedInExpenses(userEmail: String): Boolean {
        val profile = _friendsProfiles.value[userEmail]!!
        return repository.isUserInvolvedInExpenses(bill.value, profile.userId!!)
    }

    private fun friendAlreadyAdded(email: String): Boolean {
        return _friendsProfiles.value.containsKey(email)
    }

    private fun <K, V> MutableStateFlow<Map<K, V>>.remove(elementKey: K) {
        val newMap = value.toMutableMap()
        newMap.remove(elementKey)
        value = newMap
    }

    private fun <K, V> MutableStateFlow<Map<K, V>>.add(elementKey: K, elementValue: V) {
        val newMap = value.toMutableMap()
        newMap[elementKey] = elementValue
        value = newMap
    }
}
